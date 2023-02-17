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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.credentials.service.VerificationService;

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
  void shouldRegisterSignedDataFilterOnIssueApiEndpoints() {
    SignedDataFilter filter = new SignedDataFilter(new ObjectMapper(), SIGNATURE_SECRET_KEY);

    var registrationBean = configuration.registerSignedDataFilter(filter);

    Collection<String> urlPatterns = registrationBean.getUrlPatterns();
    assertThat("Unexpected filter patterns.", urlPatterns, hasItem("/api/issue/*"));
  }

  @Test
  void shouldRegisterSignedDataFilterOnVerifyApiEndpoints() {
    SignedDataFilter filter = new SignedDataFilter(new ObjectMapper(), SIGNATURE_SECRET_KEY);

    var registrationBean = configuration.registerSignedDataFilter(filter);

    Collection<String> urlPatterns = registrationBean.getUrlPatterns();
    assertThat("Unexpected filter patterns.", urlPatterns, hasItem("/api/verify/*"));
  }

  @Test
  void shouldRegisterVerifiedSessionFilter() {
    VerificationService verificationService = mock(VerificationService.class);
    VerifiedSessionFilter filter = new VerifiedSessionFilter(verificationService);

    var registrationBean = configuration.registerVerifiedSessionFilter(filter);

    VerifiedSessionFilter registeredFilter = registrationBean.getFilter();
    assertThat("Unexpected registered filter.", registeredFilter, sameInstance(filter));
  }

  @Test
  void shouldRegisterVerifiedSessionFilterOnIssueApiEndpoints() {
    VerificationService verificationService = mock(VerificationService.class);
    VerifiedSessionFilter filter = new VerifiedSessionFilter(verificationService);

    var registrationBean = configuration.registerVerifiedSessionFilter(filter);

    Collection<String> urlPatterns = registrationBean.getUrlPatterns();
    assertThat("Unexpected filter patterns.", urlPatterns, hasItem("/api/issue/*"));
  }

  @Test
  void shouldNotRegisterVerifiedSessionFilterOnVerifyApiEndpoints() {
    VerificationService verificationService = mock(VerificationService.class);
    VerifiedSessionFilter filter = new VerifiedSessionFilter(verificationService);

    var registrationBean = configuration.registerVerifiedSessionFilter(filter);

    Collection<String> urlPatterns = registrationBean.getUrlPatterns();
    assertThat("Unexpected filter patterns.", urlPatterns, not(hasItem("/api/verify/*")));
  }
}
