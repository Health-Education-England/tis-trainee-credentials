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

package uk.nhs.hee.tis.trainee.credentials.service;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import io.jsonwebtoken.Claims;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.VerificationProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService.TokenResponse;

/** A service providing credential verification functionality. */
@Slf4j
@Service
@XRayEnabled
public class VerificationService {

  private static final String CREDENTIAL_PREFIX = "openid ";
  private static final String CREDENTIAL_TYPE_IDENTITY = "Identity";

  private static final String CLAIM_NONCE = "nonce";
  private static final String CLAIM_FIRST_NAME = "Identity.ID-LegalFirstName";
  private static final String CLAIM_FAMILY_NAME = "Identity.ID-LegalSurname";
  private static final String CLAIM_BIRTH_DATE = "Identity.ID-BirthDate";
  private static final String CLAIM_TOKEN_IDENTIFIER = "origin_jti";
  private static final String CLAIM_UNIQUE_IDENTIFIER = "Identity.UniqueIdentifier";

  private static final String CODE_CHALLENGE = "code_challenge";
  private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
  private static final String PARAM_REASON = "reason";
  private static final String PARAM_SCOPE = "scope";
  private static final String PARAM_STATE = "state";

  private final GatewayService gatewayService;
  private final JwtService jwtService;
  private final CachingDelegate cachingDelegate;
  private final VerificationProperties properties;
  private final MetricsService metricsService;

  private final LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
  private final DoubleMetaphone metaphone = new DoubleMetaphone();

  /**
   * Create a service providing credential verification functionality.
   *
   * @param gatewayService The service to handle all credential gateway interactions.
   * @param jwtService The JWT service to use.
   * @param cachingDelegate The caching delegate for caching data between requests.
   * @param properties The application's gateway verification configuration.
   */
  VerificationService(
      GatewayService gatewayService,
      JwtService jwtService,
      CachingDelegate cachingDelegate,
      VerificationProperties properties,
      MetricsService metricsService) {
    this.gatewayService = gatewayService;
    this.jwtService = jwtService;
    this.cachingDelegate = cachingDelegate;
    this.properties = properties;
    this.metricsService = metricsService;
  }

  /**
   * Start the identity credential verification process, the result will direct the user to provide
   * an identity credential using the credential gateway.
   *
   * @param authToken The request's authorization token.
   * @param dto The user's identity data.
   * @param clientState The state sent by client when making the request to start verification.
   * @return A URI to a credential gateway prompt for an identity credential.
   */
  public URI startIdentityVerification(
      String authToken, IdentityDataDto dto, @Nullable String clientState) {
    // Cache the provided identity data against the nonce.
    UUID nonce = UUID.randomUUID();
    cachingDelegate.cacheIdentityData(nonce, dto);

    // Cache an ID from the token to represent the session.
    Claims authClaims = jwtService.getClaims(authToken);
    String sessionId = authClaims.get(CLAIM_TOKEN_IDENTIFIER, String.class);
    cachingDelegate.cacheUnverifiedSessionIdentifier(nonce, sessionId);

    // Generate new state for the internal request/callback and cache the client state.
    UUID internalState = UUID.randomUUID();
    cachingDelegate.cacheClientState(internalState, clientState);

    // Generate and cache the code verifier against the state.
    String codeVerifier = generateCodeVerifier();
    cachingDelegate.cacheCodeVerifier(internalState, codeVerifier);
    String codeChallenge = generateCodeChallenge(codeVerifier);

    // Build and return the URI at which the user can provide an identity credential.
    return UriComponentsBuilder.fromUriString(properties.authorizeEndpoint())
        .queryParam(CLAIM_NONCE, nonce)
        .queryParam(PARAM_STATE, internalState)
        .queryParam(CODE_CHALLENGE_METHOD, "S256")
        .queryParam(CODE_CHALLENGE, codeChallenge)
        .queryParam(PARAM_SCOPE, "openid+Identity")
        .build()
        .toUri();
  }

  /**
   * Generate a random PKCE code verifier.
   *
   * @return The generated code verifier.
   */
  private String generateCodeVerifier() {
    SecureRandom secureRandom = new SecureRandom();
    byte[] codeVerifier = new byte[32];
    secureRandom.nextBytes(codeVerifier);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
  }

  /**
   * Generate a code challenge from the code verifier.
   *
   * @param codeVerifier The code verifier to generate a challenge from.
   * @return The generated code challenge.
   */
  private String generateCodeChallenge(String codeVerifier) {
    byte[] codeChallenge = DigestUtils.sha256(codeVerifier);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(codeChallenge);
  }

  /**
   * Complete the credential verification process.
   *
   * @param code The code provided by the credential gateway.
   * @param state The state set in the initial gateway request.
   * @param error The error code from the verification callback.
   * @param errorDescription The error description from the verification callback.
   * @return The built redirect URI for completed verification.
   */
  public URI completeCredentialVerification(
      String code, String state, @Nullable String error, @Nullable String errorDescription) {
    // If successfully verified then continue, else return callback error.
    if (error == null) {
      return completeCredentialVerification(code, state);
    }

    log.info("Credential was not verified.");

    // Build and return the redirect_uri
    UUID stateUuid = UUID.fromString(state);
    Optional<String> clientState = cachingDelegate.getClientState(stateUuid);

    return UriComponentsBuilder.fromUriString("/invalid-credential")
        .queryParamIfPresent(PARAM_STATE, clientState)
        .queryParamIfPresent(PARAM_REASON, Optional.ofNullable(errorDescription))
        .build()
        .toUri();
  }

  /**
   * Complete the credential verification process.
   *
   * @param code The code provided by the credential gateway.
   * @param state The state set in the initial gateway request.
   * @return The built redirect URI for completed verification.
   */
  private URI completeCredentialVerification(String code, String state) {
    UUID stateUuid = UUID.fromString(state);
    Optional<String> codeVerifier = cachingDelegate.getCodeVerifier(stateUuid);
    String failureCode;

    if (codeVerifier.isEmpty()) {
      failureCode = "no_code_verifier";
    } else {
      URI tokenEndpoint = URI.create(properties.tokenEndpoint());
      URI redirectEndpoint = URI.create(properties.redirectUri());
      ResponseEntity<TokenResponse> tokenResponseEntity =
          gatewayService.getTokenResponse(
              tokenEndpoint, redirectEndpoint, code, codeVerifier.get());
      Claims claims = gatewayService.getTokenClaims(tokenResponseEntity);
      String scope = gatewayService.getTokenScope(tokenResponseEntity);
      String credentialType = scope.replace(CREDENTIAL_PREFIX, "");

      if (credentialType.equals(CREDENTIAL_TYPE_IDENTITY)) {
        failureCode = verifyIdentity(claims) ? null : "identity_verification_failed";
      } else {
        // Should never get here as the scope is pre-validated.
        log.error("Unsupported credential type '{}'.", credentialType);
        failureCode = "unsupported_scope";
      }
    }

    UriComponentsBuilder uriBuilder;

    if (failureCode == null) {
      uriBuilder = UriComponentsBuilder.fromUriString("/credential-verified");
    } else {
      uriBuilder =
          UriComponentsBuilder.fromUriString("/invalid-credential")
              .queryParam(PARAM_REASON, failureCode);
    }

    Optional<String> clientState = cachingDelegate.getClientState(stateUuid);
    uriBuilder.queryParamIfPresent(PARAM_STATE, clientState);
    return uriBuilder.build().toUri();
  }

  /**
   * Check the identity claims against the cached identity data for the user.
   *
   * @param claims The credential gateway token claims.
   * @return true if identity matches, else false.
   */
  private boolean verifyIdentity(Claims claims) {
    UUID nonce = UUID.fromString(claims.get(CLAIM_NONCE, String.class));
    Optional<IdentityDataDto> optionalIdentityData = cachingDelegate.getIdentityData(nonce);

    if (optionalIdentityData.isPresent()) {
      IdentityDataDto identityData = optionalIdentityData.get();

      String claimFirstName = claims.get(CLAIM_FIRST_NAME, String.class);
      String claimFamilyName = claims.get(CLAIM_FAMILY_NAME, String.class);
      LocalDate claimBirthDate = LocalDate.parse(claims.get(CLAIM_BIRTH_DATE, String.class));
      String identityId = claims.get(CLAIM_UNIQUE_IDENTIFIER, String.class);

      NameVerificationResult firstNameResult = verifyName(identityData.forenames(), claimFirstName);
      metricsService.publishForenamePhoneticAccuracy(firstNameResult.phoneticAccuracy());
      metricsService.publishForenameTextAccuracy(firstNameResult.textAccuracy());

      NameVerificationResult familyNameResult = verifyName(identityData.surname(), claimFamilyName);
      metricsService.publishSurnamePhoneticAccuracy(familyNameResult.phoneticAccuracy());
      metricsService.publishSurnameTextAccuracy(familyNameResult.textAccuracy());

      if (firstNameResult.valid()
          && familyNameResult.valid()
          && identityData.dateOfBirth().equals(claimBirthDate)
          && identityId != null) {
        Optional<String> sessionIdentifier = cachingDelegate.getUnverifiedSessionIdentifier(nonce);

        // If the unverified session is cached, move it to the verified session cache.
        if (sessionIdentifier.isPresent()) {
          cachingDelegate.cacheVerifiedIdentityIdentifier(
              sessionIdentifier.get(), UUID.fromString(identityId));
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Verify the givens names are similar enough to be considered equal.
   *
   * @param identityName The TIS sourced identity name data.
   * @param claimName The credential sourced identity name data.
   * @return The verification result.
   */
  private NameVerificationResult verifyName(String identityName, String claimName) {
    LinkedHashSet<String> claimNames = new LinkedHashSet<>();
    claimNames.add(claimName);
    claimNames.addAll(List.of(claimName.split("[- ]")));

    NameVerificationResult result =
        claimNames.stream()
            .map(
                name -> {
                  double phoneticAccuracy = calculatePhoneticAccuracy(identityName, name);
                  double textAccuracy = calculateTextAccuracy(identityName, name);

                  // Require 50% text accuracy if the name is a phonetic match, otherwise require
                  // 80%.
                  double requiredTextAccuracy = phoneticAccuracy == 1.0 ? 0.5 : 0.8;
                  boolean valid = textAccuracy >= requiredTextAccuracy;

                  return new NameVerificationResult(name, valid, phoneticAccuracy, textAccuracy);
                })
            .max(
                Comparator.comparingDouble(NameVerificationResult::phoneticAccuracy)
                    .thenComparingDouble(NameVerificationResult::textAccuracy))
            .orElseThrow();

    if (!result.valid()) {
      log.warn(
          "Name validation failed for {} and {} (phonetic: {}, text: {}) .",
          identityName,
          claimName,
          result.phoneticAccuracy(),
          result.textAccuracy());
    }

    return result;
  }

  /**
   * Calculate the text-based accuracy of an identity's name.
   *
   * @param identityName The TIS sourced identity name data.
   * @param claimName The credential sourced identity name data.
   * @return The percentage accuracy between 0.0 and 1.0.
   */
  private double calculateTextAccuracy(String identityName, String claimName) {
    double textDistance =
        levenshteinDistance.apply(identityName.toLowerCase(), claimName.toLowerCase());
    double textLength = Math.max(identityName.length(), claimName.length());
    return 1 - (textDistance / textLength);
  }

  /**
   * Calculate the phonetic-based accuracy of an identity's name.
   *
   * @param identityName The TIS sourced identity name data.
   * @param claimName The credential sourced identity name data.
   * @return The percentage accuracy between 0.0 and 1.0.
   */
  private double calculatePhoneticAccuracy(String identityName, String claimName) {
    String identityCode = metaphone.doubleMetaphone(identityName);
    String claimCode = metaphone.doubleMetaphone(claimName);
    double phoneticDistance =
        LevenshteinDistance.getDefaultInstance().apply(identityCode, claimCode);
    double phoneticLength = Math.max(identityCode.length(), claimCode.length());
    return 1 - (phoneticDistance / phoneticLength);
  }

  /**
   * Check whether there is a verified session for the given request.
   *
   * @param request The request to check the session for.
   * @return true if verified, otherwise false.
   */
  public boolean hasVerifiedSession(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    Claims authClaims = jwtService.getClaims(authorization);
    String sessionId = authClaims.get(CLAIM_TOKEN_IDENTIFIER, String.class);
    Optional<UUID> verifiedIdentityId = cachingDelegate.getVerifiedIdentityIdentifier(sessionId);

    return verifiedIdentityId.isPresent();
  }

  /**
   * A result of verifying the similarity of names.
   *
   * @param name The identity that was checked against.
   * @param phoneticAccuracy The phonetic accuracy of the name.
   * @param textAccuracy The text accuracy of the name.
   */
  private record NameVerificationResult(
      String name, boolean valid, double phoneticAccuracy, double textAccuracy) {}
}
