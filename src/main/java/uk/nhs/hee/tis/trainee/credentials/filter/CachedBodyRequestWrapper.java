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

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.springframework.util.StreamUtils;

/**
 * A request wrapper which caches the content of the original request's input stream so it can be
 * read multiple times.
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

  private final byte[] cachedBody;

  /**
   * Create a request wrapper which caches the content of the original request's input stream so it
   * can be read multiple times.
   *
   * @param request The request to wrap.
   * @throws IOException if the request's input stream could not be cached.
   */
  public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
    super(request);
    InputStream requestInputStream = request.getInputStream();
    cachedBody = StreamUtils.copyToByteArray(requestInputStream);
  }

  @Override
  public ServletInputStream getInputStream() {
    return new CachedBodyInputStream(cachedBody);
  }

  @Override
  public BufferedReader getReader() {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(this.cachedBody);
    return new BufferedReader(new InputStreamReader(inputStream));
  }

  /**
   * An input stream which can be constructed from a cached request body.
   */
  private static class CachedBodyInputStream extends ServletInputStream {

    private final ByteArrayInputStream cachedBody;

    /**
     * Create an input stream from a cached request body.
     *
     * @param cachedBody The cached request body.
     */
    public CachedBodyInputStream(byte[] cachedBody) {
      this.cachedBody = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public int read() {
      return cachedBody.read();
    }

    @Override
    public boolean isFinished() {
      return cachedBody.available() == 0;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener listener) {
      throw new UnsupportedOperationException();
    }
  }
}
