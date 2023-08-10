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
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * An interface representing a DTO containing credential data.
 */
public interface CredentialDto extends Serializable {

  /**
   * Get the TIS ID of the record, which is recorded in the credential metadata, not the credential
   * itself.
   */
  @JsonIgnore
  String getTisId();

  /**
   * Get the expiry instant for the credential.
   *
   * @param issuedAt The time the credential is being issued.
   * @return The expiry instant of the credential.
   */
  @JsonIgnore
  Instant getExpiration(Instant issuedAt);

  /**
   * Get gateway credential type, known as scope internally.
   *
   * @return The credential's scope.
   */
  @JsonIgnore
  String getScope();

  /**
   * Get internal credential type.
   *
   * @return The credential's type.
   */
  @JsonIgnore
  CredentialType getCredentialType();

  /**
   * Get the unique identifier for identity binding.
   *
   * @return The unique identifier to use for identity binding.
   */
  @JsonIgnore
  UUID getUniqueIdentifier();
}
