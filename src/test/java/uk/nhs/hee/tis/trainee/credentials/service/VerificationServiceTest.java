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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.impl.DefaultClaims;
import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.VerificationProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;

class VerificationServiceTest {

  private static final URI AUTHORIZE_ENDPOINT = URI.create(
      "https://credential.gateway/authorize/endpoint");
  private static final URI TOKEN_ENDPOINT = URI.create("https://credential.gateway/token/endpoint");
  private static final URI REDIRECT_URI = URI.create("https://credential.service/redirect-uri");

  private static final String AUTH_TOKEN = "dummy-auth-token";
  private static final String CODE = "code";
  private static final String IDENTITY_SCOPE = "openid Identity";

  private static final String QUERY_PARAM_REASON = "reason";
  private static final String QUERY_PARAM_SCOPE = "scope";
  private static final String QUERY_PARAM_STATE = "state";
  private static final String CLAIM_NONCE = "nonce";
  private static final String CLAIM_FIRST_NAME = "Identity.ID-LegalFirstName";
  private static final String CLAIM_FAMILY_NAME = "Identity.ID-LegalSurname";
  private static final String CLAIM_BIRTH_DATE = "Identity.ID-BirthDate";

  private static final String IDENTITY_FORENAMES = "Anthony";
  private static final String IDENTITY_SURNAME = "Gilliam";
  private static final LocalDate IDENTITY_DOB = LocalDate.parse("1991-11-11");

  private VerificationService verificationService;
  private GatewayService gatewayService;
  private JwtService jwtService;
  private CachingDelegate cachingDelegate;

  @BeforeEach
  void setUp() {
    gatewayService = mock(GatewayService.class);
    jwtService = mock(JwtService.class);
    cachingDelegate = spy(CachingDelegate.class);
    var properties = new VerificationProperties(AUTHORIZE_ENDPOINT.toString(),
        TOKEN_ENDPOINT.toString(), REDIRECT_URI.toString());
    verificationService = new VerificationService(gatewayService, jwtService, cachingDelegate,
        properties);
  }

  @Test
  void shouldCacheIdentityDataWhenStartingVerification() {
    IdentityDataDto dto = new IdentityDataDto("Anthony", "Gilliam", LocalDate.now());

    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, dto, null);

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheIdentityData(uuidCaptor.capture(), eq(dto));

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get(CLAIM_NONCE)));
  }

  @Test
  void shouldCacheClientStateWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, "some-client-state");

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheClientState(uuidCaptor.capture(), eq("some-client-state"));

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get(QUERY_PARAM_STATE)));
  }

  @Test
  void shouldCacheCodeVerifierWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheCodeVerifier(uuidCaptor.capture(), any());

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get(QUERY_PARAM_STATE)));
  }

  @Test
  void shouldCacheUnverifiedSessionIdWhenStartingVerification() {
    DefaultClaims claims = new DefaultClaims(Map.of(
        "origin_jti", "session-id-1"
    ));
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(claims);

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, "some-client-state");

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheUnverifiedSessionIdentifier(uuidCaptor.capture(),
        eq("session-id-1"));

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get(CLAIM_NONCE)));
  }

  @Test
  void shouldUseAuthorizeEndpointWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    URI relativeUri = uri.relativize(AUTHORIZE_ENDPOINT);
    assertThat("Unexpected relative URI.", relativeUri, is(URI.create("")));
  }

  @Test
  void shouldIncludeNonceWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());
    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String nonce = queryParams.get(CLAIM_NONCE);
    assertDoesNotThrow(() -> UUID.fromString(nonce));
  }

  @Test
  void shouldIncludeStateWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String state = queryParams.get(QUERY_PARAM_STATE);
    assertDoesNotThrow(() -> UUID.fromString(state));
  }

  @Test
  void shouldIncludeCodeChallengeMethodWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String codeChallengeMethod = queryParams.get("code_challenge_method");
    assertThat("Unexpected query parameter value.", codeChallengeMethod, is("S256"));
  }

  @Test
  void shouldIncludeCodeChallengeWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String codeChallenge = queryParams.get("code_challenge");
    assertThat("Unexpected query parameter value.", codeChallenge, notNullValue());
  }

  @Test
  void shouldIncludeScopeWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String scope = queryParams.get(QUERY_PARAM_SCOPE);
    assertThat("Unexpected query parameter value.", scope, is("openid+Identity"));
  }

  @Test
  void shouldUsePkceChallengeWhenStartingVerification() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    URI uri = verificationService.startIdentityVerification(AUTH_TOKEN, null, null);

    ArgumentCaptor<String> codeVerifierCaptor = ArgumentCaptor.forClass(String.class);
    verify(cachingDelegate).cacheCodeVerifier(any(), codeVerifierCaptor.capture());

    Map<String, String> queryParams = splitQueryParams(uri);
    String codeChallengeParam = queryParams.get("code_challenge");

    String codeVerifier = codeVerifierCaptor.getValue();
    byte[] codeChallengeBytes = DigestUtils.sha256(codeVerifier);
    String codeChallenge = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(codeChallengeBytes);
    assertThat("Unexpected query parameter value.", codeChallengeParam, is(codeChallenge));
  }

  @Test
  void shouldReturnInvalidCredentialWhenNoCachedCodeVerifier() {
    UUID state = UUID.randomUUID();
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.empty());

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("no_code_verifier"));
  }

  @Test
  void shouldReturnInvalidCredentialWhenUnsupportedCredentialScope() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";

    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(new DefaultClaims());

    URI uri = verificationService.completeCredentialVerification(CODE, "invalid-scope",
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("unsupported_scope"));
  }

  @Test
  void shouldReturnInvalidCredentialWhenIdentityVerificationFailsOnMissingIdentity() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(CLAIM_NONCE, nonce.toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.empty());

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("identity_verification_failed"));
  }

  @Test
  void shouldReturnInvalidCredentialWhenIdentityVerificationFailsOnForenames() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(
        CLAIM_NONCE, nonce.toString(),
        CLAIM_FIRST_NAME, "mismatch",
        CLAIM_FAMILY_NAME, IDENTITY_SURNAME,
        CLAIM_BIRTH_DATE, IDENTITY_DOB.toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getUnverifiedSessionIdentifier(nonce)).thenReturn(
        Optional.of("session123"));

    IdentityDataDto identityData = new IdentityDataDto(IDENTITY_FORENAMES, IDENTITY_SURNAME,
        IDENTITY_DOB);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.of(identityData));

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("identity_verification_failed"));
  }

  @Test
  void shouldReturnInvalidCredentialWhenIdentityVerificationFailsOnSurname() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(
        CLAIM_NONCE, nonce.toString(),
        CLAIM_FIRST_NAME, IDENTITY_FORENAMES,
        CLAIM_FAMILY_NAME, "mismatch",
        CLAIM_BIRTH_DATE, IDENTITY_DOB.toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getUnverifiedSessionIdentifier(nonce)).thenReturn(
        Optional.of("session123"));

    IdentityDataDto identityData = new IdentityDataDto(IDENTITY_FORENAMES, IDENTITY_SURNAME,
        IDENTITY_DOB);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.of(identityData));

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("identity_verification_failed"));
  }

  @Test
  void shouldReturnInvalidCredentialWhenIdentityVerificationFailsOnDob() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(
        CLAIM_NONCE, nonce.toString(),
        CLAIM_FIRST_NAME, IDENTITY_FORENAMES,
        CLAIM_FAMILY_NAME, IDENTITY_SURNAME,
        CLAIM_BIRTH_DATE, LocalDate.now().toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getUnverifiedSessionIdentifier(nonce)).thenReturn(
        Optional.of("session123"));

    IdentityDataDto identityData = new IdentityDataDto(IDENTITY_FORENAMES, IDENTITY_SURNAME,
        IDENTITY_DOB);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.of(identityData));

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("identity_verification_failed"));
  }

  @Test
  void shouldReturnInvalidCredentialWhenIdentityVerificationFailsOnMissingSession() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(
        CLAIM_NONCE, nonce.toString(),
        CLAIM_FIRST_NAME, IDENTITY_FORENAMES,
        CLAIM_FAMILY_NAME, IDENTITY_SURNAME,
        CLAIM_BIRTH_DATE, IDENTITY_DOB.toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getUnverifiedSessionIdentifier(nonce)).thenReturn(Optional.empty());

    IdentityDataDto identityData = new IdentityDataDto(IDENTITY_FORENAMES, IDENTITY_SURNAME,
        IDENTITY_DOB);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.of(identityData));

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/invalid-credential"));

    Map<String, String> queryParams = splitQueryParams(uri);
    String reason = queryParams.get(QUERY_PARAM_REASON);
    assertThat("Unexpected invalid reason.", reason, is("identity_verification_failed"));
  }

  @Test
  void shouldReturnCredentialVerifiedWhenIdentityVerificationPasses() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(
        CLAIM_NONCE, nonce.toString(),
        CLAIM_FIRST_NAME, IDENTITY_FORENAMES,
        CLAIM_FAMILY_NAME, IDENTITY_SURNAME,
        CLAIM_BIRTH_DATE, IDENTITY_DOB.toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getUnverifiedSessionIdentifier(nonce)).thenReturn(
        Optional.of("session123"));

    IdentityDataDto identityData = new IdentityDataDto(IDENTITY_FORENAMES, IDENTITY_SURNAME,
        IDENTITY_DOB);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.of(identityData));

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    assertThat("Unexpected URI path.", uri.getPath(), is("/credential-verified"));
  }

  @Test
  void shouldCacheVerifiedSessionWhenIdentityVerificationPasses() {
    UUID state = UUID.randomUUID();
    String codeVerifier = "code-verifier";
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.of(codeVerifier));

    UUID nonce = UUID.randomUUID();
    DefaultClaims claims = new DefaultClaims(Map.of(
        CLAIM_NONCE, nonce.toString(),
        CLAIM_FIRST_NAME, IDENTITY_FORENAMES,
        CLAIM_FAMILY_NAME, IDENTITY_SURNAME,
        CLAIM_BIRTH_DATE, IDENTITY_DOB.toString()));
    when(gatewayService.getTokenClaims(TOKEN_ENDPOINT, REDIRECT_URI, CODE, codeVerifier))
        .thenReturn(claims);
    when(cachingDelegate.getUnverifiedSessionIdentifier(nonce)).thenReturn(
        Optional.of("session123"));

    IdentityDataDto identityData = new IdentityDataDto(IDENTITY_FORENAMES, IDENTITY_SURNAME,
        IDENTITY_DOB);
    when(cachingDelegate.getIdentityData(nonce)).thenReturn(Optional.of(identityData));

    verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE, state.toString());

    verify(cachingDelegate).cacheVerifiedSessionIdentifier("session123");
  }

  @Test
  void shouldReturnStateWhenClientStateCached() {
    UUID state = UUID.randomUUID();
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.empty());
    when(cachingDelegate.getClientState(state)).thenReturn(Optional.of("client-state"));

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    Map<String, String> queryParams = splitQueryParams(uri);
    String clientState = queryParams.get(QUERY_PARAM_STATE);
    assertThat("Unexpected client state.", clientState, is("client-state"));
  }

  @Test
  void shouldNotReturnStateWhenClientStateNotCached() {
    UUID state = UUID.randomUUID();
    when(cachingDelegate.getCodeVerifier(state)).thenReturn(Optional.empty());
    when(cachingDelegate.getClientState(state)).thenReturn(Optional.empty());

    URI uri = verificationService.completeCredentialVerification(CODE, IDENTITY_SCOPE,
        state.toString());

    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected client state presence.", queryParams.keySet(),
        not(hasItem(QUERY_PARAM_STATE)));
  }

  /**
   * Split the query params of a URI in to a Map.
   *
   * @param uri The URI to split the query params from.
   * @return A map of parameter names to value.
   */
  private Map<String, String> splitQueryParams(URI uri) {
    return Arrays.stream(
            uri.getQuery().split("&"))
        .map(param -> param.split("="))
        .collect(Collectors.toMap(keyValue -> keyValue[0], keyValue -> keyValue[1]));
  }
}
