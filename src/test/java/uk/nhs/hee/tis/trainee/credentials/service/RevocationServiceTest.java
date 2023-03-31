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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.model.ModificationMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;
import uk.nhs.hee.tis.trainee.credentials.repository.ModificationMetadataRepository;

class RevocationServiceTest {

  private static final String TIS_ID = UUID.randomUUID().toString();
  private static final String CREDENTIAL_ID = UUID.randomUUID().toString();

  private RevocationService service;
  private CredentialMetadataRepository credentialMetadataRepository;
  private ModificationMetadataRepository repository;
  private GatewayService gatewayService;

  @BeforeEach
  void setUp() {
    credentialMetadataRepository = mock(CredentialMetadataRepository.class);
    repository = mock(ModificationMetadataRepository.class);
    gatewayService = mock(GatewayService.class);
    service = new RevocationService(credentialMetadataRepository, repository, gatewayService);
  }

  @Test
  void shouldReturnPresentLastModifiedWhenMetadataFound() {
    Instant now = Instant.now();
    ModificationMetadata metadata = new ModificationMetadata("", null, now);
    when(repository.findById(any())).thenReturn(Optional.of(metadata));

    Optional<Instant> lastModifiedDate = service.getLastModifiedDate("", null);

    assertThat("Unexpected last modified date presence", lastModifiedDate.isPresent(), is(true));
    assertThat("Unexpected last modified date", lastModifiedDate.get(), is(now));
  }

  @Test
  void shouldReturnEmptyLastModifiedWhenMetadataNotFound() {
    when(repository.findById(any())).thenReturn(Optional.empty());

    Optional<Instant> lastModifiedDate = service.getLastModifiedDate("", null);

    assertThat("Unexpected last modified date presence", lastModifiedDate.isEmpty(), is(true));
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotAttemptRevocationWhenCredentialNotIssued(CredentialType credentialType) {
    when(
        credentialMetadataRepository.findByCredentialTypeAndTisId(credentialType.getIssuanceScope(),
            TIS_ID)).thenReturn(Optional.empty());

    service.revoke(TIS_ID, credentialType);

    verifyNoInteractions(gatewayService);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotDeleteCredentialMetadataWhenRevocationFails(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        Optional.of(credentialMetadata));
    doThrow(ResponseStatusException.class).when(gatewayService)
        .revokeCredential(credentialType.getTemplateName(), CREDENTIAL_ID);

    assertThrows(ResponseStatusException.class, () -> service.revoke(TIS_ID, credentialType));

    verify(credentialMetadataRepository).findByCredentialTypeAndTisId(scope, TIS_ID);
    verifyNoMoreInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldDeleteCredentialMetadataWhenRevocationSuccessful(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        Optional.of(credentialMetadata));

    service.revoke(TIS_ID, credentialType);

    verify(gatewayService).revokeCredential(credentialType.getTemplateName(), CREDENTIAL_ID);
    verify(credentialMetadataRepository).deleteById(CREDENTIAL_ID);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldRevokeStaleWhenModifiedAfterBaseline(CredentialType credentialType) {
    Instant baseline = Instant.now();
    Instant modified = baseline.plus(Duration.ofDays(1));

    ModificationMetadata metadata = new ModificationMetadata(null, modified);
    when(repository.findById(any())).thenReturn(Optional.of(metadata));

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(any(), any())).thenReturn(
        Optional.of(new CredentialMetadata()));

    boolean revoked = service.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialType, baseline);

    assertThat("Unexpected revoked flag.", revoked, is(true));
    verify(gatewayService).revokeCredential(any(), any());
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotRevokeStaleWhenModifiedBeforeBaseline(CredentialType credentialType) {
    Instant baseline = Instant.now();
    Instant modified = baseline.minus(Duration.ofDays(1));

    ModificationMetadata metadata = new ModificationMetadata(null, modified);
    when(repository.findById(any())).thenReturn(Optional.of(metadata));

    boolean revoked = service.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialType, baseline);

    assertThat("Unexpected revoked flag.", revoked, is(false));
    verifyNoInteractions(gatewayService);
    verifyNoInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotRevokeStaleWhenModifiedEqualsBaseline(CredentialType credentialType) {
    Instant baseline = Instant.now();

    ModificationMetadata metadata = new ModificationMetadata(null, baseline);
    when(repository.findById(any())).thenReturn(Optional.of(metadata));

    boolean revoked = service.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialType, baseline);

    assertThat("Unexpected revoked flag.", revoked, is(false));
    verifyNoInteractions(gatewayService);
    verifyNoInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotRevokeStaleWhenNotModified(CredentialType credentialType) {
    Instant baseline = Instant.now();

    when(repository.findById(any())).thenReturn(Optional.empty());

    boolean revoked = service.revokeIfStale(CREDENTIAL_ID, TIS_ID, credentialType, baseline);

    assertThat("Unexpected revoked flag.", revoked, is(false));
    verifyNoInteractions(gatewayService);
    verifyNoInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldStoreLastModifiedDateWhenRevoking(CredentialType credentialType) {
    service.revoke(TIS_ID, credentialType);

    ArgumentCaptor<ModificationMetadata> metadataCaptor = ArgumentCaptor.forClass(
        ModificationMetadata.class);
    verify(repository).save(metadataCaptor.capture());

    ModificationMetadata metadata = metadataCaptor.getValue();
    assertThat("Unexpected TIS ID.", metadata.id().tisId(), is(TIS_ID));
    assertThat("Unexpected credential type.", metadata.id().credentialType(), is(credentialType));

    Instant timestamp = metadata.lastModifiedDate();
    Instant now = Instant.now();
    int delta = (int) Duration.between(timestamp, now).toMinutes();
    assertThat("Unexpected modification timestamp delta.", delta, is(0));
  }
}
