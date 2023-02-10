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

package uk.nhs.hee.tis.trainee.credentials.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.VerificationProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService.ParResponse;

class GatewayServiceTest {

  private static final String CLIENT_ID = "client-id";
  private static final String CLIENT_SECRET = "no-very-secret";
  private static final String AUTHORIZE_ENDPOINT = "https://credential.gateway/authorize/endpoint";
  private static final String PAR_ENDPOINT = "https://credential.gateway/par/endpoint";
  private static final String REDIRECT_URI = "https://redirect.uri";

  private static final String STATE = "some-client-state";

  private GatewayService service;
  private JwtService jwtService;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    jwtService = mock(JwtService.class);

    IssuingProperties issuingProperties = new IssuingProperties(PAR_ENDPOINT, AUTHORIZE_ENDPOINT,
        "", null, REDIRECT_URI);
    VerificationProperties verificationProperties = new VerificationProperties("");
    GatewayProperties gatewayProperties = new GatewayProperties(CLIENT_ID, CLIENT_SECRET,
        issuingProperties, verificationProperties);

    service = new GatewayService(restTemplate, jwtService, gatewayProperties);
  }

  @Test
  void shouldIncludeAcceptHeaderInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    when(jwtService.generateToken(dto)).thenReturn("id_token_hint_value");

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    HttpHeaders headers = argumentCaptor.getValue().getHeaders();
    assertThat("Unexpected accept header.", headers.get(HttpHeaders.ACCEPT),
        is(List.of(MediaType.APPLICATION_JSON_VALUE)));
  }

  @Test
  void shouldIncludeContentTypeHeaderInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    when(jwtService.generateToken(dto)).thenReturn("id_token_hint_value");

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    HttpHeaders headers = argumentCaptor.getValue().getHeaders();
    assertThat("Unexpected content type header.", headers.get(HttpHeaders.CONTENT_TYPE),
        is(List.of(MediaType.APPLICATION_FORM_URLENCODED_VALUE)));
  }

  @Test
  void shouldIncludeClientIdInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected client ID.", requestBody.get("client_id"), is(List.of(CLIENT_ID)));
  }

  @Test
  void shouldIncludeClientSecretInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected client secret.", requestBody.get("client_secret"),
        is(List.of(CLIENT_SECRET)));
  }

  @Test
  void shouldIncludeRedirectUriInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected redirect URI.", requestBody.get("redirect_uri"),
        is(List.of(REDIRECT_URI)));
  }

  @Test
  void shouldIncludeProgrammeMembershipScopeInParRequest() {
    var dto = new ProgrammeMembershipCredentialDto("", LocalDate.MIN, LocalDate.MAX);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected scope.", requestBody.get("scope"),
        is(List.of("issue.TrainingProgramme")));
  }

  @Test
  void shouldIncludePlacementScopeInParRequest() {
    PlacementCredentialDto dto
        = new PlacementCredentialDto("", "", "", "", "", LocalDate.MIN, LocalDate.MAX);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected scope.", requestBody.get("scope"),
        is(List.of("issue.TrainingPlacement")));
  }

  @Test
  void shouldIncludeTestScopeInParRequest() {
    TestCredentialDto dto = new TestCredentialDto("", "", LocalDate.now());

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected scope.", requestBody.get("scope"), is(List.of("issue.TestCredential")));
  }

  @Test
  void shouldIncludeStateInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected state.", requestBody.get("state"), is(List.of(STATE)));
  }

  @Test
  void shouldIncludeGeneratedNonceInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected nonce.", requestBody.get("nonce"), notNullValue());
  }

  @Test
  void shouldIncludeIdTokenHintInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    when(jwtService.generateToken(dto)).thenReturn("id_token_hint_value");

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected id token hint.", requestBody.get("id_token_hint"),
        is(List.of("id_token_hint_value")));
  }

  @Test
  void shouldReturnEmptyUriWhenParResponseCodeNotCreated() {
    CredentialDto dto = mock(CredentialDto.class);

    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), any(), eq(ParResponse.class))).thenReturn(
        ResponseEntity.notFound().build());

    Optional<URI> optional = service.getCredentialUri(dto, STATE);

    assertThat("Unexpected URI presence.", optional.isPresent(), is(false));
  }

  @Test
  void shouldReturnEmptyUriWhenParResponseEmpty() {
    CredentialDto dto = mock(CredentialDto.class);

    var response = ResponseEntity.status(HttpStatus.CREATED).body((ParResponse) null);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), any(), eq(ParResponse.class))).thenReturn(
        response);

    Optional<URI> optional = service.getCredentialUri(dto, STATE);

    assertThat("Unexpected URI presence.", optional.isPresent(), is(false));
  }

  @Test
  void shouldReturnUriWhenParResponseNotEmpty() {
    CredentialDto dto = mock(CredentialDto.class);

    String requestUri = "a-new-request-uri";
    var response = ResponseEntity.status(HttpStatus.CREATED).body(new ParResponse(requestUri));
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), any(), eq(ParResponse.class))).thenReturn(
        response);

    Optional<URI> optional = service.getCredentialUri(dto, STATE);

    assertThat("Unexpected URI presence.", optional.isPresent(), is(true));

    URI credentialUri = optional.get();
    URI relativeUri = credentialUri.relativize(URI.create(AUTHORIZE_ENDPOINT));
    assertThat("Unexpected relative URI.", relativeUri, is(URI.create("")));
    assertThat("Unexpected URI query.", credentialUri.getQuery(), is("request_uri=" + requestUri));
  }
}
