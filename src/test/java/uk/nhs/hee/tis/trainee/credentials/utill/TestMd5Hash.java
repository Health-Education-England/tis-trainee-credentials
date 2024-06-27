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

package uk.nhs.hee.tis.trainee.credentials.utill;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

 /**
  * A utility for generating test JWT tokens.
  */
class TestMd5Hash {
  private static final String DEFAULT_HASH = "00000000000000000000000000000000";

  private Md5Hash utill;

  @BeforeEach
  void setUp() {
    utill = mock(Md5Hash.class);
  }

  @Test
  void shouldUseDefaultHashIfMd5NotAvailable() {
    MockedStatic<MessageDigest> mockedMessageDigest = Mockito.mockStatic(MessageDigest.class);
    mockedMessageDigest.when(() -> MessageDigest.getInstance(any()))
        .thenThrow(new NoSuchAlgorithmException("error"));

    String hash = utill.createMd5Hash("some input");
    assertThat("Unexpected default hash.", hash, is(DEFAULT_HASH));
    mockedMessageDigest.close();
  }

  @Test
  void shouldUseDefaultHashIfInputIsNull() {
    String hash = utill.createMd5Hash(null);
    assertThat("Unexpected default hash.", hash, is(DEFAULT_HASH));
  }
}
