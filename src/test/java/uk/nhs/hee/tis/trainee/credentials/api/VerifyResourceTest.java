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

package uk.nhs.hee.tis.trainee.credentials.api;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.trainee.credentials.SignatureTestUtil;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;
import uk.nhs.hee.tis.trainee.credentials.filter.FilterConfiguration;
import uk.nhs.hee.tis.trainee.credentials.service.RevocationService;
import uk.nhs.hee.tis.trainee.credentials.service.VerificationService;

@WebMvcTest(VerifyResource.class)
@ComponentScan(basePackageClasses = FilterConfiguration.class)
class VerifyResourceTest {

  private static final String AUTH_TOKEN = "auth-token";
  private static final String CODE_PARAM = "code";
  private static final String CODE_VALUE = "not-a-real-code";
  private static final String SCOPE_PARAM = "scope";
  private static final String SCOPE_VALUE = "openid Identity";
  private static final String STATE_PARARM = "state";
  private static final String STATE_VALUE = UUID.randomUUID().toString();

  private static final String UNSIGNED_IDENTITY_DATA = """
      {
            "forenames": "Anthony",
            "surname": "Gilliam",
            "dateOfBirth": "1991-11-11",
            "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
      """.formatted(Instant.MIN, Instant.MAX);

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private RevocationService revocationService;

  @MockBean
  private VerificationService service;

  @MockBean
  private RestTemplateBuilder restTemplateBuilder;

  @Value("${application.signature.secret-key}")
  private String secretKey;

  @Test
  void shouldForbidUnsignedVerifyIdentityRequests() throws Exception {
    mockMvc.perform(
            post("/api/verify/identity")
                .content(UNSIGNED_IDENTITY_DATA)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnCreatedWhenVerificationStarted()
      throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_IDENTITY_DATA, secretKey);

    String uriString = "the-uri";
    URI uri = URI.create(uriString);
    when(service.startIdentityVerification(any(), any(IdentityDataDto.class), any())).thenReturn(
        uri);

    mockMvc.perform(
            post("/api/verify/identity")
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, uriString))
        .andExpect(content().string(uriString));
  }

  @Test
  void shouldPassAuthTokenDownstream() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_IDENTITY_DATA, secretKey);

    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    when(service.startIdentityVerification(tokenCaptor.capture(), any(IdentityDataDto.class),
        any())).thenReturn(URI.create(""));

    mockMvc.perform(
        post("/api/verify/identity")
            .header(HttpHeaders.AUTHORIZATION, "some-auth-token")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    String token = tokenCaptor.getValue();
    assertThat("Unexpected token.", token, is("some-auth-token"));
  }

  @Test
  void shouldPassStateDownstreamWhenStateGiven() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_IDENTITY_DATA, secretKey);

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    when(service.startIdentityVerification(any(), any(IdentityDataDto.class),
        stateCaptor.capture())).thenReturn(URI.create(""));

    mockMvc.perform(
        post("/api/verify/identity")
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .queryParam("state", "some-state-value")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, is("some-state-value"));
  }

  @Test
  void shouldNotPassStateDownstreamWhenNoStateGiven() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_IDENTITY_DATA, secretKey);

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    when(service.startIdentityVerification(any(), any(IdentityDataDto.class),
        stateCaptor.capture())).thenReturn(URI.create(""));

    mockMvc.perform(
        post("/api/verify/identity")
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, nullValue());
  }

  @Test
  void shouldUseIdentityDataDtoFromRequestBody() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_IDENTITY_DATA, secretKey);

    ArgumentCaptor<IdentityDataDto> dtoCaptor = ArgumentCaptor.forClass(IdentityDataDto.class);
    when(service.startIdentityVerification(any(), dtoCaptor.capture(), any())).thenReturn(
        URI.create(""));

    mockMvc.perform(
        post("/api/verify/identity")
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    IdentityDataDto dto = dtoCaptor.getValue();
    assertThat("Unexpected forenames.", dto.forenames(), is("Anthony"));
    assertThat("Unexpected surname.", dto.surname(), is("Gilliam"));
    assertThat("Unexpected date of birth.", dto.dateOfBirth(), is(LocalDate.of(1991, 11, 11)));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
          forenames    | ''
          forenames    |
          surname      | ''
          surname      |
          dateOfBirth  | ''
          dateOfBirth  |
      """)
  void shouldRejectIdentityDataWhenPropertiesInvalid(String fieldName, String fieldValue)
      throws Exception {
    String signedData = SignatureTestUtil.overwriteFieldAndSignData(UNSIGNED_IDENTITY_DATA,
        secretKey, fieldName, fieldValue);

    mockMvc.perform(
            post("/api/verify/identity")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "forenames",
      "surname",
      "dateOfBirth"
  })
  void shouldRejectIdentityDataWhenPropertiesMissing(String fieldName) throws Exception {
    String signedData = SignatureTestUtil.removeFieldAndSignData(UNSIGNED_IDENTITY_DATA, secretKey,
        fieldName);

    mockMvc.perform(
            post("/api/verify/identity")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(service);
  }

  @Test
  void shouldFailValidationWhenStateNotProvided() throws Exception {
    mockMvc.perform(
            get(("/api/verify/callback"))
                .queryParam(CODE_PARAM, CODE_VALUE))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$").doesNotExist());

    verifyNoInteractions(service);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "not-a-valid-state"})
  void shouldFailValidationWhenStateInvalid(String state) throws Exception {
    mockMvc.perform(
            get(("/api/verify/callback"))
                .queryParam(CODE_PARAM, CODE_VALUE)
                .queryParam(STATE_PARARM, state))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.state").value(is("must be a valid UUID")));

    verifyNoInteractions(service);
  }

  @Test
  void shouldRedirectWhenVerificationCompleted() throws Exception {
    when(service.completeCredentialVerification(CODE_VALUE, SCOPE_VALUE, STATE_VALUE)).thenReturn(
        URI.create("test-redirect"));

    mockMvc.perform(
            get(("/api/verify/callback"))
                .queryParam(CODE_PARAM, CODE_VALUE)
                .queryParam(SCOPE_PARAM, SCOPE_VALUE)
                .queryParam(STATE_PARARM, STATE_VALUE))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "test-redirect"))
        .andExpect(jsonPath("$").doesNotExist());
  }
}
