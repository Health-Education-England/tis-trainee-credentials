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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.trainee.credentials.SignatureTestUtil;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService;

@WebMvcTest(IssueResource.class)
class IssueResourceTest {

  private static final String UNSIGNED_DATA = """
      {
            "givenName": "Anthony",
            "familyName": "Gilliam",
            "birthDate": "1991-11-11",
            "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
      """.formatted(Instant.MIN, Instant.MAX);

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GatewayService service;

  @MockBean
  private RestTemplateBuilder restTemplateBuilder;

  @Value("${application.signature.secret-key}")
  private String secretKey;

  @Test
  void shouldForbidUnsignedRequests() throws Exception {
    mockMvc.perform(
            post("/api/issue/test")
                .content(UNSIGNED_DATA)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementDto
      test                 | uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto
      """)
  void shouldReturnErrorWhenCredentialUriNotAvailable(String mapping,
      Class<? extends TestCredentialDto> dtoClass)
      throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    when(service.getCredentialUri(any(dtoClass), any())).thenReturn(Optional.empty());

    mockMvc.perform(
            post("/api/issue/" + mapping)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementDto
      test                 | uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto
      """)
  void shouldReturnCreatedWhenCredentialUriAvailable(String mapping,
      Class<? extends TestCredentialDto> dtoClass)
      throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    String credentialUriString = "the-credential-uri";
    URI credentialUri = URI.create(credentialUriString);
    when(service.getCredentialUri(any(dtoClass), any())).thenReturn(Optional.of(credentialUri));

    mockMvc.perform(
            post("/api/issue/" + mapping)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, credentialUriString))
        .andExpect(content().string(credentialUriString));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementDto
      test                 | uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto
      """)
  void shouldPassStateDownstreamWhenStateGiven(String mapping,
      Class<? extends TestCredentialDto> dtoClass) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/" + mapping)
            .queryParam("state", "some-state-value")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(service).getCredentialUri(any(dtoClass), stateCaptor.capture());

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, is("some-state-value"));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementDto
      test                 | uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto
      """)
  void shouldNotPassStateDownstreamWhenNoStateGiven(String mapping,
      Class<? extends TestCredentialDto> dtoClass) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/" + mapping)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(service).getCredentialUri(any(dtoClass), stateCaptor.capture());

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, nullValue());
  }

  @Test
  void shouldUseTestCredentialDtoFromRequestBody() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/test")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<TestCredentialDto> dtoCaptor = ArgumentCaptor.forClass(TestCredentialDto.class);
    verify(service).getCredentialUri(dtoCaptor.capture(), any());

    TestCredentialDto dto = dtoCaptor.getValue();
    assertThat("Unexpected given name.", dto.givenName(), is("Anthony"));
    assertThat("Unexpected family name.", dto.familyName(), is("Gilliam"));
    assertThat("Unexpected birth date.", dto.birthDate(), is(LocalDate.of(1991, 11, 11)));
  }

  @Test
  void shouldUseProgrammeMembershipDtoFromRequestBody() throws Exception {
    String programmeMembership = """
        {
          "tisId": "123",
          "programmeName": "programme one",
          "startDate": "2022-01-01",
          "endDate": "2022-12-31",
          "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
        }
        """.formatted(Instant.MIN, Instant.MAX);
    String signedData = SignatureTestUtil.signData(programmeMembership, secretKey);

    mockMvc.perform(
        post("/api/issue/programme-membership")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<ProgrammeMembershipDto> dtoCaptor = ArgumentCaptor.forClass(
        ProgrammeMembershipDto.class);
    verify(service).getCredentialUri(dtoCaptor.capture(), any());

    ProgrammeMembershipDto dto = dtoCaptor.getValue();
    assertThat("Unexpected TIS ID.", dto.tisId(), is("123"));
    assertThat("Unexpected programme name.", dto.programmeName(), is("programme one"));
    assertThat("Unexpected start date.", dto.startDate(), is(LocalDate.of(2022, 1, 1)));
    assertThat("Unexpected end date.", dto.endDate(), is(LocalDate.of(2022, 12, 31)));
  }

  @Test
  void shouldUsePlacementDtoFromRequestBody() throws Exception {
    String placement = """
        {
          "tisId": "123",
          "specialty": "placement specialty",
          "grade": "placement grade",
          "nationalPostNumber": "NPN",
          "employingBody": "employing body",
          "site": "placement site",
          "startDate": "2022-01-01",
          "endDate": "2022-06-30",
          "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
        }
        """.formatted(Instant.MIN, Instant.MAX);
    String signedData = SignatureTestUtil.signData(placement, secretKey);

    mockMvc.perform(
        post("/api/issue/placement")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<PlacementDto> dtoCaptor = ArgumentCaptor.forClass(
        PlacementDto.class);
    verify(service).getCredentialUri(dtoCaptor.capture(), any());

    PlacementDto dto = dtoCaptor.getValue();
    assertThat("Unexpected TIS ID.", dto.tisId(), is("123"));
    assertThat("Unexpected specialty.", dto.specialty(), is("placement specialty"));
    assertThat("Unexpected specialty.", dto.grade(), is("placement grade"));
    assertThat("Unexpected specialty.", dto.nationalPostNumber(), is("NPN"));
    assertThat("Unexpected specialty.", dto.employingBody(), is("employing body"));
    assertThat("Unexpected specialty.", dto.site(), is("placement site"));
    assertThat("Unexpected start date.", dto.startDate(), is(LocalDate.of(2022, 1, 1)));
    assertThat("Unexpected end date.", dto.endDate(), is(LocalDate.of(2022, 6, 30)));
  }
}
