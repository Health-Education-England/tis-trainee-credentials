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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.nhs.hee.tis.trainee.credentials.SignatureTestUtil;

class SignedDataFilterTest {

  private static final Instant SIGNED_AT = Instant.now().minus(Duration.ofDays(1));
  private static final Instant VALID_UNTIL = Instant.now().plus(Duration.ofDays(1));

  private static final String SIGNATURE_SECRET_KEY = "do-not-actually-use-this";

  private static final String SIGNED_DATA_TEMPLATE = """
      {
        "id": 123,
        "signature": {
          "signedAt": "%s",
          "validUntil": "%s"
        }
      }
      """;

  private SignedDataFilter filter;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());

    filter = new SignedDataFilter(mapper, SIGNATURE_SECRET_KEY);
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
  void shouldBeForbiddenWhenNoDataInRequest() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenDataIsNotJson() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent("not JSON".getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenDataIsNotSigned() throws Exception {
    String unsignedData = """
          {
            "id": 123
          }
        """;

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(unsignedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenSignatureHasNoHmac() throws Exception {
    String signedData = """
          {
            "id": 123,
            "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
        """.formatted(SIGNED_AT, VALID_UNTIL);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenSignatureHasNoSignedAt() throws Exception {
    String signedData = SignatureTestUtil.signData("""
          {
            "id": 123,
            "signature": {
              "validUntil": "%s"
            }
          }
        """.formatted(VALID_UNTIL), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "abc"})
  void shouldBeForbiddenWhenSignatureHasInvalidSignedAt(String signedAt) throws Exception {
    String signedData = SignatureTestUtil.signData(
        SIGNED_DATA_TEMPLATE.formatted(signedAt, VALID_UNTIL), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenSignatureHasSignedAtInTheFuture() throws Exception {
    String signedData = SignatureTestUtil.signData(
        SIGNED_DATA_TEMPLATE.formatted(Instant.MAX, VALID_UNTIL), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenSignatureHasNoValidUntil() throws Exception {
    String signedData = SignatureTestUtil.signData("""
          {
            "id": 123,
            "signature": {
              "signedAt": "%s"
            }
          }
        """.formatted(SIGNED_AT), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", "abc"})
  void shouldBeForbiddenWhenSignatureHasInvalidValidUntil(String validUntil) throws Exception {
    String signedData = SignatureTestUtil.signData(
        SIGNED_DATA_TEMPLATE.formatted(SIGNED_AT, validUntil), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldBeForbiddenWhenSignatureHasValidUntilInThePast() throws Exception {
    String signedData = SignatureTestUtil.signData(
        SIGNED_DATA_TEMPLATE.formatted(SIGNED_AT, Instant.MIN), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(403));
    verifyNoInteractions(filterChain);
  }

  @Test
  void shouldCallFilterChainWhenSignatureVerifies() throws Exception {
    String signedData = SignatureTestUtil.signData(
        SIGNED_DATA_TEMPLATE.formatted(SIGNED_AT, VALID_UNTIL), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    assertThat("Unexpected response status.", response.getStatus(), is(200));
    verify(filterChain).doFilter(any(HttpServletRequest.class), eq(response));
  }

  @Test
  void shouldWrapTheRequestInputStreamForDownstreamConsumers() throws Exception {
    String signedData = SignatureTestUtil.signData(
        SIGNED_DATA_TEMPLATE.formatted(SIGNED_AT, VALID_UNTIL), SIGNATURE_SECRET_KEY);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(signedData.getBytes(UTF_8));

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    filter.doFilterInternal(request, response, filterChain);

    ArgumentCaptor<HttpServletRequest> requestCaptor = ArgumentCaptor.forClass(
        HttpServletRequest.class);
    verify(filterChain).doFilter(requestCaptor.capture(), any());

    ServletInputStream inputStream = requestCaptor.getValue().getInputStream();
    assertThat("Unexpected stream ready status.", inputStream.isReady(), is(true));
    assertThat("Unexpected stream finished status.", inputStream.isFinished(), is(false));
    assertThat("Unexpected stream contents.", inputStream.readAllBytes(),
        is(signedData.getBytes(UTF_8)));
  }
}
