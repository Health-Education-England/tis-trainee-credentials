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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

/**
 * A DTO representing Placement credential data.
 *
 * @param specialty          The placement specialty.
 * @param grade              The placement grade.
 * @param nationalPostNumber The placement national post number (NPN).
 * @param employingBody      The employing body of the placement post.
 * @param site               The placement site (location).
 * @param startDate          The placement's start date.
 * @param endDate            The placement's end date.
 */
public record PlacementDto(
    @JsonProperty(access = Access.WRITE_ONLY)
    String tisId,
    String specialty,
    String grade,
    String nationalPostNumber,
    String employingBody,
    String site,
    LocalDate startDate,
    LocalDate endDate) implements CredentialDataDto {

  @Override
  public Instant getExpiration(Instant issuedAt) {
    return endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
  }

  @Override
  public String getScope() {
    return "issue.Placement";
  }
}
