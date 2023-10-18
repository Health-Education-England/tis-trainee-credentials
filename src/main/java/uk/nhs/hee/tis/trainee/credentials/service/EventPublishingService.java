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

import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialEventDto;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialEventMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;

/**
 * A service handling publishing of events to an external message system.
 */
@Slf4j
@Service
public class EventPublishingService {

  private static final String ROUTING_KEY = "event_type";
  private static final String ROUTING_REVOCATION = "CREDENTIAL_REVOKED";

  private final SnsTemplate snsTemplate;
  private final URI topicArn;
  private final CredentialEventMapper mapper;

  /**
   * A service handling publishing of events to an Amazon SNS.
   *
   * @param snsTemplate        The SNS template to publish with.
   * @param revocationTopicArn The topic ARN to publish revocation events to.
   * @param mapper             A mapper to allow creation of events from other data types.
   */
  public EventPublishingService(SnsTemplate snsTemplate,
      @Value("${application.aws.sns.revocation-topic}") URI revocationTopicArn,
      CredentialEventMapper mapper) {
    this.snsTemplate = snsTemplate;
    this.topicArn = revocationTopicArn;
    this.mapper = mapper;
  }

  /**
   * Publish a revocation event.
   *
   * @param credentialMetadata The metadata of the credential that was revoked.
   */
  public void publishRevocationEvent(CredentialMetadata credentialMetadata) {
    String credentialId = credentialMetadata.getCredentialId();
    log.info("Publishing revocation event for credential {}", credentialId);
    CredentialEventDto credentialEvent = mapper.toCredentialEvent(credentialMetadata);

    SnsNotification<CredentialEventDto> message = SnsNotification.builder(credentialEvent)
        .groupId(credentialMetadata.getCredentialId())
        .header(ROUTING_KEY, ROUTING_REVOCATION)
        .build();
    snsTemplate.sendNotification(topicArn.toString(), message);
    log.info("Published revocation event for credential {} to topic {}", credentialId, topicArn);
  }
}
