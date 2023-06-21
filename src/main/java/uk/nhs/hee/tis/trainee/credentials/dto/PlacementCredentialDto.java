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

package uk.nhs.hee.tis.trainee.credentials.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * A DTO representing a Placement credential.
 *
 * @param specialty          The placement specialty.
 * @param grade              The placement grade.
 * @param nationalPostNumber The national post number (NPN) for the post.
 * @param employingBody      The employing body.
 * @param site               The placement site.
 * @param startDate          The placement's start date.
 * @param endDate            The placement's end date.
 */
public record PlacementCredentialDto(
    @JsonIgnore
    String tisId,

    @JsonProperty("TPL-Specialty")
    String specialty,

    @JsonProperty("TPL-Grade")
    String grade,

    @JsonProperty("TPL-PlacementNPN")
    String nationalPostNumber,

    @JsonProperty("TPL-EmployingBodyOfPost")
    String employingBody,

    @JsonProperty("TPL-Site")
    String site,

    @JsonProperty("TPL-PlacementStartDate")
    LocalDate startDate,

    @JsonProperty("TPL-PlacementEndDate")
    LocalDate endDate,

    @JsonProperty("TPL-Origin")
    String metadataOrigin,

    @JsonProperty("TPL-AssurancePolicy")
    String metadataAssurancePolicy,

    @JsonProperty("TPL-AssuranceOutcome")
    String metadataAssuranceOutcome,

    @JsonProperty("TPL-Provider")
    String metadataProvider,

    @JsonProperty("TPL-Verifier")
    String metadataVerifier,

    @JsonProperty("TPL-VerificationMethod")
    String metadataVerificationMethod,

    @JsonProperty("TPL-Pedigree")
    String metadataPedigree,

    @JsonProperty("TPL-LastRefresh")
    LocalDate metadataLastRefresh
) implements CredentialDto {

  @Override
  public Instant getExpiration(Instant issuedAt) {
    return endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
  }

  @Override
  public String getScope() {
    return CredentialType.TRAINING_PLACEMENT.getIssuanceScope();
  }

  @Override
  public CredentialType getCredentialType() {
    return CredentialType.TRAINING_PLACEMENT;
  }

  @Override
  public String getTisId() {
    return tisId;
  }
}
