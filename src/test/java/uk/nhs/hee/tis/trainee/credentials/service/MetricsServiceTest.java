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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MetricsServiceTest {

  private MetricsService service;
  MeterProvider<DistributionSummary> identityAccuracyProvider;
  private DistributionSummary identityAccuracy;

  @BeforeEach
  void setUp() {
    identityAccuracyProvider = mock(MeterProvider.class);

    when(identityAccuracyProvider.withTags(any(String[].class)))
        .thenAnswer(
            invocation -> {
              String[] tags =
                  Arrays.stream(invocation.getArguments())
                      .map(Object::toString)
                      .toArray(String[]::new);
              identityAccuracy =
                  spy(
                      DistributionSummary.builder("test.meter")
                          .tags(tags)
                          .register(new SimpleMeterRegistry()));
              return identityAccuracy;
            });

    service = new MetricsService(identityAccuracyProvider);
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void shouldNotPublishForenameTextAccuracyWhenInvalid(double accuracy) {
    assertThrows(
        IllegalArgumentException.class, () -> service.publishForenameTextAccuracy(accuracy));

    verifyNoInteractions(identityAccuracyProvider);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          0.0  | 1.0
          0.25 | 0.75
          0.5  | 0.5
          0.75 | 0.25
          1.0  | 0.0
          """)
  void shouldPublishForenameTextAccuracyWhenValid(double accuracy, double inaccuracy) {
    service.publishForenameTextAccuracy(accuracy);

    verify(identityAccuracy).record(inaccuracy);

    String analysisType = identityAccuracy.getId().getTag("AnalysisType");
    assertThat("Unexpected AnalysisType.", analysisType, is("text"));

    String nameType = identityAccuracy.getId().getTag("NameType");
    assertThat("Unexpected NameType.", nameType, is("forename"));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void shouldNotPublishForenamePhoneticAccuracyWhenInvalid(double accuracy) {
    assertThrows(
        IllegalArgumentException.class, () -> service.publishForenamePhoneticAccuracy(accuracy));

    verifyNoInteractions(identityAccuracyProvider);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          0.0  | 1.0
          0.25 | 0.75
          0.5  | 0.5
          0.75 | 0.25
          1.0  | 0.0
         """)
  void shouldPublishForenamePhoneticAccuracyWhenValid(double accuracy, double inaccuracy) {
    service.publishForenamePhoneticAccuracy(accuracy);

    verify(identityAccuracy).record(inaccuracy);

    String analysisType = identityAccuracy.getId().getTag("AnalysisType");
    assertThat("Unexpected AnalysisType.", analysisType, is("phonetic"));

    String nameType = identityAccuracy.getId().getTag("NameType");
    assertThat("Unexpected NameType.", nameType, is("forename"));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void shouldNotPublishSurnameTextAccuracyWhenInvalid(double accuracy) {
    assertThrows(
        IllegalArgumentException.class, () -> service.publishSurnameTextAccuracy(accuracy));

    verifyNoInteractions(identityAccuracyProvider);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          0.0  | 1.0
          0.25 | 0.75
          0.5  | 0.5
          0.75 | 0.25
          1.0  | 0.0
          """)
  void shouldPublishSurnameTextAccuracyWhenValid(double accuracy, double inaccuracy) {
    service.publishSurnameTextAccuracy(accuracy);

    verify(identityAccuracy).record(inaccuracy);

    String analysisType = identityAccuracy.getId().getTag("AnalysisType");
    assertThat("Unexpected AnalysisType.", analysisType, is("text"));

    String nameType = identityAccuracy.getId().getTag("NameType");
    assertThat("Unexpected NameType.", nameType, is("surname"));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void shouldNotPublishSurnamePhoneticAccuracyWhenInvalid(double accuracy) {
    assertThrows(
        IllegalArgumentException.class, () -> service.publishSurnamePhoneticAccuracy(accuracy));

    verifyNoInteractions(identityAccuracyProvider);
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      textBlock =
          """
          0.0  | 1.0
          0.25 | 0.75
          0.5  | 0.5
          0.75 | 0.25
          1.0  | 0.0
          """)
  void shouldPublishSurnamePhoneticAccuracyWhenValid(double accuracy, double inaccuracy) {
    service.publishSurnamePhoneticAccuracy(accuracy);

    verify(identityAccuracy).record(inaccuracy);

    String analysisType = identityAccuracy.getId().getTag("AnalysisType");
    assertThat("Unexpected AnalysisType.", analysisType, is("phonetic"));

    String nameType = identityAccuracy.getId().getTag("NameType");
    assertThat("Unexpected NameType.", nameType, is("surname"));
  }
}
