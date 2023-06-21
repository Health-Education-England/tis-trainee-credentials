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
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementDataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDataDto;

/**
 * A mapper to map between TIS Data and Credential Data.
 */
@Mapper(componentModel = ComponentModel.SPRING)
public interface CredentialDataMapper {

  String METADATA_ORIGIN_VALUE = "NHS England";
  String METADATA_ASSURANCE_POLICY_VALUE = "GPG45";
  String METADATA_ASSURANCE_OUTCOME_VALUE = "High";
  String METADATA_PROVIDER_VALUE = "NHS England";
  String METADATA_VERIFIER_VALUE = "Origin";
  String METADATA_VERIFICATION_METHOD_VALUE = "Record Verification";
  String METADATA_PEDIGREE_VALUE = "Authoritative";

  /**
   * Map a programme membership data DTO to the equivalent credential DTO.
   *
   * @param data The programme membership data to map.
   * @return The mapped programme membership credential.
   */
  @Mapping(target = "metadataOrigin", constant = METADATA_ORIGIN_VALUE)
  @Mapping(target = "metadataAssurancePolicy", constant = METADATA_ASSURANCE_POLICY_VALUE)
  @Mapping(target = "metadataAssuranceOutcome", constant = METADATA_ASSURANCE_OUTCOME_VALUE)
  @Mapping(target = "metadataProvider", constant = METADATA_PROVIDER_VALUE)
  @Mapping(target = "metadataVerifier", constant = METADATA_VERIFIER_VALUE)
  @Mapping(target = "metadataVerificationMethod", constant = METADATA_VERIFICATION_METHOD_VALUE)
  @Mapping(target = "metadataPedigree", constant = METADATA_PEDIGREE_VALUE)
  @Mapping(target = "metadataLastRefresh", expression = "java(LocalDate.now())")
  ProgrammeMembershipCredentialDto toCredential(ProgrammeMembershipDataDto data);

  /**
   * Map a placement data DTO to the equivalent credential DTO.
   *
   * @param data The placement data to map.
   * @return The mapped placement credential.
   */
  @Mapping(target = "metadataOrigin", constant = METADATA_ORIGIN_VALUE)
  @Mapping(target = "metadataAssurancePolicy", constant = METADATA_ASSURANCE_POLICY_VALUE)
  @Mapping(target = "metadataAssuranceOutcome", constant = METADATA_ASSURANCE_OUTCOME_VALUE)
  @Mapping(target = "metadataProvider", constant = METADATA_PROVIDER_VALUE)
  @Mapping(target = "metadataVerifier", constant = METADATA_VERIFIER_VALUE)
  @Mapping(target = "metadataVerificationMethod", constant = METADATA_VERIFICATION_METHOD_VALUE)
  @Mapping(target = "metadataPedigree", constant = METADATA_PEDIGREE_VALUE)
  @Mapping(target = "metadataLastRefresh", expression = "java(LocalDate.now())")
  PlacementCredentialDto toCredential(PlacementDataDto data);
}
