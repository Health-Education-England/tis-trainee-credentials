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

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.BaseUnits;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration of custom application metrics. */
@Configuration
public class MetricsConfiguration {

  /**
   * A distribution summary for the accuracy of text based identity verification.
   *
   * @param registry The meter registry to use.
   * @return The build distribution summary.
   */
  @Bean
  DistributionSummary identityTextAccuracy(MeterRegistry registry) {
    return DistributionSummary.builder("identity.accuracy.text")
        .description("How closely matched the text of a given identity was to TIS data.")
        .baseUnit(BaseUnits.PERCENT)
        .scale(100)
        .maximumExpectedValue(100.0)
        .register(registry);
  }

  /**
   * A distribution summary for the accuracy of phonetic based identity verification.
   *
   * @param registry The meter registry to use.
   * @return The build distribution summary.
   */
  @Bean
  DistributionSummary identityPhoneticAccuracy(MeterRegistry registry) {
    return DistributionSummary.builder("identity.accuracy.phonetic")
        .description("How closely matched the phonetics of a given identity was to TIS data.")
        .baseUnit(BaseUnits.PERCENT)
        .scale(100)
        .maximumExpectedValue(100.0)
        .register(registry);
  }
}
