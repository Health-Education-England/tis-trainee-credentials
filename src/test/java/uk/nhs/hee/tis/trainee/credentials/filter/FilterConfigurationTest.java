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

package uk.nhs.hee.tis.trainee.credentials.filter;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.Ordered;

class FilterConfigurationTest {

  private static final String SIGNATURE_SECRET_KEY = "test-secret-key";

  FilterConfiguration configuration;

  @BeforeEach
  void setUp() {
    configuration = new FilterConfiguration();
  }

  @Test
  void shouldRegisterSignedDataFilter() {
    SignedDataFilter filter = new SignedDataFilter(new ObjectMapper(), SIGNATURE_SECRET_KEY);

    var registrationBean = configuration.registerSignedDataFilter(filter);

    SignedDataFilter registeredFilter = registrationBean.getFilter();
    assertThat("Unexpected registered filter.", registeredFilter, sameInstance(filter));
  }

  @Test
  void shouldRegisterSignedDataFilterWithHighestPrecedence() {
    SignedDataFilter filter = new SignedDataFilter(new ObjectMapper(), SIGNATURE_SECRET_KEY);

    var registrationBean = configuration.registerSignedDataFilter(filter);

    int order = registrationBean.getOrder();
    assertThat("Unexpected filter precedence.", order, is(Ordered.HIGHEST_PRECEDENCE));
  }

  @Test
  void shouldRegisterSignedDataFilterOnIssueApiEndpoints() {
    SignedDataFilter filter = new SignedDataFilter(new ObjectMapper(), SIGNATURE_SECRET_KEY);

    var registrationBean = configuration.registerSignedDataFilter(filter);

    Collection<String> urlPatterns = registrationBean.getUrlPatterns();
    assertThat("Unexpected filter pattern count.", urlPatterns.size(), is(1));
    assertThat("Unexpected filter patterns.", urlPatterns, hasItem("/api/issue/*"));
  }
}
