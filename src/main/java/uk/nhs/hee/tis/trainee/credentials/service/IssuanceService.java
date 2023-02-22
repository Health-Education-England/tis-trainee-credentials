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
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

@Service
public class IssuanceService {

  private final GatewayService gatewayService;
  private final JwtService jwtService;
  private final CachingDelegate cachingDelegate;
  private final CredentialMetadataRepository repository;
  private final IssuingProperties properties;

  IssuanceService(GatewayService gatewayService, JwtService jwtService, CachingDelegate cachingDelegate, CredentialMetadataRepository repository, IssuingProperties properties) {
    this.gatewayService = gatewayService;
    this.jwtService = jwtService;
    this.cachingDelegate = cachingDelegate;
    this.repository = repository;
    this.properties = properties;
  }

  public URI startCredentialIssuance(String authToken, CredentialDto dto, @Nullable String clientState) {
    // Cache the provided identity data against the nonce.
    UUID nonce = UUID.randomUUID();
    cachingDelegate.cacheCredentialData(nonce, dto);

    // Generate new state for the internal request/callback and cache the client state.
    UUID internalState = UUID.randomUUID();
    cachingDelegate.cacheClientState(internalState, clientState);

    // Cache an ID from the token to represent the session.
    Claims authClaims = jwtService.getClaims(authToken);
    String traineeId = authClaims.get("custom:tisId", String.class);
    cachingDelegate.cacheTraineeIdentifier(internalState, traineeId);

    return gatewayService.getCredentialUri(dto, nonce.toString(), internalState.toString()).get();
  }

  public URI completeCredentialIssuance(String code, String state) {
    UUID stateUuid = UUID.fromString(state);
    Optional<String> traineeId = cachingDelegate.getTraineeIdentifier(stateUuid);
    String failureCode = null;

    if (traineeId.isEmpty()) {
      failureCode = "invalid_state";
    } else {
      URI tokenEndpoint = URI.create(properties.tokenEndpoint());
      URI redirectEndpoint = URI.create(properties.redirectUri());
      Claims claims = gatewayService.getTokenClaims(tokenEndpoint, redirectEndpoint, code, null);

      UUID nonce = UUID.fromString(claims.get("nonce", String.class));
      Optional<CredentialDto> credentialData = cachingDelegate.getCredentialData(nonce);

      if (credentialData.isEmpty()) {
        failureCode = "invalid_nonce";
      } else {
        CredentialDto credentialDto = credentialData.get();

        CredentialMetadata metadata = new CredentialMetadata();
        metadata.setTisId(credentialDto.getTisId());
        metadata.setCredentialType(credentialDto.getScope());
        metadata.setTraineeId(traineeId.get());
        metadata.setCredentialId(claims.get("SerialNumber", String.class));
        metadata.setIssuedAt(claims.getIssuedAt().toInstant());
        metadata.setExpiresAt(claims.getExpiration().toInstant());

        repository.save(metadata);
      }
    }

    UriComponentsBuilder uriBuilder;

    if (failureCode == null) {
      uriBuilder = UriComponentsBuilder.fromUriString("/credential-issued");
    } else {
      uriBuilder = UriComponentsBuilder.fromUriString("/invalid-credential")
          .queryParam("reason", failureCode);
    }

    Optional<String> clientState = cachingDelegate.getClientState(stateUuid);
    uriBuilder.queryParamIfPresent("state", clientState);
    return uriBuilder.build().toUri();
  }
}
