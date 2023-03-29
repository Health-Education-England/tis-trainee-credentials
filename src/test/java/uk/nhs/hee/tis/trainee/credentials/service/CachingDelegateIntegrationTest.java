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

import com.amazonaws.services.sqs.AmazonSQSAsync;
import io.awspring.cloud.autoconfigure.messaging.SqsAutoConfiguration;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.PublicKey;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.nhs.hee.tis.trainee.credentials.TestCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;

@SpringBootTest(properties = "embedded.containers.enabled=true")
@ActiveProfiles("redis")
@Testcontainers(disabledWithoutDocker = true)
@EnableAutoConfiguration(exclude = SqsAutoConfiguration.class)
class CachingDelegateIntegrationTest {

  @MockBean
  private AmazonSQSAsync amazonSqsAsync;

  @Autowired
  CachingDelegate delegate;

  @Test
  void shouldReturnEmptyClientStateWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<String> cachedOptional = delegate.getClientState(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedClientStateAfterCaching() {
    UUID key = UUID.randomUUID();

    String clientState = "clientState1";
    String cachedString = delegate.cacheClientState(key, clientState);
    assertThat("Unexpected cached value.", cachedString, is(clientState));
  }

  @Test
  void shouldGetCachedClientStateWhenCached() {
    UUID key = UUID.randomUUID();

    String clientState = "clientState1";
    delegate.cacheClientState(key, clientState);

    Optional<String> cachedOptional = delegate.getClientState(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(clientState)));
  }

  @Test
  void shouldRemoveClientStateWhenRetrieved() {
    UUID key = UUID.randomUUID();

    String clientState = "clientState1";
    delegate.cacheClientState(key, clientState);

    // Ignore this result, the cached value should be evicted.
    delegate.getClientState(key);

    Optional<String> cachedOptional = delegate.getClientState(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyCodeVerifierWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<String> cachedOptional = delegate.getCodeVerifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedCodeVerifierAfterCaching() {
    UUID key = UUID.randomUUID();

    String codeVerifier = "codeVerifier1";
    String cachedString = delegate.cacheCodeVerifier(key, codeVerifier);
    assertThat("Unexpected cached value.", cachedString, is(codeVerifier));
  }

  @Test
  void shouldGetCachedCodeVerifierWhenCached() {
    UUID key = UUID.randomUUID();

    String codeVerifier = "codeVerifier1";
    delegate.cacheCodeVerifier(key, codeVerifier);

    Optional<String> cachedOptional = delegate.getCodeVerifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(codeVerifier)));
  }

  @Test
  void shouldRemoveCodeVerifierWhenRetrieved() {
    UUID key = UUID.randomUUID();

    String codeVerifier = "codeVerifier1";
    delegate.cacheCodeVerifier(key, codeVerifier);

    // Ignore this result, the cached value should be evicted.
    delegate.getCodeVerifier(key);

    Optional<String> cachedOptional = delegate.getCodeVerifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyCredentialDataWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<CredentialDto> cachedOptional = delegate.getCredentialData(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedCredentialDataAfterCaching() {
    UUID key = UUID.randomUUID();

    CredentialDto credentialData = new TestCredentialDto("123");
    CredentialDto cachedData = delegate.cacheCredentialData(key, credentialData);
    assertThat("Unexpected cached value.", cachedData, is(credentialData));
  }

  @Test
  void shouldGetCachedCredentialDataWhenCached() {
    UUID key = UUID.randomUUID();

    CredentialDto credentialData = new TestCredentialDto("123");
    delegate.cacheCredentialData(key, credentialData);

    Optional<CredentialDto> cachedOptional = delegate.getCredentialData(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(credentialData)));
  }

  @Test
  void shouldRemoveCredentialDataWhenRetrieved() {
    UUID key = UUID.randomUUID();

    CredentialDto credentialData = new TestCredentialDto("123");
    delegate.cacheCredentialData(key, credentialData);

    // Ignore this result, the cached value should be evicted.
    delegate.getCredentialData(key);

    Optional<CredentialDto> cachedOptional = delegate.getCredentialData(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyIssuanceTimestampWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<Instant> cachedOptional = delegate.getIssuanceTimestamp(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedIssuanceTimestampAfterCaching() {
    UUID key = UUID.randomUUID();

    Instant timestamp = Instant.now();
    Instant cachedData = delegate.cacheIssuanceTimestamp(key, timestamp);
    assertThat("Unexpected cached value.", cachedData, is(timestamp));
  }

  @Test
  void shouldGetCachedIssuanceTimestampWhenCached() {
    UUID key = UUID.randomUUID();

    Instant timestamp = Instant.now();
    delegate.cacheIssuanceTimestamp(key, timestamp);

    Optional<Instant> cachedOptional = delegate.getIssuanceTimestamp(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(timestamp)));
  }

  @Test
  void shouldRemoveIssuanceTimestampWhenRetrieved() {
    UUID key = UUID.randomUUID();

    Instant timestamp = Instant.now();
    delegate.cacheIssuanceTimestamp(key, timestamp);

    // Ignore this result, the cached value should be evicted.
    delegate.getIssuanceTimestamp(key);

    Optional<Instant> cachedOptional = delegate.getIssuanceTimestamp(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyIdentityDataWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<IdentityDataDto> cachedOptional = delegate.getIdentityData(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedIdentityDataAfterCaching() {
    UUID key = UUID.randomUUID();

    IdentityDataDto identityData = new IdentityDataDto("Anthony", "Gilliam", LocalDate.now());
    IdentityDataDto cachedDto = delegate.cacheIdentityData(key, identityData);
    assertThat("Unexpected cached value.", cachedDto, is(identityData));
  }

  @Test
  void shouldGetCachedIdentityDataWhenCached() {
    UUID key = UUID.randomUUID();

    IdentityDataDto identityData = new IdentityDataDto("Anthony", "Gilliam", LocalDate.now());
    delegate.cacheIdentityData(key, identityData);

    Optional<IdentityDataDto> cachedOptional = delegate.getIdentityData(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(identityData)));
  }

  @Test
  void shouldRemoveIdentityDataWhenRetrieved() {
    UUID key = UUID.randomUUID();

    IdentityDataDto identityData = new IdentityDataDto("Anthony", "Gilliam", LocalDate.now());
    delegate.cacheIdentityData(key, identityData);

    // Ignore this result, the cached value should be evicted.
    delegate.getIdentityData(key);

    Optional<IdentityDataDto> cachedOptional = delegate.getIdentityData(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyPublicKeyWhenNotCached() {
    String certificateThumbprint = UUID.randomUUID().toString();
    Optional<PublicKey> cachedOptional = delegate.getPublicKey(certificateThumbprint);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedPublicKeyAfterCaching() {
    String certificateThumbprint = UUID.randomUUID().toString();
    PublicKey publicKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    PublicKey cachedKey = delegate.cachePublicKey(certificateThumbprint, publicKey);
    assertThat("Unexpected cached value.", cachedKey, is(publicKey));
  }

  @Test
  void shouldGetCachedPublicKeyWhenCached() {
    String certificateThumbprint = UUID.randomUUID().toString();
    PublicKey publicKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    delegate.cachePublicKey(certificateThumbprint, publicKey);

    Optional<PublicKey> cachedOptional = delegate.getPublicKey(certificateThumbprint);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(publicKey)));
  }

  @Test
  void shouldNotRemovePublicKeyWhenRetrieved() {
    String certificateThumbprint = UUID.randomUUID().toString();
    PublicKey publicKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    delegate.cachePublicKey(certificateThumbprint, publicKey);

    // Ignore this result, the cached value should not be evicted.
    delegate.getPublicKey(certificateThumbprint);

    Optional<PublicKey> cachedOptional = delegate.getPublicKey(certificateThumbprint);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(publicKey)));
  }

  @Test
  void shouldReturnEmptyTraineeIdentifierWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<String> cachedOptional = delegate.getTraineeIdentifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedTraineeIdentifierAfterCaching() {
    UUID key = UUID.randomUUID();

    String traineeIdentifier = "123";
    String cachedData = delegate.cacheTraineeIdentifier(key, traineeIdentifier);
    assertThat("Unexpected cached value.", cachedData, is(traineeIdentifier));
  }

  @Test
  void shouldGetCachedTraineeIdentifierWhenCached() {
    UUID key = UUID.randomUUID();

    String traineeIdentifier = "123";
    delegate.cacheTraineeIdentifier(key, traineeIdentifier);

    Optional<String> cachedOptional = delegate.getTraineeIdentifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(traineeIdentifier)));
  }

  @Test
  void shouldRemoveTraineeIdentifierWhenRetrieved() {
    UUID key = UUID.randomUUID();

    String traineeIdentifier = "123";
    delegate.cacheTraineeIdentifier(key, traineeIdentifier);

    // Ignore this result, the cached value should be evicted.
    delegate.getTraineeIdentifier(key);

    Optional<String> cachedOptional = delegate.getTraineeIdentifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyUnverifiedSessionWhenNotCached() {
    UUID key = UUID.randomUUID();

    Optional<String> cachedOptional = delegate.getUnverifiedSessionIdentifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedUnverifiedSessionAfterCaching() {
    UUID key = UUID.randomUUID();

    String unverifiedSession = "unverifiedSession1";
    String cachedString = delegate.cacheUnverifiedSessionIdentifier(key, unverifiedSession);
    assertThat("Unexpected cached value.", cachedString, is(unverifiedSession));
  }

  @Test
  void shouldGetCachedUnverifiedSessionWhenCached() {
    UUID key = UUID.randomUUID();

    String unverifiedSession = "unverifiedSession1";
    delegate.cacheUnverifiedSessionIdentifier(key, unverifiedSession);

    Optional<String> cachedOptional = delegate.getUnverifiedSessionIdentifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(unverifiedSession)));
  }

  @Test
  void shouldRemoveUnverifiedSessionWhenRetrieved() {
    UUID key = UUID.randomUUID();

    String unverifiedSession = "unverifiedSession1";
    delegate.cacheUnverifiedSessionIdentifier(key, unverifiedSession);

    // Ignore this result, the cached value should be evicted.
    delegate.getUnverifiedSessionIdentifier(key);

    Optional<String> cachedOptional = delegate.getUnverifiedSessionIdentifier(key);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnEmptyVerifiedSessionWhenNotCached() {
    String verifiedSession = UUID.randomUUID().toString();
    Optional<String> cachedOptional = delegate.getVerifiedSessionIdentifier(verifiedSession);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.empty()));
  }

  @Test
  void shouldReturnCachedVerifiedSessionAfterCaching() {
    String verifiedSession = UUID.randomUUID().toString();
    String cachedString = delegate.cacheVerifiedSessionIdentifier(verifiedSession);
    assertThat("Unexpected cached value.", cachedString, is(verifiedSession));
  }

  @Test
  void shouldGetCachedVerifiedSessionWhenCached() {
    String verifiedSession = UUID.randomUUID().toString();
    delegate.cacheVerifiedSessionIdentifier(verifiedSession);

    Optional<String> cachedOptional = delegate.getVerifiedSessionIdentifier(verifiedSession);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(verifiedSession)));
  }

  @Test
  void shouldNotRemoveVerifiedSessionWhenRetrieved() {
    String verifiedSession = UUID.randomUUID().toString();
    delegate.cacheVerifiedSessionIdentifier(verifiedSession);

    // Ignore this result, the cached value should not be evicted.
    delegate.getVerifiedSessionIdentifier(verifiedSession);

    Optional<String> cachedOptional = delegate.getVerifiedSessionIdentifier(verifiedSession);
    assertThat("Unexpected cached value.", cachedOptional, is(Optional.of(verifiedSession)));
  }
}
