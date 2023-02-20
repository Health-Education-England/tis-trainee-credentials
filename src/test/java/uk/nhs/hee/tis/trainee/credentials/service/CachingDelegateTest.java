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

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueRequestDto;

class CachingDelegateTest {

  private CachingDelegate delegate;

  @BeforeEach
  void setUp() {
    delegate = new CachingDelegate();
  }

  @Test
  void shouldReturnCachedClientState() {
    String clientState = delegate.cacheClientState(UUID.randomUUID(), "clientState1");
    assertThat("Unexpected client state.", clientState, is("clientState1"));
  }

  @Test
  void shouldGetEmptyClientState() {
    Optional<String> clientState = delegate.getClientState(UUID.randomUUID());
    assertThat("Unexpected client state.", clientState, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedCodeVerifier() {
    String codeVerifier = delegate.cacheCodeVerifier(UUID.randomUUID(), "codeVerifier1");
    assertThat("Unexpected code verifier.", codeVerifier, is("codeVerifier1"));
  }

  @Test
  void shouldGetEmptyCodeVerifier() {
    Optional<String> codeVerifier = delegate.getCodeVerifier(UUID.randomUUID());
    assertThat("Unexpected code verifier.", codeVerifier, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedIdentityData() {
    IdentityDataDto identityData = new IdentityDataDto("Anthony", "Gilliam", LocalDate.now());
    IdentityDataDto returnValue = delegate.cacheIdentityData(UUID.randomUUID(), identityData);
    assertThat("Unexpected identity data.", returnValue, is(identityData));
  }

  @Test
  void shouldReturnCachedPublicKey() {
    PublicKey publicKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    PublicKey cachedPublicKey = delegate.cachePublicKey("certThumb", publicKey);
    assertThat("Unexpected public key.", cachedPublicKey, is(publicKey));
  }

  @Test
  void shouldGetEmptyPublicKey() {
    Optional<PublicKey> publicKey = delegate.getPublicKey("certThumb");
    assertThat("Unexpected public key.", publicKey, is(Optional.empty()));
  }

  @Test
  void shouldGetEmptyIdentityData() {
    Optional<IdentityDataDto> identityData = delegate.getIdentityData(UUID.randomUUID());
    assertThat("Unexpected identity data.", identityData, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedUnverifiedSession() {
    String unverifiedSession = delegate.cacheUnverifiedSessionIdentifier(UUID.randomUUID(),
        "unverifiedSession1");
    assertThat("Unexpected verified session.", unverifiedSession, is("unverifiedSession1"));
  }

  @Test
  void shouldGetEmptyunVerifiedSession() {
    Optional<String> unverifiedSession = delegate.getUnverifiedSessionIdentifier(UUID.randomUUID());
    assertThat("Unexpected verified session.", unverifiedSession, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedVerifiedSession() {
    String verifiedSession = delegate.cacheVerifiedSessionIdentifier("verifiedSession1");
    assertThat("Unexpected verified session.", verifiedSession, is("verifiedSession1"));
  }

  @Test
  void shouldGetEmptyVerifiedSession() {
    Optional<String> verifiedSession = delegate.getVerifiedSessionIdentifier("verifiedSession1");
    assertThat("Unexpected verified session.", verifiedSession, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedCredentialMetadata() {
    IssueRequestDto toCache = new IssueRequestDto("credential-type", "tis-id");
    IssueRequestDto credentialMetadata = delegate.cacheCredentialData(UUID.randomUUID(), toCache);
    assertThat("Unexpected credential metadata.", credentialMetadata, is(toCache));
  }

  @Test
  void shouldGetCachedCredentialMetadata() {
    Optional<IssueRequestDto> credentialMetadata = delegate.getCredentialMetadata(
        UUID.randomUUID());
    assertThat("Unexpected credential metadata.", credentialMetadata, is(Optional.empty()));
  }
}
