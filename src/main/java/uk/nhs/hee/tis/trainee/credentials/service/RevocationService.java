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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
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
@Slf4j
@Service
@XRayEnabled
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
   */
  public void revoke(String tisId, CredentialType credentialType) {
    // Update the modified timestamp.
    Instant timestamp = Instant.now();
    var modificationMetadata = new ModificationMetadata(tisId, credentialType, timestamp);
    modificationMetadataRepository.save(modificationMetadata);
    log.debug("Stored last modified time {} for {} {}.", timestamp, credentialType, tisId);

    // Find this credential in the credential metadata repository. If it exists, then revoke it.
    List<CredentialMetadata> credentialMetadataList =
        credentialMetadataRepository.findByCredentialTypeAndTisId(
            credentialType.getIssuanceScope(), tisId);
    if (!credentialMetadataList.isEmpty()) {
      log.info("{} Issued credential(s) of type {} found for TIS ID {}, revoking.",
          credentialMetadataList.size(), credentialType, tisId);
      credentialMetadataList.forEach(metadata -> {
        gatewayService.revokeCredential(credentialType.getTemplateName(),
            metadata.getCredentialId());
        credentialMetadataRepository.deleteById(metadata.getCredentialId());
        log.info("Credential {} for TIS ID {} has been revoked.", credentialType, tisId);
      });

    } else {
      log.info("No {} credential issued for TIS ID {}, skipped revocation.", credentialType, tisId);
    }
  }

  /**
   * Revoke a credential if it's data is stale.
   *
   * @param credentialId   The credential serial number.
   * @param tisId          The TIS ID of the modified object.
   * @param credentialType The credential type of the modified object.
   * @param since          The timestamp to check for modifications since.
   * @return Whether the data was stale and revoked.
   */
  public boolean revokeIfStale(String credentialId, String tisId, CredentialType credentialType,
      Instant since) {
    log.info("Checking issued credential {} for staleness.", credentialId);
    Optional<Instant> lastModifiedDate = getLastModifiedDate(tisId, credentialType);

    if (lastModifiedDate.isPresent() && lastModifiedDate.get().isAfter(since)) {
      log.info("Stale credential {} found for TIS ID {}, revoking.", credentialType, tisId);
      gatewayService.revokeCredential(credentialType.getTemplateName(), credentialId);
      return true;
    }

    return false;
  }
}
