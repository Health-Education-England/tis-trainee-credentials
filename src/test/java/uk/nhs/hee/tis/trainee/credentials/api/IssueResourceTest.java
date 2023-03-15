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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.trainee.credentials.SignatureTestUtil;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.filter.FilterConfiguration;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialDataMapper;
import uk.nhs.hee.tis.trainee.credentials.service.IssuanceService;
import uk.nhs.hee.tis.trainee.credentials.service.RevocationService;
import uk.nhs.hee.tis.trainee.credentials.service.VerificationService;

@WebMvcTest(IssueResource.class)
@ComponentScan(basePackageClasses = {CredentialDataMapper.class, FilterConfiguration.class})
class IssueResourceTest {

  private static final String AUTH_TOKEN = "auth-token";
  private static final String CODE_PARAM = "code";
  private static final String CODE_VALUE = "some-code";
  private static final String STATE_PARAM = "state";
  private static final String STATE_VALUE = "some-state";

  private static final String UNSIGNED_DATA = """
      {
            "tisId": "123",
            "programmeName": "programme one",
            "specialty": "placement specialty",
            "grade": "placement grade",
            "nationalPostNumber": "NPN",
            "employingBody": "employing body",
            "site": "placement site",
            "startDate": "2022-01-01",
            "endDate": "2022-12-31",
            "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
      """.formatted(Instant.MIN, Instant.MAX);

  private static final String UNSIGNED_PROGRAMME_MEMBERSHIP = """
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
  private static final String UNSIGNED_PLACEMENT = """
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

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private IssuanceService issuanceService;

  @MockBean
  private RevocationService revocationService;

  @MockBean
  private VerificationService verificationService;

  @MockBean
  private RestTemplateBuilder restTemplateBuilder;

  @SpyBean
  private CredentialDataMapper mapper;

  @Value("${application.signature.secret-key}")
  private String secretKey;

  @BeforeEach
  void setUp() {
    when(verificationService.hasVerifiedSession(any())).thenReturn(true);
  }

  @ParameterizedTest
  @ValueSource(strings = {"placement", "programme-membership", "non-existent"})
  void shouldForbidUnsignedRequests(String path) throws Exception {
    mockMvc.perform(
            post("/api/issue/" + path)
                .content(UNSIGNED_DATA)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(strings = {"placement", "programme-membership", "non-existent"})
  void shouldNotAuthoriseRequestsWithoutVerifiedSession(String path) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    when(verificationService.hasVerifiedSession(any())).thenReturn(false);

    mockMvc.perform(
            post("/api/issue/" + path)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE,
            "IdentityVerification realm=\"/api/verify/identity\""));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto
      """)
  void shouldReturnErrorWhenCredentialUriNotAvailable(String mapping,
      Class<? extends CredentialDto> dtoClass)
      throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    when(issuanceService.startCredentialIssuance(any(), any(dtoClass), any())).thenReturn(
        Optional.empty());

    mockMvc.perform(
            post("/api/issue/" + mapping)
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto
      """)
  void shouldReturnCreatedWhenCredentialUriAvailable(String mapping,
      Class<? extends CredentialDto> dtoClass)
      throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    String credentialUriString = "the-credential-uri";
    URI credentialUri = URI.create(credentialUriString);
    when(issuanceService.startCredentialIssuance(any(), any(dtoClass), any())).thenReturn(
        Optional.of(credentialUri));

    mockMvc.perform(
            post("/api/issue/" + mapping)
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, credentialUriString))
        .andExpect(content().string(credentialUriString));
  }

  @ParameterizedTest
  @ValueSource(strings = {"placement", "programme-membership"})
  void shouldReturnBadRequestWhenNoAuthorizationHeader(String mapping) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
            post("/api/issue/" + mapping)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto
      """)
  void shouldUseTokenFromAuthorizationHeader(String mapping,
      Class<? extends CredentialDto> dtoClass) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/" + mapping)
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
    verify(issuanceService).startCredentialIssuance(tokenCaptor.capture(), any(dtoClass), any());

    String token = tokenCaptor.getValue();
    assertThat("Unexpected token.", token, is(AUTH_TOKEN));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto
      """)
  void shouldPassStateDownstreamWhenStateGiven(String mapping,
      Class<? extends CredentialDto> dtoClass) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/" + mapping)
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .queryParam("state", "some-state-value")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(issuanceService).startCredentialIssuance(any(), any(dtoClass), stateCaptor.capture());

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, is("some-state-value"));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
      programme-membership | uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto
      placement            | uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto
      """)
  void shouldNotPassStateDownstreamWhenNoStateGiven(String mapping,
      Class<? extends CredentialDto> dtoClass) throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/" + mapping)
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(issuanceService).startCredentialIssuance(any(), any(dtoClass), stateCaptor.capture());

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, nullValue());
  }

  @Test
  void shouldUseProgrammeMembershipDataFromRequestBody() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_PROGRAMME_MEMBERSHIP, secretKey);

    mockMvc.perform(
        post("/api/issue/programme-membership")
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<ProgrammeMembershipCredentialDto> dtoCaptor = ArgumentCaptor.forClass(
        ProgrammeMembershipCredentialDto.class);
    verify(issuanceService).startCredentialIssuance(any(), dtoCaptor.capture(), any());

    ProgrammeMembershipCredentialDto dto = dtoCaptor.getValue();
    assertThat("Unexpected programme name.", dto.programmeName(), is("programme one"));
    assertThat("Unexpected start date.", dto.startDate(), is(LocalDate.of(2022, 1, 1)));
    assertThat("Unexpected end date.", dto.endDate(), is(LocalDate.of(2022, 12, 31)));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
          tisId         | ''
          tisId         |
          programmeName | ''
          programmeName |
          startDate     | ''
          startDate     |
          endDate       | ''
          endDate       |
      """)
  void shouldRejectProgrammeMembershipWhenPropertiesInvalid(String fieldName, String fieldValue)
      throws Exception {
    String signedData = SignatureTestUtil.overwriteFieldAndSignData(UNSIGNED_PROGRAMME_MEMBERSHIP,
        secretKey, fieldName,
        fieldValue);

    mockMvc.perform(
            post("/api/issue/programme-membership")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(issuanceService);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "tisId",
      "programmeName",
      "startDate",
      "endDate"
  })
  void shouldRejectProgrammeMembershipWhenPropertiesMissing(String fieldName) throws Exception {
    String signedData = SignatureTestUtil.removeFieldAndSignData(UNSIGNED_PROGRAMME_MEMBERSHIP,
        secretKey, fieldName);

    mockMvc.perform(
            post("/api/issue/programme-membership")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(issuanceService);
  }

  @Test
  void shouldUsePlacementDataFromRequestBody() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_PLACEMENT, secretKey);

    mockMvc.perform(
        post("/api/issue/placement")
            .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<PlacementCredentialDto> dtoCaptor = ArgumentCaptor.forClass(
        PlacementCredentialDto.class);
    verify(issuanceService).startCredentialIssuance(any(), dtoCaptor.capture(), any());

    PlacementCredentialDto dto = dtoCaptor.getValue();
    assertThat("Unexpected specialty.", dto.specialty(), is("placement specialty"));
    assertThat("Unexpected grade.", dto.grade(), is("placement grade"));
    assertThat("Unexpected NPN.", dto.nationalPostNumber(), is("NPN"));
    assertThat("Unexpected employing body.", dto.employingBody(), is("employing body"));
    assertThat("Unexpected site.", dto.site(), is("placement site"));
    assertThat("Unexpected start date.", dto.startDate(), is(LocalDate.of(2022, 1, 1)));
    assertThat("Unexpected end date.", dto.endDate(), is(LocalDate.of(2022, 6, 30)));
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
          tisId              | ''
          tisId              |
          specialty          | ''
          specialty          |
          grade              | ''
          grade              |
          employingBody      | ''
          employingBody      |
          site               | ''
          site               |
          startDate          | ''
          startDate          |
          endDate            | ''
          endDate            |
      """)
  void shouldRejectPlacementWhenPropertiesInvalid(String fieldName, String fieldValue)
      throws Exception {
    String signedData = SignatureTestUtil.overwriteFieldAndSignData(UNSIGNED_PLACEMENT, secretKey,
        fieldName,
        fieldValue);

    mockMvc.perform(
            post("/api/issue/placement")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(issuanceService);
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "tisId",
      "specialty",
      "grade",
      "employingBody",
      "site",
      "startDate",
      "endDate"
  })
  void shouldRejectPlacementWhenPropertiesMissing(String fieldName) throws Exception {
    String signedData = SignatureTestUtil.removeFieldAndSignData(UNSIGNED_PLACEMENT, secretKey,
        fieldName);

    mockMvc.perform(
            post("/api/issue/placement")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(issuanceService);
  }

  /**
   * Allow national post number to be missing from placement data.
   *
   * <p>This is a temporary relaxation of the normal rules until the field is populated in
   * tis-trainee-details.
   *
   * <p>@throws Exception If the dataToSign was not valid JSON.
   */
  @Test
  @Disabled("Disabled due to issues with the data, see above comment.")
  void shouldAcceptPlacementWhenNpnMissing() throws Exception {
    String signedData = SignatureTestUtil.removeFieldAndSignData(UNSIGNED_PLACEMENT, secretKey,
        "nationalPostNumber");

    mockMvc.perform(
            post("/api/issue/placement")
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
  }

  @Test
  void shouldRedirectWhenIssuingCallbackCompleted() throws Exception {
    when(issuanceService
        .completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null))
        .thenReturn(URI.create("test-redirect"));

    mockMvc.perform(
            get(("/api/issue/callback"))
                .header(HttpHeaders.AUTHORIZATION, AUTH_TOKEN)
                .queryParam(CODE_PARAM, CODE_VALUE)
                .queryParam(STATE_PARAM, STATE_VALUE))
        .andExpect(status().isFound())
        .andExpect(header().string(HttpHeaders.LOCATION, "test-redirect"))
        .andExpect(jsonPath("$").doesNotExist());
  }
}
