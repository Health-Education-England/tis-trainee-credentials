/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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

import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Service;

/** A service for publishing custom application metrics. */
@Service
public class MetricsService {

  private static final String ACCURACY_ERROR_TEMPLATE =
      "%s accuracy out of bounds, must be between 0.0 and 1.0.";

  private final DistributionSummary identityTextAccuracy;
  private final DistributionSummary identityPhoneticAccuracy;

  public MetricsService(
      DistributionSummary identityTextAccuracy, DistributionSummary identityPhoneticAccuracy) {
    this.identityTextAccuracy = identityTextAccuracy;
    this.identityPhoneticAccuracy = identityPhoneticAccuracy;
  }

  /**
   * Publish an accuracy value for the text identity accuracy metric.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   */
  public void publishIdentityTextAccuracy(double accuracy) {
    if (accuracy < 0.0 || accuracy > 1.0) {
      throw new IllegalArgumentException(ACCURACY_ERROR_TEMPLATE.formatted("text"));
    }

    identityTextAccuracy.record(accuracy);
  }

  /**
   * Publish an accuracy value for the phonetic identity accuracy metric.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   */
  public void publishIdentityPhoneticAccuracy(double accuracy) {
    if (accuracy < 0.0 || accuracy > 1.0) {
      throw new IllegalArgumentException(ACCURACY_ERROR_TEMPLATE.formatted("Phonetic"));
    }

    identityPhoneticAccuracy.record(accuracy);
  }
}
