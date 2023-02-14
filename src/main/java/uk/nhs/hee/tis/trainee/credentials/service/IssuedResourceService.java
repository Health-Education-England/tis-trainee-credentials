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
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

/**
 * A service providing credential verification functionality.
 */
@Service
public class IssuedResourceService {

  private final CredentialMetadataRepository credentialMetadataRepository;

  private final CachingDelegate cachingDelegate;
  private final IssuingProperties properties;

  /**
   * Create a service providing credential verification functionality.
   *
   * @param credentialMetadataRepository The credential log repository.
   * @param cachingDelegate              The caching delegate for caching data between requests.
   * @param properties                   The application's gateway verification configuration.
   */
  IssuedResourceService(CredentialMetadataRepository credentialMetadataRepository,
                        CachingDelegate cachingDelegate,
                        IssuingProperties properties) {
    this.credentialMetadataRepository = credentialMetadataRepository;
    this.cachingDelegate = cachingDelegate;
    this.properties = properties;
  }

  /**
   * Log the issued credential, and get the redirect.
   *
   * @param credentialMetadata The credential metadata.
   * @return The redirect URI
   */
  public URI logIssuedResource(CredentialMetadata credentialMetadata, String code, String state,
                               String error, String errorDescription) {

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
