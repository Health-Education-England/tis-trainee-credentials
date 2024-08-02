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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.micrometer.core.instrument.DistributionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MetricsServiceTest {

  private MetricsService service;
  private DistributionSummary textAccuracy;
  private DistributionSummary phoneticAccuracy;

  @BeforeEach
  void setUp() {
    textAccuracy = mock(DistributionSummary.class);
    phoneticAccuracy = mock(DistributionSummary.class);
    service = new MetricsService(textAccuracy, phoneticAccuracy);
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void shouldNotPublishIdentityTextAccuracyWhenInvalid(double accuracy) {
    assertThrows(
        IllegalArgumentException.class, () -> service.publishIdentityTextAccuracy(accuracy));

    verifyNoInteractions(textAccuracy);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 0.5, 1.0})
  void shouldPublishIdentityTextAccuracyWhenValid(double accuracy) {
    service.publishIdentityTextAccuracy(accuracy);

    verify(textAccuracy).record(accuracy);
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void shouldNotPublishIdentityPhoneticAccuracyWhenInvalid(double accuracy) {
    assertThrows(
        IllegalArgumentException.class, () -> service.publishIdentityPhoneticAccuracy(accuracy));

    verifyNoInteractions(textAccuracy);
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 0.5, 1.0})
  void shouldPublishIdentityPhoneticAccuracyWhenValid(double accuracy) {
    service.publishIdentityPhoneticAccuracy(accuracy);

    verify(phoneticAccuracy).record(accuracy);
  }
}
