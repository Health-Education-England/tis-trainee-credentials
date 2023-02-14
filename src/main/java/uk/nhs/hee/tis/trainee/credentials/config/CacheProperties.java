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

package uk.nhs.hee.tis.trainee.credentials.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;

/**
 * A representation of the application cache properties.
 *
 * @param keyPrefix  The cache key prefix.
 * @param timeToLive The time-to-live properties.
 */
@ConfigurationProperties(prefix = "application.cache")
public record CacheProperties(String keyPrefix, TimeToLiveProperties timeToLive) {

  /**
   * A representation of the application cache time-to-live properties.
   *
   * @param verificationRequest The time-to-live for verification request data.
   * @param verifiedSession     The time-to-live for verified session data.
   */
  public record TimeToLiveProperties(
      @DurationUnit(ChronoUnit.SECONDS)
      Duration verificationRequest,

      @DurationUnit(ChronoUnit.SECONDS)
      Duration verifiedSession,

      @DurationUnit(ChronoUnit.SECONDS)
      Duration credentialMetadata) {

  }
}
