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

import io.jsonwebtoken.Claims;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueResponseDto;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

/**
 * A service providing credential issuance functionality.
 */
@Slf4j
@Service
public class IssuanceService {

  private final GatewayService gatewayService;
  private final JwtService jwtService;
  private final RevocationService revocationService;
  private final CachingDelegate cachingDelegate;
  private final CredentialMetadataRepository credentialMetadataRepository;
  private final CredentialMetadataMapper credentialMetadataMapper;
  private final IssuingProperties properties;

  /**
   * Create a service providing credential issuance functionality.
   *
   * @param gatewayService  The service to handle all credential gateway interactions.
   * @param jwtService      The JWT service to use.
   * @param cachingDelegate The caching delegate for caching data between requests.
   */
  IssuanceService(GatewayService gatewayService, JwtService jwtService,
      RevocationService revocationService, CachingDelegate cachingDelegate,
      CredentialMetadataRepository credentialMetadataRepository,
      CredentialMetadataMapper credentialMetadataMapper, IssuingProperties properties) {
    this.gatewayService = gatewayService;
    this.jwtService = jwtService;
    this.revocationService = revocationService;
    this.cachingDelegate = cachingDelegate;
    this.credentialMetadataRepository = credentialMetadataRepository;
    this.credentialMetadataMapper = credentialMetadataMapper;
    this.properties = properties;
  }

  /**
   * Start the issuance of a credential.
   *
   * @param authToken   The request's authorization token.
   * @param dto         The user's credential data.
   * @param clientState The state sent by client when making the request to start verification.
   * @return The URI to continue the issuing via credential gateway, or empty if issuing failed.
   */
  public Optional<URI> startCredentialIssuance(String authToken, CredentialDto dto,
      @Nullable String clientState) {
    // Cache the provided credential data against the nonce.
    UUID nonce = UUID.randomUUID();
    cachingDelegate.cacheCredentialData(nonce, dto);

    // Generate new state for the internal request/callback and cache the client state.
    UUID internalState = UUID.randomUUID();
    cachingDelegate.cacheClientState(internalState, clientState);

    // Cache an ID from the token to represent the session.
    Claims authClaims = jwtService.getClaims(authToken);
    String traineeId = authClaims.get("custom:tisId", String.class);
    cachingDelegate.cacheTraineeIdentifier(internalState, traineeId);

    // Cache the current timestamp as the start of the issuance.
    cachingDelegate.cacheIssuanceTimestamp(internalState, Instant.now());

    return gatewayService.getCredentialUri(dto, nonce.toString(), internalState.toString());
  }

  /**
   * Complete the credential issuance process.
   *
   * @param code             The code provided by the credential gateway.
   * @param state            The state set in the initial gateway request.
   * @param error            The error text, if the credential was not issued.
   * @param errorDescription The error description, if the credential was not issued.
   * @return The built redirect URI for completed issuance.
   */
  public URI completeCredentialVerification(String code, String state, String error,
      String errorDescription) {
    UUID stateUuid = UUID.fromString(state);
    CredentialMetadata credentialMetadata = null;

    if (error == null && code != null) {
      log.info("Credential was issued successfully.");
      URI tokenEndpoint = URI.create(properties.tokenEndpoint());
      URI redirectEndpoint = URI.create(properties.redirectUri());
      Claims claims = gatewayService.getTokenClaims(tokenEndpoint, redirectEndpoint, code, null);
      UUID nonceUuid = UUID.fromString(claims.get("nonce", String.class));

      Optional<String> traineeId = cachingDelegate.getTraineeIdentifier(stateUuid);
      Optional<CredentialDto> optionalCredentialData = cachingDelegate.getCredentialData(nonceUuid);
      if (traineeId.isPresent() && optionalCredentialData.isPresent()) {
        CredentialDto credentialData = optionalCredentialData.get();
        IssueResponseDto issueResponseDto = fromIssuedResponse(claims, traineeId.get());

        Optional<Error> staleDataError = revokeIfCredentialStale(stateUuid, credentialData);
        if (staleDataError.isPresent()) {
          error = staleDataError.get().code();
          errorDescription = staleDataError.get().description();
        } else {
          credentialMetadata = credentialMetadataMapper
              .toCredentialMetadata(traineeId.get(), credentialData, issueResponseDto);
        }
      }
    } else {
      log.info("Credential was not issued.");
    }

    if (credentialMetadata != null) {
      credentialMetadataRepository.save(credentialMetadata);
    }

    Optional<String> clientState = cachingDelegate.getClientState(stateUuid);

    // Build and return the redirect_uri
    return UriComponentsBuilder.fromUriString("/credential-issued")
        .queryParamIfPresent("state", clientState)
        .queryParamIfPresent("error", Optional.ofNullable(error))
        .queryParamIfPresent("error_description", Optional.ofNullable(errorDescription))
        .build()
        .toUri();
  }

  /**
   * Build an issue response DTO from token claims and the user's authorization token.
   *
   * @param claims       The Claims data to map.
   * @param traineeTisId The user's trainee TIS ID.
   * @return The mapped credential log entry.
   */

  private IssueResponseDto fromIssuedResponse(Claims claims, String traineeTisId) {
    String credentialId = claims.get("SerialNumber", String.class);
    Instant issuedAt = Instant.ofEpochSecond(claims.get("iat", Long.class));
    Instant expiresAt = Instant.ofEpochSecond(claims.get("exp", Long.class));

    return new IssueResponseDto(credentialId, traineeTisId, issuedAt, expiresAt);
  }

  /**
   * Revoke the newly issued credential if the data used is stale.
   *
   * @param internalState The issue request state.
   * @param credentialDto The issued credential data.
   * @return Associated errors if the data was stale, else empty.
   */
  private Optional<Error> revokeIfCredentialStale(UUID internalState, CredentialDto credentialDto) {
    Error error = null;

    String tisId = credentialDto.getTisId();
    CredentialType credentialType = credentialDto.getCredentialType();
    Optional<Instant> issuanceTimestamp = cachingDelegate.getIssuanceTimestamp(internalState);

    // If unknown issuance timestamp then force staleness if modification exists.
    boolean revoked = revocationService.revokeIfStale(tisId, credentialType,
        issuanceTimestamp.orElse(Instant.MIN));

    if (revoked) {
      if (issuanceTimestamp.isEmpty()) {
        error = new Error("unknown_data_freshness",
            "The issued credential data could not be verified and has been revoked");
      } else {
        error = new Error("stale_data",
            "The issued credential data was stale and has been revoked");
      }
    }

    return Optional.ofNullable(error);
  }

  /**
   * Represents an error code and message.
   *
   * @param code        The error's code.
   * @param description The error's description.
   */
  private record Error(String code, String description) {

  }
}
