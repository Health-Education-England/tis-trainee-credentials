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

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialMetadataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueResponseDto;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;

/**
 * A mapper to map between issued data and Credential metadata.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface CredentialMetadataMapper {

  /**
   * Map the issue request and response data DTOs to the equivalent credential metadata.
   *
   * @param traineeId      The trainee ID to map.
   * @param credentialData The credential data to map.
   * @param responseDto    The issue response data to map.
   * @return The mapped credential metadata.
   */
  @Mapping(target = "credentialId", source = "responseDto.credentialId")
  @Mapping(target = "traineeId", source = "traineeId")
  @Mapping(target = "credentialType", source = "credentialData.scope")
  @Mapping(target = "tisId", source = "credentialData.tisId")
  @Mapping(target = "issuedAt", source = "responseDto.issuedAt")
  @Mapping(target = "expiresAt", source = "responseDto.expiresAt")
  CredentialMetadata toCredentialMetadata(String traineeId, CredentialDto credentialData,
      IssueResponseDto responseDto);

  /**
   * Converts a list of CredentialMetadata entities to a list of CredentialMetadataDto objects.
   *
   * @param entities A list of CredentialMetadata entities to be converted.
   * @return A list of CredentialMetadataDto objects representing the converted entities.
   */
  List<CredentialMetadataDto> toDtos(List<CredentialMetadata> entities);
}
