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
import io.micrometer.core.instrument.Meter.MeterProvider;
import org.springframework.stereotype.Service;

/** A service for publishing custom application metrics. */
@Service
public class MetricsService {

  private static final String ANALYSIS_TYPE_PHONETIC = "phonetic";
  private static final String ANALYSIS_TYPE_TEXT = "text";
  private static final String NAME_TYPE_FORENAME = "forename";
  private static final String NAME_TYPE_SURNAME = "surname";

  private final MeterProvider<DistributionSummary> identityInaccuracy;

  public MetricsService(MeterProvider<DistributionSummary> identityInaccuracy) {
    this.identityInaccuracy = identityInaccuracy;
  }

  /**
   * Publish an accuracy value for the phonetic-based forename accuracy metric.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   */
  public void publishForenamePhoneticAccuracy(double accuracy) {
    publishNameAccuracy(accuracy, ANALYSIS_TYPE_PHONETIC, NAME_TYPE_FORENAME);
  }

  /**
   * Publish an accuracy value for the text-based forename accuracy metric.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   */
  public void publishForenameTextAccuracy(double accuracy) {
    publishNameAccuracy(accuracy, ANALYSIS_TYPE_TEXT, NAME_TYPE_FORENAME);
  }

  /**
   * Publish an accuracy value for the phonetic-based surname accuracy metric.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   */
  public void publishSurnamePhoneticAccuracy(double accuracy) {
    publishNameAccuracy(accuracy, ANALYSIS_TYPE_PHONETIC, NAME_TYPE_SURNAME);
  }

  /**
   * Publish an accuracy value for the text-based surname accuracy metric.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   */
  public void publishSurnameTextAccuracy(double accuracy) {
    publishNameAccuracy(accuracy, ANALYSIS_TYPE_TEXT, NAME_TYPE_SURNAME);
  }

  /**
   * Publish an accuracy value for the given analysis and name types.
   *
   * @param accuracy The percentage accuracy between 0.0 and 1.0.
   * @param analysisType The analysis type.
   * @param nameType The name type.
   */
  private void publishNameAccuracy(double accuracy, String analysisType, String nameType) {
    if (accuracy < 0.0 || accuracy > 1.0) {
      String message =
          String.format("%s accuracy out of bounds, must be between 0.0 and 1.0.", analysisType);
      throw new IllegalArgumentException(message);
    }

    // The meter does not support minimum values, but does support maximum. The accuracy is inverted
    // so that it can be reported as "inaccuracy" with the maximum representing the lowest accuracy.
    identityInaccuracy
        .withTags(
            "AnalysisType", analysisType,
            "NameType", nameType)
        .record(1 - accuracy);
  }
}
