/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.trainee.credentials.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.service.RevocationService;

/**
 * A request filter that verifies the signature of signed data.
 */
@Slf4j
@Component
public class SignedDataFilter extends OncePerRequestFilter {

  private static final String ISSUE_PATH = "/api/issue/";

  private static final String SIGNATURE_FIELD = "signature";
  private static final String HMAC_FIELD = "hmac";
  private static final String TIS_ID_FIELD = "tisId";

  private final ObjectMapper mapper;
  private final String signatureSecretKey;

  private final RevocationService revocationService;

  /**
   * Create a request filter that verifies the signature of signed data.
   *
   * @param mapper             An {@link ObjectMapper} to use when reading the request payload.
   * @param signatureSecretKey The secret key used to sign and verify the signature.
   */
  SignedDataFilter(ObjectMapper mapper,
      @Value("${application.signature.secret-key}") String signatureSecretKey,
      RevocationService revocationService) {
    this.mapper = mapper;
    this.signatureSecretKey = signatureSecretKey;
    this.revocationService = revocationService;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    return requestUri.endsWith("/callback");
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    request = new CachedBodyRequestWrapper(request);

    try {
      if (hasValidSignature(request)) {
        filterChain.doFilter(request, response);
      } else {
        response.setStatus(403);
      }
    } catch (IOException e) {
      log.error("Unable to validate data signature.", e);
      response.setStatus(403);
    }
  }

  /**
   * Validate whether the given request has a payload with a valid signature.
   *
   * @param request The request to validate.
   * @return Whether the request was signed and valid.
   * @throws IOException If the request could not be parsed to signed data.
   */
  private boolean hasValidSignature(HttpServletRequest request) throws IOException {
    JsonNode tree = mapper.readTree(request.getInputStream());
    JsonNode signatureNode = tree.get(SIGNATURE_FIELD);

    if (signatureNode != null) {
      Signature signature = mapper.treeToValue(signatureNode, Signature.class);

      String hmac = signature.hmac();
      Instant signedAt = signature.signedAt();
      Instant validUntil = signature.validUntil();

      if (hmac != null && isValidInstant(signedAt, true) && isValidInstant(validUntil, false)
          && isDataValid(request, tree, signature)) {
        ((ObjectNode) signatureNode).remove(HMAC_FIELD);
        byte[] treeBytes = mapper.writeValueAsBytes(tree);
        String verificationSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
            signatureSecretKey).hmacHex(treeBytes);
        return hmac.equals(verificationSignature);
      }
    }

    return false;
  }

  /**
   * Check whether the signed data is still valid.
   *
   * @param request   The http request.
   * @param tree      The request's json contents.
   * @param signature The request data's signature.
   * @return true is the data is still valid, else false if the data was modified after signing.
   */
  private boolean isDataValid(HttpServletRequest request, JsonNode tree, Signature signature) {
    String servletPath = request.getServletPath();

    // Skip data validation unless issuing a credential.
    if (!servletPath.startsWith(ISSUE_PATH)) {
      return true;
    }

    Optional<CredentialType> credentialType = CredentialType.fromPath(servletPath);

    if (credentialType.isEmpty() || !tree.has(TIS_ID_FIELD)) {
      return false;
    }

    String tisId = tree.get(TIS_ID_FIELD).asText();
    Optional<Instant> modifiedDate = revocationService.getLastModifiedDate(tisId,
        credentialType.get());

    return modifiedDate.map(modified -> modified.isBefore(signature.signedAt)).orElse(true);
  }

  /**
   * Validate the provided {@link Instant} against the current Instant.
   *
   * @param instant     The Instant to validate.
   * @param requirePast Whether the Instant must be in the past, else it must be in the future.
   * @return Whether the Instant is valid.
   */
  private boolean isValidInstant(Instant instant, boolean requirePast) {
    Instant now = Instant.now();
    return instant != null && instant.compareTo(now) > 0 != requirePast;
  }

  /**
   * A representation of the signature from a signed data DTO.
   *
   * @param hmac       The hash-based message authentication code.
   * @param signedAt   When the data was signed.
   * @param validUntil When the signature is valid until.
   */
  private record Signature(String hmac, Instant signedAt, Instant validUntil) {

  }
}
