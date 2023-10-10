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

  TRAINING_PLACEMENT("Training Placement", "TrainingPlacement", "/api/issue/placement"),
  TRAINING_PROGRAMME("Training Programme", "TrainingProgramme", "/api/issue/programme-membership");

  private static final String ISSUANCE_SCOPE_PREFIX = "issue.";

  private final String displayName;
  private final String templateName;
  private final String apiPath;

  /**
   * Construct a credential type.
   *
   * @param displayName  The name displayed to users.
   * @param templateName The gateway credential template name.
   * @param apiPath      The API request path for the credential type.
   */
  CredentialType(String displayName, String templateName, String apiPath) {
    this.displayName = displayName;
    this.templateName = templateName;
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

  /**
   * Get the credential type matching the given issuance scope.
   *
   * @param issuanceScope The issuance scope to match.
   * @return The matched credential type, or else empty.
   */
  public static Optional<CredentialType> fromIssuanceScope(String issuanceScope) {
    String templateName = issuanceScope.replaceFirst(ISSUANCE_SCOPE_PREFIX, "");

    return Arrays.stream(CredentialType.values())
        .filter(ct -> ct.templateName.equals(templateName))
        .findFirst();
  }

  /**
   * Get issuance scope e.g. issue.TemplateName
   *
   * @return The issuance scope.
   */
  public String getIssuanceScope() {
    return ISSUANCE_SCOPE_PREFIX + templateName;
  }
}
