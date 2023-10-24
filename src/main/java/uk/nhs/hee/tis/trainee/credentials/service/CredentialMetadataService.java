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

import static java.util.stream.Collectors.groupingBy;

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

/**
 * A service for handling credentials.
 */
@Slf4j
@Service
@XRayEnabled
public class CredentialMetadataService {

  private final CredentialMetadataRepository credentialMetadataRepository;

  /**
   * Constructs a new CredentialMetadataService with the provided CredentialMetadataRepository.
   *
   * @param credentialMetadataRepository The repository used for managing CredentialMetadata.
   */
  CredentialMetadataService(CredentialMetadataRepository credentialMetadataRepository) {
    this.credentialMetadataRepository = credentialMetadataRepository;
  }

  /**
   * A method to get and return the latest credential by issuedAt time. This method retrieves a list
   * of credential metadata entries of the specified credential type and trainee ID, and then
   * filters and collects the latest metadata entry for each unique TIS ID based on the issuance
   * date.
   *
   * @param type      The type of credentials to retrieve.
   * @param traineeId The ID of the trainee for to retrieve metadata for.
   * @return A list of the latest credentials for the specified credential type and trainee. An
   *     empty list is returned if no matching credentials are found.
   * @throws NoSuchElementException If no credential metadata with a valid 'issuedAt' timestamp is
   *                                found for any of the retrieved entries.
   */
  public List<CredentialMetadata> getLatestCredentialsForType(CredentialType type,
      String traineeId) {

    List<CredentialMetadata> credMeta = credentialMetadataRepository
        .findByCredentialTypeAndTraineeId(type.getIssuanceScope(), traineeId);

    return credMeta.stream()
        .collect(groupingBy(CredentialMetadata::getTisId))
        .values()
        .stream()
        .map(list -> list.stream()
            .filter(meta -> meta.getIssuedAt() != null)
            .max(Comparator.comparing(CredentialMetadata::getIssuedAt))
            .orElseThrow(
                () -> new NoSuchElementException("credentials getIssuedAt value not present")))
        .collect(Collectors.toList());
  }
}
