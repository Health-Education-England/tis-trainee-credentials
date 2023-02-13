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

import static uk.nhs.hee.tis.trainee.credentials.config.CacheConfiguration.VERIFICATION_REQUEST_DATA;
import static uk.nhs.hee.tis.trainee.credentials.config.CacheConfiguration.VERIFIED_SESSION_DATA;

import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;

/**
 * Caching annotations do not work within the same class, this delegate provides a lightweight
 * caching interface for services.
 */
@Component
class CachingDelegate {

  private static final String CLIENT_STATE = "ClientState";
  private static final String CODE_VERIFIER = "CodeVerifier";
  public static final String IDENTITY_DATA = "IdentityData";
  public static final String SESSION_IDENTIFIER = "SessionIdentifier";

  /**
   * Cache a PKCE code verifier for later retrieval.
   *
   * @param key          The cache key.
   * @param codeVerifier The code verifier to cache.
   * @return The cached code verifier.
   */
  @CachePut(cacheNames = CODE_VERIFIER, cacheManager = VERIFICATION_REQUEST_DATA, key = "#key")
  public String cacheCodeVerifier(UUID key, String codeVerifier) {
    return codeVerifier;
  }

  /**
   * Get the PKCE code verifier associated with the given key, any cached value will be removed.
   *
   * @param key The cache key.
   * @return The cached code verifier, or an empty optional if not found.
   */
  @Cacheable(cacheNames = CODE_VERIFIER, cacheManager = VERIFICATION_REQUEST_DATA)
  @CacheEvict(CODE_VERIFIER)
  public Optional<String> getCodeVerifier(UUID key) {
    return Optional.empty();
  }

  /**
   * Cache an identity data dto for later retrieval.
   *
   * @param key The cache key.
   * @param dto The identity data to cache.
   * @return The cached identity data.
   */
  @CachePut(cacheNames = IDENTITY_DATA, cacheManager = VERIFICATION_REQUEST_DATA, key = "#key")
  public IdentityDataDto cacheIdentityData(UUID key, IdentityDataDto dto) {
    return dto;
  }

  /**
   * Get the identity data associated with the given key, any cached value will be removed.
   *
   * @param key The cache key.
   * @return The cached identity data, or an empty optional if not found.
   */
  @Cacheable(cacheNames = IDENTITY_DATA, cacheManager = VERIFICATION_REQUEST_DATA)
  @CacheEvict(IDENTITY_DATA)
  public Optional<IdentityDataDto> getIdentityData(UUID key) {
    return Optional.empty();
  }

  /**
   * Cache a client state for later retrieval.
   *
   * @param key         The cache key.
   * @param clientState The client state to cache.
   * @return The cached client state.
   */
  @CachePut(cacheNames = CLIENT_STATE, cacheManager = VERIFICATION_REQUEST_DATA, key = "#key")
  public String cacheClientState(UUID key, String clientState) {
    return clientState;
  }

  /**
   * Get the client state associated with the given key, any cached value will be removed.
   *
   * @param key The cache key.
   * @return The cached client state, or an empty optional if not found.
   */
  @Cacheable(cacheNames = CLIENT_STATE, cacheManager = VERIFICATION_REQUEST_DATA)
  @CacheEvict(CLIENT_STATE)
  public Optional<String> getClientState(UUID key) {
    return Optional.empty();
  }

  /**
   * Cache a verified session for later retrieval.
   *
   * @param key               The cache key.
   * @param verifiedSessionId The ID of the verified session to cache.
   * @return The cached session id.
   */
  @CachePut(cacheNames = SESSION_IDENTIFIER, cacheManager = VERIFIED_SESSION_DATA, key = "#key")
  public String cacheVerifiedSession(UUID key, String verifiedSessionId) {
    return verifiedSessionId;
  }

  /**
   * Get the verified session associated with the given key.
   *
   * @param key The cache key.
   * @return The cached session id, or an empty optional if not found.
   */
  @Cacheable(cacheNames = SESSION_IDENTIFIER, cacheManager = VERIFIED_SESSION_DATA)
  public Optional<String> getVerifiedSession(UUID key) {
    return Optional.empty();
  }
}
