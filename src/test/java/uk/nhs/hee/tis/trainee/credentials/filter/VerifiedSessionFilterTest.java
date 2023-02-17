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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.nhs.hee.tis.trainee.credentials.service.VerificationService;

class VerifiedSessionFilterTest {

  private VerifiedSessionFilter filter;
  private VerificationService verificationService;

  @BeforeEach
  void setUp() {
    verificationService = mock(VerificationService.class);
    this.filter = new VerifiedSessionFilter(verificationService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"api/issue/callback", "api/verify/callback", "/callback"})
  void shouldNotFilterCallbacks(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(uri);

    boolean filterInactive = filter.shouldNotFilter(request);
    assertThat("Unexpected filter activation.", filterInactive, is(true));
  }

  @ParameterizedTest
  @ValueSource(strings = {"api/issue/credential", "api/verify/identity", "/not-a-callback"})
  void shouldFilterNonCallbacks(String uri) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(uri);

    boolean filterInactive = filter.shouldNotFilter(request);
    assertThat("Unexpected filter deactivation.", filterInactive, is(false));
  }

  @Test
  void shouldBeUnauthorizedWhenSessionNotVerified() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(verificationService.hasVerifiedSession(request)).thenReturn(false);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(401));
    assertThat("Unexpected response status.", response.getHeader(HttpHeaders.WWW_AUTHENTICATE),
        is("IdentityVerification realm=\"/api/verify/identity\""));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldCallFilterChainWhenSessionVerified() throws ServletException, IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(verificationService.hasVerifiedSession(request)).thenReturn(true);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(200));
    verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));
  }
}
