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

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.VerificationProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;

/**
 * A service providing credential verification functionality.
 */
@Service
public class VerificationService {

  private final CachingDelegate cachingDelegate;
  private final VerificationProperties properties;

  /**
   * Create a service providing credential verification functionality.
   *
   * @param cachingDelegate The caching delegate for caching data between requests.
   * @param properties      The application's gateway verification configuration.
   */
  VerificationService(CachingDelegate cachingDelegate, VerificationProperties properties) {
    this.cachingDelegate = cachingDelegate;
    this.properties = properties;
  }

  /**
   * Start the identity credential verification process, the result will direct the user to provide
   * an identity credential using the credential gateway.
   *
   * @param dto         The user's identity data.
   * @param clientState The state sent by client when making the request to start verification.
   * @return A URI to a credential gateway prompt for an identity credential.
   */
  public URI startIdentityVerification(IdentityDataDto dto,
      @Nullable String clientState) {
    // Cache the provided identity data against the nonce.
    UUID nonce = UUID.randomUUID();
    cachingDelegate.cacheIdentityData(nonce, dto);

    // Generate new state for the internal request/callback and cache the client state.
    UUID internalState = UUID.randomUUID();
    cachingDelegate.cacheClientState(internalState, clientState);

    // Generate and cache the code verifier against the state.
    String codeVerifier = generateCodeVerifier();
    cachingDelegate.cacheCodeVerifier(internalState, codeVerifier);
    String codeChallenge = generateCodeChallenge(codeVerifier);

    // Build and return the URI at which the user can provide an identity credential.
    return UriComponentsBuilder.fromUriString(properties.authorizeEndpoint())
        .queryParam("nonce", nonce)
        .queryParam("state", internalState)
        .queryParam("code_challenge_method", "S256")
        .queryParam("code_challenge", codeChallenge)
        .queryParam("scope", "openid+Identity")
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
}
