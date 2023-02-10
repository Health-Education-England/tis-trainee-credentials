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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.VerificationProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;

class VerificationServiceTest {

  private static final String AUTHORIZE_ENDPOINT = "https://credential.gateway/authorize/endpoint";

  private VerificationService service;
  private CachingDelegate cachingDelegate;

  @BeforeEach
  void setUp() {
    cachingDelegate = spy(CachingDelegate.class);
    VerificationProperties properties = new VerificationProperties(AUTHORIZE_ENDPOINT);
    service = new VerificationService(cachingDelegate, properties);
  }

  @Test
  void shouldCacheIdentityDataWhenStartingVerification() {
    IdentityDataDto dto = new IdentityDataDto("Anthony", "Gilliam", LocalDate.now());

    URI uri = service.startIdentityVerification(dto, null);

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheIdentityData(uuidCaptor.capture(), eq(dto));

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get("nonce")));
  }

  @Test
  void shouldCacheClientStateWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, "some-client-state");

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheClientState(uuidCaptor.capture(), eq("some-client-state"));

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get("state")));
  }

  @Test
  void shouldCacheCodeVerifierWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    ArgumentCaptor<UUID> uuidCaptor = ArgumentCaptor.forClass(UUID.class);
    verify(cachingDelegate).cacheCodeVerifier(uuidCaptor.capture(), any());

    String uuid = uuidCaptor.getValue().toString();
    Map<String, String> queryParams = splitQueryParams(uri);
    assertThat("Unexpected cache key.", uuid, is(queryParams.get("state")));
  }

  @Test
  void shouldUserAuthorizeEndpointWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    URI relativeUri = uri.relativize(URI.create(AUTHORIZE_ENDPOINT));
    assertThat("Unexpected relative URI.", relativeUri, is(URI.create("")));
  }

  @Test
  void shouldIncludeNonceWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String nonce = queryParams.get("nonce");
    assertDoesNotThrow(() -> UUID.fromString(nonce));
  }

  @Test
  void shouldIncludeStateWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String state = queryParams.get("state");
    assertDoesNotThrow(() -> UUID.fromString(state));
  }

  @Test
  void shouldIncludeCodeChallengeMethodWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String codeChallengeMethod = queryParams.get("code_challenge_method");
    assertThat("Unexpected query parameter value.", codeChallengeMethod, is("S256"));
  }

  @Test
  void shouldIncludeCodeChallengeWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String codeChallenge = queryParams.get("code_challenge");
    assertThat("Unexpected query parameter value.", codeChallenge, notNullValue());
  }

  @Test
  void shouldIncludeScopeWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    Map<String, String> queryParams = splitQueryParams(uri);
    String scope = queryParams.get("scope");
    assertThat("Unexpected query parameter value.", scope, is("openid+Identity"));
  }

  @Test
  void shouldUsePkceChallengeWhenStartingVerification() {
    URI uri = service.startIdentityVerification(null, null);

    ArgumentCaptor<String> codeVerifierCaptor = ArgumentCaptor.forClass(String.class);
    verify(cachingDelegate).cacheCodeVerifier(any(), codeVerifierCaptor.capture());

    Map<String, String> queryParams = splitQueryParams(uri);
    String codeChallengeParam = queryParams.get("code_challenge");

    String coreVerifier = codeVerifierCaptor.getValue();
    byte[] codeChallengeBytes = DigestUtils.sha256(coreVerifier);
    String codeChallenge = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(codeChallengeBytes);
    assertThat("Unexpected query parameter value.", codeChallengeParam, is(codeChallenge));
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
