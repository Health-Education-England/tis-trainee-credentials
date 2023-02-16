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
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

/**
 * A service providing credential verification functionality.
 */
@Service
@Slf4j
public class IssuedResourceService {

  private final GatewayService service;
  private final CredentialMetadataRepository credentialMetadataRepository;

  private final CredentialMetadataMapper credentialMetadataMapper;
  private final CachingDelegate cachingDelegate;
  private final IssuingProperties properties;

  /**
   * Create a service providing credential verification functionality.
   *
   * @param service                      The gateway service.
   * @param credentialMetadataRepository The credential log repository.
   * @param credentialMetadataMapper     The mapper for credential metadata.
   * @param cachingDelegate              The caching delegate for caching data between requests.
   * @param properties                   The application's gateway verification configuration.
   */
  IssuedResourceService(GatewayService service,
                        CredentialMetadataRepository credentialMetadataRepository,
                        CredentialMetadataMapper credentialMetadataMapper,
                        CachingDelegate cachingDelegate, IssuingProperties properties) {
    this.service = service;
    this.credentialMetadataRepository = credentialMetadataRepository;
    this.credentialMetadataMapper = credentialMetadataMapper;
    this.cachingDelegate = cachingDelegate;
    this.properties = properties;
  }

  /**
   * Log the issued credential, and get the redirect.
   *
   * @param authToken        The user's authorization token.
   * @param code             The PAR response code.
   * @param state            The PAR response state.
   * @param error            The PAR response error.
   * @param errorDescription The PAR response error description.
   * @return The redirect URI
   */
  public URI logIssuedResource(String authToken, String code, String state, String error,
                               String errorDescription) {

    CredentialMetadata credentialMetadata = null;
    if (error == null) {
      log.info("Credential was issued successfully.");
      URI tokenEndpoint = URI.create(properties.tokenEndpoint());
      URI redirectEndpoint = URI.create(properties.callbackUri());
      Claims claims = service.getTokenClaims(tokenEndpoint, redirectEndpoint, code, null);
      try {
        credentialMetadata = credentialMetadataMapper.toCredentialMetadata(claims, authToken);
      } catch (IOException e) {
        log.error("Unable to retrieve cached metadata, could not log issued credential.", e);
      }
    } else {
      log.info("Credential was not issued.");
    }

    if (credentialMetadata != null) {
      credentialMetadataRepository.save(credentialMetadata);
    }

    // Build and return the redirect_uri
    return UriComponentsBuilder.fromUriString(properties.redirectUri())
        .queryParam("code", code)
        .queryParam("state", state)
        .queryParam("error", error)
        .queryParam("error_description", errorDescription)
        .build()
        .toUri();
  }

  public Optional<CredentialMetadata> getFromCache(UUID id) {
    return cachingDelegate.getCredentialMetadata(id);
  }
}
