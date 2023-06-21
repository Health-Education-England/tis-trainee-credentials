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
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.credentials.TestCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

class IssuanceServiceTest {

  private static final String AUTH_TOKEN = "dummy-auth-token";

  private static final URI REDIRECT_URI = URI.create("https://credential.service/redirect-uri");
  private static final URI TOKEN_ENDPOINT = URI.create("https://credential.gateway/token/endpoint");

  private static final String CODE_VALUE = "some-code";
  private static final String STATE_VALUE = UUID.randomUUID().toString();

  private static final String TIS_ID = "the-tis-id";
  private static final String TRAINEE_ID = "the-trainee-id";
  private static final String CREDENTIAL_ID = "123-456-789";
  private static final Instant ISSUED_AT = Instant.MIN.truncatedTo(ChronoUnit.SECONDS);
  private static final Instant EXPIRES_AT = Instant.MAX.truncatedTo(ChronoUnit.SECONDS);

  private IssuanceService issuanceService;
  private GatewayService gatewayService;
  private JwtService jwtService;
  private RevocationService revocationService;
  private CachingDelegate cachingDelegate;
  private CredentialMetadataRepository credentialMetadataRepository;

  @BeforeEach
  void setUp() {
    gatewayService = mock(GatewayService.class);
    jwtService = mock(JwtService.class);
    revocationService = mock(RevocationService.class);
    cachingDelegate = mock(CachingDelegate.class);
    credentialMetadataRepository = mock(CredentialMetadataRepository.class);
    var credentialMetadataMapper = Mappers.getMapper(CredentialMetadataMapper.class);

    var properties = new IssuingProperties("", "", TOKEN_ENDPOINT.toString(), null,
        REDIRECT_URI.toString());

    issuanceService = new IssuanceService(gatewayService, jwtService, revocationService,
        cachingDelegate, credentialMetadataRepository, credentialMetadataMapper, properties);
  }

  /**
   * Create a stream of credentials, allowing testing of common functionality with different
   * credential types.
   *
   * @return The stream of credential objects.
   */
  private static Stream<CredentialDto> credentialMethodSource() {
    return Stream.of(
        new TestCredentialDto(TIS_ID),
        new PlacementCredentialDto(TIS_ID, "", "", "", "", "", LocalDate.MIN, LocalDate.MAX,
            "", "", "", "", "", "", "", LocalDate.now()),
        new ProgrammeMembershipCredentialDto(TIS_ID, "", LocalDate.MIN, LocalDate.MAX,
            "", "", "", "", "", "", "", LocalDate.now())
    );
  }

  @Test
  void shouldCacheCredentialDataAgainstNonceWhenStartingIssuance() {
    CredentialDto credentialData = new TestCredentialDto("123");
    when(jwtService.getClaims(any())).thenReturn(new DefaultClaims());

    issuanceService.startCredentialIssuance(null, credentialData, null);

    ArgumentCaptor<UUID> cacheKeyCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheCredentialData(cacheKeyCaptor.capture(), eq(credentialData));

    ArgumentCaptor<String> requestNonceCaptor = ArgumentCaptor.forClass(String.class);
    verify(gatewayService).getCredentialUri(any(), requestNonceCaptor.capture(), any());

    UUID cacheKey = cacheKeyCaptor.getValue();
    String requestNonce = requestNonceCaptor.getValue();
    assertThat("Unexpected nonce cache key.", cacheKey.toString(), is(requestNonce));
  }

  @Test
  void shouldCacheClientStateAgainstInternalStateWhenStartingIssuance() {
    when(jwtService.getClaims(any())).thenReturn(new DefaultClaims());

    issuanceService.startCredentialIssuance(null, null, "some-client-state");

    ArgumentCaptor<UUID> cacheKeyCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheClientState(cacheKeyCaptor.capture(), eq("some-client-state"));

    ArgumentCaptor<String> requestStateCaptor = ArgumentCaptor.forClass(String.class);
    verify(gatewayService).getCredentialUri(any(), any(), requestStateCaptor.capture());

    UUID cacheKey = cacheKeyCaptor.getValue();
    String requestState = requestStateCaptor.getValue();
    assertThat("Unexpected state cache key.", cacheKey.toString(), is(requestState));
  }

  @Test
  void shouldCacheTraineeIdentifierAgainstInternalStateWhenStartingIssuance() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims(Map.of(
        "custom:tisId", "123"
    )));

    issuanceService.startCredentialIssuance(AUTH_TOKEN, null, null);

    ArgumentCaptor<UUID> cacheKeyCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheTraineeIdentifier(cacheKeyCaptor.capture(), eq("123"));

    ArgumentCaptor<String> requestStateCaptor = ArgumentCaptor.forClass(String.class);
    verify(gatewayService).getCredentialUri(any(), any(), requestStateCaptor.capture());

    UUID cacheKey = cacheKeyCaptor.getValue();
    String requestState = requestStateCaptor.getValue();
    assertThat("Unexpected state cache key.", cacheKey.toString(), is(requestState));
  }

  @Test
  void shouldCacheIssuanceTimestampAgainstInternalStateWhenStartingIssuance() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());

    issuanceService.startCredentialIssuance(AUTH_TOKEN, null, null);

    ArgumentCaptor<UUID> cacheKeyCaptor = ArgumentCaptor.forClass(UUID.class);
    ArgumentCaptor<Instant> timestampCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(cachingDelegate).cacheIssuanceTimestamp(cacheKeyCaptor.capture(),
        timestampCaptor.capture());

    ArgumentCaptor<String> requestStateCaptor = ArgumentCaptor.forClass(String.class);
    verify(gatewayService).getCredentialUri(any(), any(), requestStateCaptor.capture());

    UUID cacheKey = cacheKeyCaptor.getValue();
    String requestState = requestStateCaptor.getValue();
    assertThat("Unexpected state cache key.", cacheKey.toString(), is(requestState));

    Instant timestamp = timestampCaptor.getValue();
    Instant now = Instant.now();
    int delta = (int) Duration.between(timestamp, now).toMinutes();
    assertThat("Unexpected issuance timestamp delta.", delta, is(0));
  }

  @Test
  void shouldReturnEmptyWhenGatewayRequestFails() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());
    CredentialDto credentialData = new TestCredentialDto("123");

    when(gatewayService.getCredentialUri(eq(credentialData), any(), any())).thenReturn(
        Optional.empty());

    Optional<URI> uri = issuanceService.startCredentialIssuance(AUTH_TOKEN, credentialData, null);

    assertThat("Unexpected URI.", uri, is(Optional.empty()));
  }

  @Test
  void shouldReturnRedirectUriWhenGatewayRequestSuccessful() {
    when(jwtService.getClaims(AUTH_TOKEN)).thenReturn(new DefaultClaims());
    CredentialDto credentialData = new TestCredentialDto("123");

    URI redirectUri = URI.create("/redirect-uri");
    when(gatewayService.getCredentialUri(eq(credentialData), any(), any())).thenReturn(
        Optional.of(redirectUri));

    Optional<URI> uri = issuanceService.startCredentialIssuance(AUTH_TOKEN, credentialData, null);

    assertThat("Unexpected URI.", uri, is(Optional.of(redirectUri)));
  }

  @Test
  void shouldNotSaveToRepositoryWhenCredentialNotIssued() {
    issuanceService.completeCredentialVerification(null, STATE_VALUE, "error", "error_description");
    verifyNoInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @MethodSource("credentialMethodSource")
  void shouldSaveToRepositoryWhenCredentialIssued(CredentialDto credentialData) {
    UUID nonce = UUID.randomUUID();
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", nonce.toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.getEpochSecond());
    claimsIssued.put("exp", EXPIRES_AT.getEpochSecond());

    when(gatewayService.getTokenClaims(eq(TOKEN_ENDPOINT), eq(REDIRECT_URI), eq(CODE_VALUE), any()))
        .thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialData(nonce)).thenReturn(Optional.of(credentialData));
    when(cachingDelegate.getTraineeIdentifier(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.of(TRAINEE_ID));
    issuanceService.completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null);

    ArgumentCaptor<CredentialMetadata> argument = ArgumentCaptor.forClass(CredentialMetadata.class);
    verify(credentialMetadataRepository).save(argument.capture());
    assertEquals(CREDENTIAL_ID, argument.getValue().getCredentialId());
    assertEquals(credentialData.getScope(), argument.getValue().getCredentialType());
    assertEquals(TIS_ID, argument.getValue().getTisId());
    assertEquals(TRAINEE_ID, argument.getValue().getTraineeId());
    assertEquals(ISSUED_AT, argument.getValue().getIssuedAt());
    assertEquals(EXPIRES_AT, argument.getValue().getExpiresAt());
  }

  @Test
  void shouldNotSaveToRepositoryWhenCredentialDataNotCached() {
    UUID nonce = UUID.randomUUID();
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", nonce.toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.getEpochSecond());
    claimsIssued.put("exp", EXPIRES_AT.getEpochSecond());

    when(gatewayService.getTokenClaims(eq(TOKEN_ENDPOINT), eq(REDIRECT_URI), eq(CODE_VALUE), any()))
        .thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialData(nonce)).thenReturn(Optional.empty());
    when(cachingDelegate.getTraineeIdentifier(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.of(TRAINEE_ID));
    issuanceService.completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null);

    verifyNoInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @MethodSource("credentialMethodSource")
  void shouldNotSaveToRepositoryWhenTraineeIdNotCached(CredentialDto credentialData) {
    UUID nonce = UUID.randomUUID();
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", nonce.toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.getEpochSecond());
    claimsIssued.put("exp", EXPIRES_AT.getEpochSecond());

    when(gatewayService.getTokenClaims(eq(TOKEN_ENDPOINT), eq(REDIRECT_URI), eq(CODE_VALUE), any()))
        .thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialData(nonce)).thenReturn(Optional.of(credentialData));
    when(cachingDelegate.getTraineeIdentifier(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.empty());
    issuanceService.completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null);

    verifyNoInteractions(credentialMetadataRepository);
  }

  // TODO: rewrite test to check errors vs code null. Other tests may need code included.
  @Test
  void shouldReturnCredentialIssuedWhenGatewayIssueSuccessful() {
    URI uri = issuanceService.completeCredentialVerification(null, STATE_VALUE, null, null);

    assertThat("Unexpected URI path.", uri.getPath(), is("/credential-issued"));
  }

  @Test
  void shouldIncludeGatewayErrorsInFinalRedirectWhenHasError() {
    URI uri = issuanceService.completeCredentialVerification(null, STATE_VALUE, "error_code",
        "error description");

    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected query parameter count.", queryParams.size(), is(2));
    assertThat("Unexpected error.", queryParams.get("error"), is("error_code"));
    assertThat("Unexpected error description.", queryParams.get("error_description"),
        is("error description"));
  }

  @Test
  void shouldNotIncludeGatewayErrorsInFinalRedirectWhenNoError() {
    URI uri = issuanceService.completeCredentialVerification(null, STATE_VALUE, null, null);

    assertThat("Unexpected uri query.", uri.getQuery(), nullValue());
  }

  @Test
  void shouldIncludeClientStateInFinalRedirectWhenGiven() {
    when(cachingDelegate.getClientState(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.of("some-client-state"));

    URI uri = issuanceService.completeCredentialVerification(null, STATE_VALUE, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected query parameter count.", queryParams.size(), is(1));
    assertThat("Unexpected state.", queryParams.get("state"), is("some-client-state"));
  }

  @Test
  void shouldNotIncludeClientStateInFinalRedirectWhenNotGiven() {
    when(cachingDelegate.getClientState(UUID.fromString(STATE_VALUE))).thenReturn(Optional.empty());

    URI uri = issuanceService.completeCredentialVerification(null, STATE_VALUE, null, null);

    assertThat("Unexpected uri query.", uri.getQuery(), nullValue());
  }

  @ParameterizedTest
  @MethodSource("credentialMethodSource")
  void shouldReturnErrorWhenNewCredentialRevokedWithUnknownIssuance(CredentialDto credentialData) {
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", UUID.randomUUID().toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.getEpochSecond());
    claimsIssued.put("exp", EXPIRES_AT.getEpochSecond());

    when(gatewayService.getTokenClaims(any(), any(), any(), any())).thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialData(any())).thenReturn(Optional.of(credentialData));
    when(cachingDelegate.getTraineeIdentifier(any())).thenReturn(Optional.of(TIS_ID));

    when(cachingDelegate.getIssuanceTimestamp(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.empty());
    when(revocationService.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialData.getCredentialType(),
        Instant.MIN)).thenReturn(
        true);

    URI uri = issuanceService.completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected query parameter count.", queryParams.size(), is(2));
    assertThat("Unexpected error.", queryParams.get("error"), is("unknown_data_freshness"));
    assertThat("Unexpected error description.", queryParams.get("error_description"),
        is("The issued credential data could not be verified and has been revoked"));
  }

  @ParameterizedTest
  @MethodSource("credentialMethodSource")
  void shouldReturnErrorWhenNewCredentialRevokedWithKnownIssuance(CredentialDto credentialData) {
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", UUID.randomUUID().toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.getEpochSecond());
    claimsIssued.put("exp", EXPIRES_AT.getEpochSecond());

    when(gatewayService.getTokenClaims(any(), any(), any(), any())).thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialData(any())).thenReturn(Optional.of(credentialData));
    when(cachingDelegate.getTraineeIdentifier(any())).thenReturn(Optional.of(TIS_ID));

    Instant now = Instant.now();
    when(cachingDelegate.getIssuanceTimestamp(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.of(now));
    when(revocationService.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialData.getCredentialType(),
        now)).thenReturn(true);

    URI uri = issuanceService.completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected query parameter count.", queryParams.size(), is(2));
    assertThat("Unexpected error.", queryParams.get("error"), is("stale_data"));
    assertThat("Unexpected error description.", queryParams.get("error_description"),
        is("The issued credential data was stale and has been revoked"));
  }

  @ParameterizedTest
  @MethodSource("credentialMethodSource")
  void shouldNotReturnErrorWhenNewCredentialNotRevoked(CredentialDto credentialData) {
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", UUID.randomUUID().toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.getEpochSecond());
    claimsIssued.put("exp", EXPIRES_AT.getEpochSecond());

    when(gatewayService.getTokenClaims(any(), any(), any(), any())).thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialData(any())).thenReturn(Optional.of(credentialData));
    when(cachingDelegate.getTraineeIdentifier(any())).thenReturn(Optional.of(TIS_ID));

    Instant now = Instant.now();
    when(cachingDelegate.getIssuanceTimestamp(UUID.fromString(STATE_VALUE))).thenReturn(
        Optional.of(now));
    when(revocationService.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialData.getCredentialType(),
        now)).thenReturn(false);

    URI uri = issuanceService.completeCredentialVerification(CODE_VALUE, STATE_VALUE, null, null);

    assertThat("Unexpected uri query.", uri.getQuery(), nullValue());
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
