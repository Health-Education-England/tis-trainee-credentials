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

package uk.nhs.hee.tis.trainee.credentials.mapper;

import java.util.Optional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialEventDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;

/**
 * A mapper to map to credential events.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface CredentialEventMapper {

  /**
   * Map metadata to a credential event.
   *
   * @param metadata The metadata to map.
   * @return The mapped credential event.
   */
  @Mapping(target = "credentialType", source = "metadata")
  CredentialEventDto toCredentialEvent(CredentialMetadata metadata);

  /**
   * Get the credential type of the given credential metadata.
   *
   * @param metadata The credential metadata to get the type of.
   * @return The credential type if matched, or else null.
   */
  default String getTypeDisplayName(CredentialMetadata metadata) {
    Optional<CredentialType> credentialType = CredentialType.fromIssuanceScope(
        metadata.getCredentialType());
    return credentialType.map(CredentialType::getDisplayName).orElse(null);
  }
}
