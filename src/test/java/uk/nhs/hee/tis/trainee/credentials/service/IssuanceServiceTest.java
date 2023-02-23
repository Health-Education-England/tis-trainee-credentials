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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.impl.DefaultClaims;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.credentials.TestCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;

class IssuanceServiceTest {

  private static final String AUTH_TOKEN = "dummy-auth-token";

  private IssuanceService issuanceService;
  private GatewayService gatewayService;
  private JwtService jwtService;
  private CachingDelegate cachingDelegate;

  @BeforeEach
  void setUp() {
    gatewayService = mock(GatewayService.class);
    jwtService = mock(JwtService.class);
    cachingDelegate = mock(CachingDelegate.class);
    issuanceService = new IssuanceService(gatewayService, jwtService, cachingDelegate);
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
}
