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

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueFinishDto;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueStartDto;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;

/**
 * A mapper to map between issued data and Credential metadata.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface CredentialMetadataMapper {

  /**
   * Map a issue start data DTO to the equivalent credential metadata.
   *
   * @param startDto The issue start data to map.
   * @param finishDto The issue finish data to map.
   * @return The mapped credential metadata.
   */
  @Mapping(source = "startDto.credentialType", target="credentialType")
  @Mapping(source = "startDto.tisId", target="tisId")
  @Mapping(source = "finishDto.credentialId", target="credentialId")
  @Mapping(source = "finishDto.traineeId", target="traineeId")
  @Mapping(source = "finishDto.issuedAt", target="issuedAt")
  @Mapping(source = "finishDto.expiresAt", target="expiresAt")
  CredentialMetadata toCredentialMetadata(IssueStartDto startDto, IssueFinishDto finishDto);

}
