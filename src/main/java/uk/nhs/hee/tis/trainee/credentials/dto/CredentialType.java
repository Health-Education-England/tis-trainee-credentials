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

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

/**
 * An enumeration of available credential types.
 */
@Getter
public enum CredentialType {

  TRAINING_PLACEMENT("issue.TrainingPlacement", "/api/issue/placement"),
  TRAINING_PROGRAMME("issue.TrainingProgramme", "/api/issue/programme-membership");

  private final String gatewayScope;
  private final String apiPath;

  /**
   * Construct a credential type.
   *
   * @param gatewayScope The gateway scope e.g. issue.DataType.
   * @param apiPath      The API request path for the credential type.
   */
  CredentialType(String gatewayScope, String apiPath) {
    this.gatewayScope = gatewayScope;
    this.apiPath = apiPath;
  }

  /**
   * Get the credential type matching the given path.
   *
   * @param apiPath The API path to match.
   * @return The matched credential type, or else empty.
   */
  public static Optional<CredentialType> fromPath(String apiPath) {
    return Arrays.stream(CredentialType.values())
        .filter(ct -> ct.apiPath.equals(apiPath))
        .findFirst();
  }
}
