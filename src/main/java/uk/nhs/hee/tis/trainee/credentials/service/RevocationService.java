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

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.model.ModificationMetadata;
import uk.nhs.hee.tis.trainee.credentials.model.ModificationMetadata.ModificationMetadataId;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;
import uk.nhs.hee.tis.trainee.credentials.repository.ModificationMetadataRepository;

/**
 * A service providing credential revocation functionality.
 */
@Service
public class RevocationService {
  private final CredentialMetadataRepository credentialMetadataRepository;

  private final ModificationMetadataRepository modificationMetadataRepository;
  private final GatewayService gatewayService;

  RevocationService(CredentialMetadataRepository credentialMetadataRepository,
                    ModificationMetadataRepository modificationMetadataRepository,
                    GatewayService gatewayService) {
    this.credentialMetadataRepository = credentialMetadataRepository;
    this.modificationMetadataRepository = modificationMetadataRepository;
    this.gatewayService = gatewayService;
  }

  /**
   * Get the last modified date for the record with matching credential type and ID.
   *
   * @param tisId          The TIS ID of the modified object.
   * @param credentialType The credential type of the modified object.
   * @return The {@link Instant} of the last modification, or empty if never modified.
   */
  public Optional<Instant> getLastModifiedDate(String tisId, CredentialType credentialType) {
    ModificationMetadataId id = new ModificationMetadataId(tisId, credentialType);
    Optional<ModificationMetadata> metadata = modificationMetadataRepository.findById(id);

    return metadata.map(ModificationMetadata::lastModifiedDate);
  }

  /**
   * Revoke any issued credentials for matching credential type and ID.
   *
   * @param tisId          The TIS ID of the modified object.
   * @param credentialType The credential type of the modified object.
   * @return The {@link Instant} of the last modification, or empty if never modified.
   */
  public void revoke(String tisId, CredentialType credentialType) {
    //find this credential in the credential metadata repository
    //if it exists, then revoke it
    Optional<CredentialMetadata> metadata
        = credentialMetadataRepository.findByCredentialTypeAndTisId(
            credentialType.getGatewayScope(), tisId);
    if (metadata.isPresent()) {
      gatewayService.revokeCredential(credentialType.getGatewayScope(),
          metadata.get().getCredentialId());
      //TODO: remove 'issue.' from gateway scope?
    }
  }
}
