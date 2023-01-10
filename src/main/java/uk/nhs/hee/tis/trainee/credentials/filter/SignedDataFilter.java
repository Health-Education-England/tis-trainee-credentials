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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class SignedDataFilter extends OncePerRequestFilter {

  private static final String SIGNATURE_FIELD = "signature";
  private static final String SIGNATURE_TIMESTAMP_FIELD = "signatureTimestamp";
  private static final String SIGNATURE_SECRET_KEY = "do-not-actually-use-this";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    CachedBodyHttpServletRequest cachedBodyHttpServletRequest = new CachedBodyHttpServletRequest(
        request);

    if (hasValidSignature(cachedBodyHttpServletRequest)) {
      filterChain.doFilter(cachedBodyHttpServletRequest, response);
    } else {
      response.setStatus(403);
    }
  }

  private boolean hasValidSignature(CachedBodyHttpServletRequest request) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode tree = (ObjectNode) mapper.readTree(request.getInputStream());

    String signature = tree.remove(SIGNATURE_FIELD).asText();
    LocalDate signatureTimestamp = LocalDate.parse(tree.get(SIGNATURE_TIMESTAMP_FIELD).asText());

    if (signature != null && signatureTimestamp != null) {

      if (!signatureTimestamp.isBefore(LocalDate.now().minus(Period.ofDays(1)))) {
        // Signature not expired.

        byte[] treeBytes = mapper.writeValueAsBytes(tree);
        String verificationSignature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
            SIGNATURE_SECRET_KEY).hmacHex(treeBytes);
        return signature.equals(verificationSignature);
      }
    }

    return false;
  }
}
