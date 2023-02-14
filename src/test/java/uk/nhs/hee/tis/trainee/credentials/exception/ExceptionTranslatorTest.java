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

package uk.nhs.hee.tis.trainee.credentials.exception;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExceptionTranslatorTest {

  private ExceptionTranslator translator;

  @BeforeEach
  void setUp() {
    translator = new ExceptionTranslator();
  }

  @Test
  void shouldReturnAllViolations() {
    Path path1 = PathImpl.createPathFromString("field1");
    Path path2 = PathImpl.createPathFromString("field2");

    Set<ConstraintViolation<Object>> violations = Set.of(
        constructViolation(path1, "not a supported value"),
        constructViolation(path2, "also not a supported value")
    );
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Map<String, String> errors = translator.handleConstraintViolations(exception);
    assertThat("Unexpected error count.", errors.size(), is(2));
    assertThat("Unexpected error message.", errors.get("field1"), is("not a supported value"));
    assertThat("Unexpected error message.", errors.get("field2"), is("also not a supported value"));
  }

  @Test
  void shouldGetParameterNameWhenInstanceOfPathImpl() {
    Path path = PathImpl.createPathFromString("methodName.field1");

    Set<ConstraintViolation<Object>> violations = Set.of(
        constructViolation(path, "not a supported value")
    );
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Map<String, String> errors = translator.handleConstraintViolations(exception);
    assertThat("Unexpected error count.", errors.size(), is(1));
    assertThat("Unexpected error message.", errors.get("field1"), is("not a supported value"));
  }

  @Test
  void shouldGetFullNameWhenNotInstanceOfPathImpl() {
    Path path = new Path() {
      @NotNull
      @Override
      public Iterator<Node> iterator() {
        return Collections.emptyIterator();
      }

      @Override
      public String toString() {
        return "methodName.field1";
      }
    };

    Set<ConstraintViolation<Object>> violations = Set.of(
        constructViolation(path, "not a supported value")
    );
    ConstraintViolationException exception = new ConstraintViolationException(violations);

    Map<String, String> errors = translator.handleConstraintViolations(exception);
    assertThat("Unexpected error count.", errors.size(), is(1));
    assertThat("Unexpected error message.", errors.get("methodName.field1"),
        is("not a supported value"));
  }

  /**
   * Constructs a {@link ConstraintViolation} for the given path and message, with all other
   * parameters set to default values.
   *
   * @param path    The violation path.
   * @param message The violation message.
   * @return The constructed violation.
   */
  private ConstraintViolation<Object> constructViolation(Path path, String message) {
    return ConstraintViolationImpl.forParameterValidation(message, Map.of(), Map.of(), message,
        Object.class, new Object(), new Object(), "some invalid value", path, null, new Object[0],
        null);
  }
}
