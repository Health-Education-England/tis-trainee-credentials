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

package uk.nhs.hee.tis.trainee.credentials.config;

import static io.micrometer.core.instrument.binder.BaseUnits.PERCENT;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.MeterProvider;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsConfigurationTest {

  private MetricsConfiguration configuration;

  @BeforeEach
  void setUp() {
    configuration = new MetricsConfiguration();
  }

  @Test
  void shouldConfigureIdentityAccuracy() {
    MeterRegistry registry = new SimpleMeterRegistry();

    MeterProvider<DistributionSummary> provider = configuration.identityAccuracy(registry);

    DistributionSummary identityAccuracy = provider.withTags();
    Meter.Id id = identityAccuracy.getId();
    assertThat("Unexpected meter name.", id.getName(), is("identity.accuracy"));
    assertThat("Unexpected base unit.", id.getBaseUnit(), is(PERCENT));

    identityAccuracy.record(0.5);
    assertThat("Unexpected meter scaling.", identityAccuracy.totalAmount(), is(50.0));
  }
}
