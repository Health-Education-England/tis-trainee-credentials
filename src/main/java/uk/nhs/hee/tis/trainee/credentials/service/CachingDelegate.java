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

import static uk.nhs.hee.tis.trainee.credentials.config.CacheConfiguration.CREDENTIAL_METADATA;
import static uk.nhs.hee.tis.trainee.credentials.config.CacheConfiguration.VERIFICATION_REQUEST_DATA;
import static uk.nhs.hee.tis.trainee.credentials.config.CacheConfiguration.VERIFIED_SESSION_DATA;

import java.security.PublicKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueRequestDto;

/**
 * Caching annotations do not work within the same class, this delegate provides a lightweight
 * caching interface for services.
 */
@Component
class CachingDelegate {

  private static final String CLIENT_STATE = "ClientState";
  private static final String CODE_VERIFIER = "CodeVerifier";
  private static final String CREDENTIAL_DATA = "CredentialData";
  private static final String IDENTITY_DATA = "IdentityData";
  private static final String PUBLIC_KEY = "PublicKey";
  private static final String TRAINEE_ID = "TraineeIdentifier";
  private static final String CREDENTIAL_LOG_DATA = "CredentialLogData";
  private static final String UNVERIFIED_SESSION_IDENTIFIER = "SessionIdentifier::Unverified";
  private static final String VERIFIED_SESSION_IDENTIFIER = "SessionIdentifier::Verified";

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
   * Cache an credential data dto for later retrieval.
   *
   * @param key The cache key.
   * @param dto The credential data to cache.
   * @return The cached credential data.
   */
  @CachePut(cacheNames = CREDENTIAL_DATA, cacheManager = CREDENTIAL_METADATA, key = "#key")
  public CredentialDto cacheCredentialData(UUID key, CredentialDto dto) {
    return dto;
  }

  /**
   * Cache a credential issuing request dto for later retrieval.
   *
   * @param key  The cache key.
   * @param data The credential issue request metadata to cache.
   * @return The cached credential log data.
   */
  @CachePut(cacheNames = CREDENTIAL_LOG_DATA, cacheManager = CREDENTIAL_METADATA, key = "#key")
  public IssueRequestDto cacheCredentialData(UUID key, IssueRequestDto data) {
    return data;
  }

  /**
   * Get the credential data associated with the given key, any cached value will be removed.
   *
   * @param key The cache key.
   * @return The cached credential data, or an empty optional if not found.
   */
  @Cacheable(cacheNames = CREDENTIAL_DATA, cacheManager = CREDENTIAL_METADATA)
  @CacheEvict(CREDENTIAL_DATA)
  public Optional<CredentialDto> getCredentialData(UUID key) {
    return Optional.empty();
  }

  /**
   * Get the credential issue request metadata associated with the given key, any cached value will
   * be removed.
   *
   * @param key The cache key.
   * @return The cached credential issue request metadata, or an empty optional if not found.
   */
  @Cacheable(cacheNames = CREDENTIAL_LOG_DATA, cacheManager = CREDENTIAL_METADATA)
  @CacheEvict(CREDENTIAL_LOG_DATA)
  public Optional<IssueRequestDto> getCredentialMetadata(UUID key) {
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
   * Cache a public key for later retrieval.
   *
   * @param certificateThumbprint The certificate thumbprint.
   * @param publicKey             The public key to cache.
   * @return The cached public key.
   */
  @CachePut(cacheNames = PUBLIC_KEY, key = "#certificateThumbprint")
  public PublicKey cachePublicKey(String certificateThumbprint, PublicKey publicKey) {
    return publicKey;
  }

  /**
   * Get the public key associated with the give certificate thumbprint.
   *
   * @param certificateThumbprint The certificate thumbprint.
   * @return The cached public key, or an empty optional if not found.
   */
  @Cacheable(cacheNames = PUBLIC_KEY)
  public Optional<PublicKey> getPublicKey(String certificateThumbprint) {
    return Optional.empty();
  }

  /**
   * Cache a trainee identifier for later retrieval.
   *
   * @param key               The cache key.
   * @param traineeIdentifier The trainee identifier to cache.
   * @return The cached trainee identifier.
   */
  @CachePut(cacheNames = TRAINEE_ID, cacheManager = CREDENTIAL_METADATA, key = "#key")
  public String cacheTraineeIdentifier(UUID key, String traineeIdentifier) {
    return traineeIdentifier;
  }

  /**
   * Get the trainee identifier associated with the given key, any cached value will be removed.
   *
   * @param key The cache key.
   * @return The cached trainee identifier, or an empty optional if not found.
   */
  @Cacheable(cacheNames = TRAINEE_ID, cacheManager = CREDENTIAL_METADATA)
  @CacheEvict(TRAINEE_ID)
  public Optional<String> getTraineeIdentifier(UUID key) {
    return Optional.empty();
  }

  /**
   * Cache an unverified session identifier for later retrieval.
   *
   * @param key       The cache key.
   * @param sessionId The session identifier to cache.
   * @return The cached session identifier.
   */
  @CachePut(cacheNames = UNVERIFIED_SESSION_IDENTIFIER,
      cacheManager = VERIFICATION_REQUEST_DATA, key = "#key")
  public String cacheUnverifiedSessionIdentifier(UUID key, String sessionId) {
    return sessionId;
  }

  /**
   * Get the unverified session identifier associated with the given key, any cached value will be
   * removed.
   *
   * @param key The cache key.
   * @return The cached session identifier, or an empty optional if not found.
   */
  @Cacheable(cacheNames = UNVERIFIED_SESSION_IDENTIFIER, cacheManager = VERIFICATION_REQUEST_DATA)
  @CacheEvict(UNVERIFIED_SESSION_IDENTIFIER)
  public Optional<String> getUnverifiedSessionIdentifier(UUID key) {
    return Optional.empty();
  }

  /**
   * Cache a verified session identifier for later retrieval.
   *
   * @param sessionId The identifier of the verified session to cache.
   * @return The cached session identifier.
   */
  @CachePut(cacheNames = VERIFIED_SESSION_IDENTIFIER, cacheManager = VERIFIED_SESSION_DATA)
  public String cacheVerifiedSessionIdentifier(String sessionId) {
    return sessionId;
  }

  /**
   * Get the verified session identifier.
   *
   * @param sessionId The identifier of the verified session.
   * @return The cached session identifier, or an empty optional if not found.
   */
  @Cacheable(cacheNames = VERIFIED_SESSION_IDENTIFIER, cacheManager = VERIFIED_SESSION_DATA)
  public Optional<String> getVerifiedSessionIdentifier(String sessionId) {
    return Optional.empty();
  }
}
