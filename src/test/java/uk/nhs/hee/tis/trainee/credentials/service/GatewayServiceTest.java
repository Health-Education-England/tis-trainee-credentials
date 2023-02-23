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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService.ParResponse;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService.TokenResponse;

class GatewayServiceTest {

  private static final String CLIENT_ID = "client-id";
  private static final String CLIENT_SECRET = "no-very-secret";
  private static final String HOST = "https://credential.gateway";
  private static final String AUTHORIZE_ENDPOINT = "https://credential.gateway/authorize/endpoint";
  private static final String JWKS_ENDPOINT = "https://credential.gateway/jwks/endpoint";
  private static final String PAR_ENDPOINT = "https://credential.gateway/par/endpoint";
  private static final String TOKEN_ENDPOINT = "https://credential.gateway/token/endpoint";
  private static final String REDIRECT_URI = "https://redirect.uri";
  private static final String CALLBACK_URI = "https://callback.uri";

  private static final String NONCE = UUID.randomUUID().toString();
  private static final String STATE = "some-client-state";

  private GatewayService service;
  private JwtService jwtService;
  private RestTemplate restTemplate;

  private CachingDelegate cachingDelegate;

  @BeforeEach
  void setUp() {
    restTemplate = mock(RestTemplate.class);
    jwtService = mock(JwtService.class);
    cachingDelegate = mock(CachingDelegate.class);

    IssuingProperties issuingProperties = new IssuingProperties(PAR_ENDPOINT, AUTHORIZE_ENDPOINT,
        "", null, CALLBACK_URI, REDIRECT_URI);
    VerificationProperties verificationProperties = new VerificationProperties("", "", "");
    GatewayProperties gatewayProperties = new GatewayProperties(HOST, CLIENT_ID, CLIENT_SECRET,
        JWKS_ENDPOINT, issuingProperties, verificationProperties);

    service = new GatewayService(restTemplate, jwtService, gatewayProperties, cachingDelegate);
  }

  void setUpWithoutCallbackUri() {
    restTemplate = mock(RestTemplate.class);
    jwtService = mock(JwtService.class);
    cachingDelegate = mock(CachingDelegate.class);

    IssuingProperties issuingProperties = new IssuingProperties(PAR_ENDPOINT, AUTHORIZE_ENDPOINT,
        "", null, null, REDIRECT_URI);
    VerificationProperties verificationProperties = new VerificationProperties("", "", "");
    GatewayProperties gatewayProperties = new GatewayProperties(HOST, CLIENT_ID, CLIENT_SECRET,
        JWKS_ENDPOINT, issuingProperties, verificationProperties);

    service = new GatewayService(restTemplate, jwtService, gatewayProperties, cachingDelegate);
  }

  @Test
  void shouldIncludeAcceptHeaderInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    when(jwtService.generateToken(dto)).thenReturn("id_token_hint_value");

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

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

    service.getCredentialUri(dto, NONCE, STATE);

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

    service.getCredentialUri(dto, NONCE, STATE);

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

    service.getCredentialUri(dto, NONCE, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected client secret.", requestBody.get("client_secret"),
        is(List.of(CLIENT_SECRET)));
  }

  @Test
  void shouldIncludeRedirectUriInParRequestIfNoCallbackUri() {
    setUpWithoutCallbackUri();
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected redirect URI.", requestBody.get("redirect_uri"),
        is(List.of(REDIRECT_URI)));
  }

  @Test
  void shouldIncludeCallbackUriInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected redirect URI.", requestBody.get("redirect_uri"),
        is(List.of(CALLBACK_URI)));
  }

  @Test
  void shouldIncludeProgrammeMembershipScopeInParRequest() {
    var dto = new ProgrammeMembershipCredentialDto("", "", LocalDate.MIN, LocalDate.MAX);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected scope.", requestBody.get("scope"),
        is(List.of("issue.TrainingProgramme")));
  }

  @Test
  void shouldIncludePlacementScopeInParRequest() {
    PlacementCredentialDto dto
        = new PlacementCredentialDto("", "", "", "", "", "", LocalDate.MIN, LocalDate.MAX);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected scope.", requestBody.get("scope"),
        is(List.of("issue.TrainingPlacement")));
  }

  @Test
  void shouldIncludeStateInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

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

    service.getCredentialUri(dto, NONCE, STATE);

    var request = (HttpEntity<MultiValueMap<String, String>>) argumentCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected nonce.", requestBody.get("nonce"), is(List.of(NONCE)));
  }

  @Test
  void shouldIncludeIdTokenHintInParRequest() {
    CredentialDto dto = mock(CredentialDto.class);

    when(jwtService.generateToken(dto)).thenReturn("id_token_hint_value");

    var argumentCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), argumentCaptor.capture(),
        eq(ParResponse.class))).thenReturn(ResponseEntity.ok(null));

    service.getCredentialUri(dto, NONCE, STATE);

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

    Optional<URI> optional = service.getCredentialUri(dto, NONCE, STATE);

    assertThat("Unexpected URI presence.", optional.isPresent(), is(false));
  }

  @Test
  void shouldReturnEmptyUriWhenParResponseEmpty() {
    CredentialDto dto = mock(CredentialDto.class);

    var response = ResponseEntity.status(HttpStatus.CREATED).body((ParResponse) null);
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), any(), eq(ParResponse.class))).thenReturn(
        response);

    Optional<URI> optional = service.getCredentialUri(dto, NONCE, STATE);

    assertThat("Unexpected URI presence.", optional.isPresent(), is(false));
  }

  @Test
  void shouldReturnUriWhenParResponseNotEmpty() {
    CredentialDto dto = mock(CredentialDto.class);

    String requestUri = "a-new-request-uri";
    var response = ResponseEntity.status(HttpStatus.CREATED).body(new ParResponse(requestUri));
    when(restTemplate.postForEntity(eq(PAR_ENDPOINT), any(), eq(ParResponse.class))).thenReturn(
        response);

    Optional<URI> optional = service.getCredentialUri(dto, NONCE, STATE);

    assertThat("Unexpected URI presence.", optional.isPresent(), is(true));

    URI credentialUri = optional.get();
    URI relativeUri = credentialUri.relativize(URI.create(AUTHORIZE_ENDPOINT));
    assertThat("Unexpected relative URI.", relativeUri, is(URI.create("")));
    assertThat("Unexpected URI query.", credentialUri.getQuery(), is("request_uri=" + requestUri));
  }

  @Test
  void shouldReturnEmptyClaimsWhenTokenResponseNotOk() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    when(restTemplate.postForEntity(eq(tokenEndpoint), any(), eq(TokenResponse.class))).thenReturn(
        ResponseEntity.notFound().build());

    Claims claims = service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    assertThat("Unexpected claim count.", claims.size(), is(0));
  }

  @Test
  void shouldReturnEmptyClaimsWhenTokenResponseEmpty() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    when(restTemplate.postForEntity(eq(tokenEndpoint), any(), eq(TokenResponse.class))).thenReturn(
        ResponseEntity.ok().build());

    Claims claims = service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    assertThat("Unexpected claim count.", claims.size(), is(0));
  }

  @Test
  void shouldReturnTokenClaimsWhenTokenResponseOk() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    String token = "tokenString";
    var response = ResponseEntity.ok().body(new TokenResponse(token));
    when(restTemplate.postForEntity(eq(tokenEndpoint), any(), eq(TokenResponse.class))).thenReturn(
        response);
    when(jwtService.getClaims(token, true)).thenReturn(new DefaultClaims(Map.of(
        "claim1", "value1",
        "claim2", "value2"
    )));

    Claims claims = service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    assertThat("Unexpected claim count.", claims.size(), is(2));
    assertThat("Unexpected claim value.", claims.get("claim1"), is("value1"));
    assertThat("Unexpected claim value.", claims.get("claim2"), is("value2"));
  }

  @Test
  void shouldIncludeClientIdInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected client id.", requestBody.get("client_id"), is(List.of(CLIENT_ID)));
  }

  @Test
  void shouldIncludeClientSecretInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected client secret.", requestBody.get("client_secret"),
        is(List.of(CLIENT_SECRET)));
  }

  @Test
  void shouldIncludeRedirectUriInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected redirect uri.", requestBody.get("redirect_uri"),
        is(List.of(REDIRECT_URI)));
  }

  @Test
  void shouldIncludeGrantTypeInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected grant type.", requestBody.get("grant_type"),
        is(List.of("authorization_code")));
  }

  @Test
  void shouldIncludeCodeInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "code123", "");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected code.", requestBody.get("code"), is(List.of("code123")));
  }

  @Test
  void shouldIncludeCodeVerifierInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "", "codeVerifier123");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    assertThat("Unexpected code verifier.", requestBody.get("code_verifier"),
        is(List.of("codeVerifier123")));
  }

  @Test
  void shouldIncludeStateInTokenRequest() {
    URI tokenEndpoint = URI.create(TOKEN_ENDPOINT);

    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);
    when(restTemplate.postForEntity(eq(tokenEndpoint), requestCaptor.capture(),
        eq(TokenResponse.class))).thenReturn(ResponseEntity.notFound().build());

    service.getTokenClaims(tokenEndpoint, URI.create(REDIRECT_URI), "code123", "");

    var request = (HttpEntity<MultiValueMap<String, String>>) requestCaptor.getValue();
    MultiValueMap<String, String> requestBody = request.getBody();
    List<String> state = requestBody.get("state");
    assertThat("Unexpected state count.", state.size(), is(1));
    assertDoesNotThrow(() -> UUID.fromString(state.get(0)), "Unexpected state format.");
  }
}
