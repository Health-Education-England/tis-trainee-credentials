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

import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialEventDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialEventMapperImpl;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;

class EventPublishingServiceTest {

  private static final URI REVOCATION_TOPIC_ARN = URI.create("arn:sns:test");

  private static final UUID CREDENTIAL_ID = UUID.randomUUID();
  private static final String TRAINEE_ID = "traineeId";
  private static final String TIS_ID = "tisId";

  private static final Instant ISSUED_AT = Instant.now();
  private static final Instant REVOKED_AT = Instant.now().plus(Duration.ofHours(1));

  private EventPublishingService service;
  private SnsTemplate snsTemplate;

  @BeforeEach
  void setUp() {
    snsTemplate = mock(SnsTemplate.class);
    service = new EventPublishingService(snsTemplate, REVOCATION_TOPIC_ARN,
        new CredentialEventMapperImpl());
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldSendToRevocationTopicWhenPublishingRevocationEvent(CredentialType credentialType) {
    CredentialMetadata metadata = new CredentialMetadata();
    metadata.setCredentialId(CREDENTIAL_ID.toString());
    metadata.setCredentialType(credentialType.getIssuanceScope());
    metadata.setTisId(TIS_ID);
    metadata.setTraineeId(TRAINEE_ID);

    service.publishRevocationEvent(metadata);

    verify(snsTemplate).sendNotification(eq(REVOCATION_TOPIC_ARN.toString()),
        any(SnsNotification.class));
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldSetGroupIdWhenPublishingRevocationEvent(CredentialType credentialType) {
    CredentialMetadata metadata = new CredentialMetadata();
    metadata.setCredentialId(CREDENTIAL_ID.toString());
    metadata.setCredentialType(credentialType.getIssuanceScope());
    metadata.setTisId(TIS_ID);
    metadata.setTraineeId(TRAINEE_ID);

    service.publishRevocationEvent(metadata);

    ArgumentCaptor<SnsNotification<CredentialEventDto>> messageCaptor = ArgumentCaptor.forClass(
        SnsNotification.class);
    verify(snsTemplate).sendNotification(any(), messageCaptor.capture());

    SnsNotification<CredentialEventDto> message = messageCaptor.getValue();
    assertThat("Unexpected group ID.", message.getGroupId(), is(CREDENTIAL_ID.toString()));
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldSetRoutingKeyWhenPublishingRevocationEvent(CredentialType credentialType) {
    CredentialMetadata metadata = new CredentialMetadata();
    metadata.setCredentialId(CREDENTIAL_ID.toString());
    metadata.setCredentialType(credentialType.getIssuanceScope());
    metadata.setTisId(TIS_ID);
    metadata.setTraineeId(TRAINEE_ID);

    service.publishRevocationEvent(metadata);

    ArgumentCaptor<SnsNotification<CredentialEventDto>> messageCaptor = ArgumentCaptor.forClass(
        SnsNotification.class);
    verify(snsTemplate).sendNotification(any(), messageCaptor.capture());

    SnsNotification<CredentialEventDto> message = messageCaptor.getValue();
    Map<String, Object> headers = message.getHeaders();
    Object eventType = headers.get("event_type");
    assertThat("Unexpected event type.", eventType, is("CREDENTIAL_REVOKED"));
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldIncludePayloadWhenPublishingRevocationEvent(CredentialType credentialType) {
    CredentialMetadata metadata = new CredentialMetadata();
    metadata.setCredentialId(CREDENTIAL_ID.toString());
    metadata.setCredentialType(credentialType.getIssuanceScope());
    metadata.setTisId(TIS_ID);
    metadata.setTraineeId(TRAINEE_ID);
    metadata.setIssuedAt(ISSUED_AT);
    metadata.setRevokedAt(REVOKED_AT);

    service.publishRevocationEvent(metadata);

    ArgumentCaptor<SnsNotification<CredentialEventDto>> messageCaptor = ArgumentCaptor.forClass(
        SnsNotification.class);
    verify(snsTemplate).sendNotification(any(), messageCaptor.capture());

    SnsNotification<CredentialEventDto> message = messageCaptor.getValue();
    CredentialEventDto payload = message.getPayload();
    assertThat("Unexpected credential id.", payload.credentialId(), is(CREDENTIAL_ID));
    assertThat("Unexpected credential type.", payload.credentialType(),
        is(credentialType.getDisplayName()));
    assertThat("Unexpected trainee id.", payload.traineeId(), is(TRAINEE_ID));
    assertThat("Unexpected issued at.", payload.issuedAt(), is(ISSUED_AT));
    assertThat("Unexpected revoked at.", payload.revokedAt(), is(REVOKED_AT));
  }
}
