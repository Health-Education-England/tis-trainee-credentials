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
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.servlet.ServletInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CachedBodyRequestWrapperTest {

  private static final String REQUEST_STRING = "request content";
  private static final byte[] REQUEST_BYTES = REQUEST_STRING.getBytes(StandardCharsets.UTF_8);

  private CachedBodyRequestWrapper requestWrapper;

  @BeforeEach
  void setUp() throws IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setContent(REQUEST_BYTES);

    requestWrapper = new CachedBodyRequestWrapper(request);
  }

  @Test
  void shouldGetInputStreamsWithCachedBody() throws IOException {
    ServletInputStream inputStream1 = requestWrapper.getInputStream();
    assertThat("Unexpected stream ready status.", inputStream1.isReady(), is(true));
    assertThat("Unexpected stream finished status.", inputStream1.isFinished(), is(false));
    assertThat("Unexpected stream contents.", inputStream1.readAllBytes(), is(REQUEST_BYTES));
    assertThat("Unexpected stream finished status.", inputStream1.isFinished(), is(true));

    ServletInputStream inputStream2 = requestWrapper.getInputStream();
    assertThat("Unexpected stream ready status.", inputStream2.isReady(), is(true));
    assertThat("Unexpected stream finished status.", inputStream2.isFinished(), is(false));
    assertThat("Unexpected stream contents.", inputStream2.readAllBytes(), is(REQUEST_BYTES));
    assertThat("Unexpected stream finished status.", inputStream2.isFinished(), is(true));
  }

  @Test
  void shouldGetReadersWithCachedBody() throws IOException {
    BufferedReader reader1 = requestWrapper.getReader();
    assertThat("Unexpected ready status.", reader1.ready(), is(true));
    assertThat("Unexpected request body.", reader1.readLine(), is(REQUEST_STRING));

    BufferedReader reader2 = requestWrapper.getReader();
    assertThat("Unexpected ready status.", reader2.ready(), is(true));
    assertThat("Unexpected request body.", reader2.readLine(), is(REQUEST_STRING));
  }

  @Test
  void shouldThrowExceptionWhenSettingReadListenerOnCachedInputStream() {
    ServletInputStream inputStream = requestWrapper.getInputStream();

    assertThrows(UnsupportedOperationException.class, () -> inputStream.setReadListener(null));
  }
}
