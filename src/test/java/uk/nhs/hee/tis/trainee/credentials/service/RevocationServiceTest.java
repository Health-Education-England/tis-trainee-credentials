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
import static org.hibernate.validator.internal.util.Contracts.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ResponseStatusException;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.model.ModificationMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;
import uk.nhs.hee.tis.trainee.credentials.repository.ModificationMetadataRepository;

class RevocationServiceTest {

  private static final String TIS_ID = UUID.randomUUID().toString();
  private static final String CREDENTIAL_ID = UUID.randomUUID().toString();
  private static final String CREDENTIAL_HASH_CODE_1 = "12345";
  private static final String CREDENTIAL_HASH_CODE_2 = "23456";
  private static final Logger log = LoggerFactory.getLogger(RevocationService.class);
  private RevocationService service;
  private CredentialMetadataRepository credentialMetadataRepository;
  private ModificationMetadataRepository repository;
  private GatewayService gatewayService;
  private EventPublishingService eventPublishingService;

  @BeforeEach
  void setUp() {
    credentialMetadataRepository = mock(CredentialMetadataRepository.class);
    repository = mock(ModificationMetadataRepository.class);
    gatewayService = mock(GatewayService.class);
    eventPublishingService = mock(EventPublishingService.class);
    service = new RevocationService(credentialMetadataRepository, repository, gatewayService,
        eventPublishingService);
  }

  @Test
  void shouldReturnPresentLastModifiedWhenMetadataFound() {
    Instant now = Instant.now();
    ModificationMetadata metadata = new ModificationMetadata("", null, now);
    when(repository.findById(any())).thenReturn(Optional.of(metadata));

    Optional<Instant> lastModifiedDate = service.getLastModifiedDate("", null);

    assertThat("Unexpected last modified date presence", lastModifiedDate.isPresent(),
        is(true));
    assertThat("Unexpected last modified date", lastModifiedDate.get(), is(now));
  }

  @Test
  void shouldReturnEmptyLastModifiedWhenMetadataNotFound() {
    when(repository.findById(any())).thenReturn(Optional.empty());

    Optional<Instant> lastModifiedDate = service.getLastModifiedDate("", null);

    assertThat("Unexpected last modified date presence", lastModifiedDate.isEmpty(),
        is(true));
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotAttemptRevocationWhenCredentialNotIssued(CredentialType credentialType) {
    when(
        credentialMetadataRepository.findByCredentialTypeAndTisId(credentialType.getIssuanceScope(),
            TIS_ID)).thenReturn(Collections.emptyList());

    service.revoke(TIS_ID, credentialType, null);

    verifyNoInteractions(gatewayService);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotAttemptRevocationWhenCredentialAlreadyRevoked(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setRevokedAt(Instant.now());

    when(
        credentialMetadataRepository.findByCredentialTypeAndTisId(credentialType.getIssuanceScope(),
            TIS_ID)).thenReturn(List.of(credentialMetadata));

    service.revoke(TIS_ID, credentialType, null);

    verifyNoInteractions(gatewayService);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotUpdateCredentialMetadataWhenRevocationFails(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata));
    doThrow(ResponseStatusException.class).when(gatewayService)
        .revokeCredential(credentialType.getTemplateName(), CREDENTIAL_ID);

    assertThrows(ResponseStatusException.class, () -> service.revoke(TIS_ID, credentialType,
        CREDENTIAL_HASH_CODE_2));

    verify(credentialMetadataRepository).findByCredentialTypeAndTisId(scope, TIS_ID);
    verifyNoMoreInteractions(credentialMetadataRepository);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldUpdateCredentialMetadataWhenRevocationSuccessful(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata));

    service.revoke(TIS_ID, credentialType, CREDENTIAL_HASH_CODE_2);

    verify(gatewayService).revokeCredential(credentialType.getTemplateName(), CREDENTIAL_ID);

    ArgumentCaptor<CredentialMetadata> metadataCaptor = ArgumentCaptor.forClass(
        CredentialMetadata.class);
    verify(credentialMetadataRepository).save(metadataCaptor.capture());

    CredentialMetadata revokedMetadata = metadataCaptor.getValue();
    assertThat("Unexpected credential ID.", revokedMetadata.getCredentialId(),
        is(CREDENTIAL_ID));
    assertThat("Unexpected TIS ID.", revokedMetadata.getTisId(), is(TIS_ID));
    assertThat("Unexpected revoked at.", revokedMetadata.getRevokedAt(), notNullValue());
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldNotPublishRevocationEventWhenRevocationFails(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata));
    doThrow(ResponseStatusException.class).when(gatewayService)
        .revokeCredential(credentialType.getTemplateName(), CREDENTIAL_ID);

    assertThrows(ResponseStatusException.class, () -> service.revoke(TIS_ID, credentialType,
        CREDENTIAL_HASH_CODE_2));

    verify(credentialMetadataRepository).findByCredentialTypeAndTisId(scope, TIS_ID);
    verifyNoMoreInteractions(eventPublishingService);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldPublishRevocationEventWhenRevocationSuccessful(CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata));

    service.revoke(TIS_ID, credentialType, CREDENTIAL_HASH_CODE_2);

    verify(gatewayService).revokeCredential(credentialType.getTemplateName(), CREDENTIAL_ID);

    ArgumentCaptor<CredentialMetadata> metadataCaptor = ArgumentCaptor.forClass(
        CredentialMetadata.class);
    verify(eventPublishingService).publishRevocationEvent(metadataCaptor.capture());

    CredentialMetadata revokedMetadata = metadataCaptor.getValue();
    assertThat("Unexpected credential ID.", revokedMetadata.getCredentialId(),
        is(CREDENTIAL_ID));
    assertThat("Unexpected TIS ID.", revokedMetadata.getTisId(), is(TIS_ID));
    assertThat("Unexpected revoked at.", revokedMetadata.getRevokedAt(), notNullValue());
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldPublishMultipleRevocationEventsWhenRevocationSuccessful(
      CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    String credentialId2 = UUID.randomUUID().toString();
    CredentialMetadata credentialMetadata2 = new CredentialMetadata();
    credentialMetadata2.setTisId(TIS_ID);
    credentialMetadata2.setCredentialId(credentialId2);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata, credentialMetadata2));

    service.revoke(TIS_ID, credentialType, CREDENTIAL_HASH_CODE_2);

    ArgumentCaptor<CredentialMetadata> metadataCaptor = ArgumentCaptor.forClass(
        CredentialMetadata.class);
    verify(eventPublishingService, times(4))
        .publishRevocationEvent(metadataCaptor.capture());

    List<CredentialMetadata> revokedMetadata = metadataCaptor.getAllValues();
    CredentialMetadata revokedMetadata1 = revokedMetadata.get(0);
    assertThat("Unexpected credential ID.", revokedMetadata1.getCredentialId(),
        is(CREDENTIAL_ID));
    assertThat("Unexpected TIS ID.", revokedMetadata1.getTisId(), is(TIS_ID));
    assertThat("Unexpected revoked at.", revokedMetadata1.getRevokedAt(), notNullValue());

    CredentialMetadata revokedMetadata2 = revokedMetadata.get(1);
    assertThat("Unexpected credential ID.", revokedMetadata2.getCredentialId(),
        is(credentialId2));
    assertThat("Unexpected TIS ID.", revokedMetadata2.getTisId(), is(TIS_ID));
    assertThat("Unexpected revoked at.", revokedMetadata2.getRevokedAt(), notNullValue());
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldUpdateMultipleCredentialMetadataWhenRevocationSuccessful(
      CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    String credentialId2 = UUID.randomUUID().toString();
    CredentialMetadata credentialMetadata2 = new CredentialMetadata();
    credentialMetadata2.setTisId(TIS_ID);
    credentialMetadata2.setCredentialId(credentialId2);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata, credentialMetadata2));

    service.revoke(TIS_ID, credentialType, CREDENTIAL_HASH_CODE_2);

    ArgumentCaptor<CredentialMetadata> metadataCaptor = ArgumentCaptor.forClass(
        CredentialMetadata.class);
    verify(credentialMetadataRepository, times(4)).save(metadataCaptor
        .capture());

    List<CredentialMetadata> revokedMetadata = metadataCaptor.getAllValues();
    CredentialMetadata revokedMetadata1 = revokedMetadata.get(0);
    assertThat("Unexpected credential ID.", revokedMetadata1.getCredentialId(),
        is(CREDENTIAL_ID));
    assertThat("Unexpected TIS ID.", revokedMetadata1.getTisId(), is(TIS_ID));
    assertThat("Unexpected revoked at.", revokedMetadata1.getRevokedAt(), notNullValue());

    CredentialMetadata revokedMetadata2 = revokedMetadata.get(1);
    assertThat("Unexpected credential ID.", revokedMetadata2.getCredentialId(),
        is(credentialId2));
    assertThat("Unexpected TIS ID.", revokedMetadata2.getTisId(), is(TIS_ID));
    assertThat("Unexpected revoked at.", revokedMetadata2.getRevokedAt(), notNullValue());
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldRevokeStaleWhenModifiedAfterBaseline(CredentialType credentialType) {
    Instant baseline = Instant.now();
    Instant modified = baseline.plus(Duration.ofDays(1));

    ModificationMetadata metadata = new ModificationMetadata(null, modified);
    when(repository.findById(any())).thenReturn(Optional.of(metadata));

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(any(), any())).thenReturn(
        List.of(new CredentialMetadata()));

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
  void shouldNotRevokeCredentialWhenMd5HashMatches(@NotNull CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) log;
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata));
    List<ILoggingEvent> logsList = listAppender.list;
    service.revoke(TIS_ID, credentialType, CREDENTIAL_HASH_CODE_1);

    //verify log is called
    // Assert that the log message is present
    boolean logFound = logsList.stream()
        .anyMatch(event -> event.getFormattedMessage()
            .contains("has no changes to wallet data, skipped revocation."));
    assertTrue(logFound, "Log message not found!");
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldRevokeCredentialWhenMd5HashDoesNotMatch(@NotNull CredentialType credentialType) {
    CredentialMetadata credentialMetadata = new CredentialMetadata();
    credentialMetadata.setTisId(TIS_ID);
    credentialMetadata.setCredentialId(CREDENTIAL_ID);
    credentialMetadata.setCredentialHashCode(CREDENTIAL_HASH_CODE_1);

    ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) log;
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    String scope = credentialType.getIssuanceScope();

    when(credentialMetadataRepository.findByCredentialTypeAndTisId(scope, TIS_ID)).thenReturn(
        List.of(credentialMetadata));

    List<ILoggingEvent> logsList = listAppender.list;
    service.revoke(TIS_ID, credentialType, CREDENTIAL_HASH_CODE_2);

    //verify log is called
    // Assert that the log message is present
    boolean logFound = logsList.stream()
        .anyMatch(event -> event.getFormattedMessage().contains("has been revoked."));
    assertTrue(logFound, "Log message not found!");
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldStoreLastModifiedDateWhenRevoking(CredentialType credentialType) {
    service.revoke(TIS_ID, credentialType, null);

    ArgumentCaptor<ModificationMetadata> metadataCaptor = ArgumentCaptor.forClass(
        ModificationMetadata.class);
    verify(repository).save(metadataCaptor.capture());

    ModificationMetadata metadata = metadataCaptor.getValue();
    assertThat("Unexpected TIS ID.", metadata.id().tisId(), is(TIS_ID));
    assertThat("Unexpected credential type.", metadata.id().credentialType(),
        is(credentialType));

    Instant timestamp = metadata.lastModifiedDate();
    Instant now = Instant.now();
    int delta = (int) Duration.between(timestamp, now).toMinutes();
    assertThat("Unexpected modification timestamp delta.", delta, is(0));
  }
}
