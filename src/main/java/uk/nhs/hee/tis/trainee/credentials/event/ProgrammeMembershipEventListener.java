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

package uk.nhs.hee.tis.trainee.credentials.event;

import static io.awspring.cloud.messaging.listener.SqsMessageDeletionPolicy.ON_SUCCESS;

import io.awspring.cloud.messaging.listener.annotation.SqsListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.dto.DeleteEventDto;
import uk.nhs.hee.tis.trainee.credentials.service.RevocationService;

/**
 * An event listener for programme membership deletes / updates.
 */
@Slf4j
@Component
public class ProgrammeMembershipEventListener {

  private final RevocationService revocationService;

  ProgrammeMembershipEventListener(RevocationService revocationService) {
    this.revocationService = revocationService;
  }

  /**
   * Listener for delete events.
   *
   * @param deletedProgrammeMembership The deleted programme membership.
   */
  @SqsListener(value = "${application.aws.sqs.delete-programme-membership}",
      deletionPolicy = ON_SUCCESS)
  void deleteProgrammeMembership(DeleteEventDto deletedProgrammeMembership) {
    log.info("Received delete event for programme membership {}.", deletedProgrammeMembership);
    revocationService.revoke(deletedProgrammeMembership.tisId(), CredentialType.TRAINING_PROGRAMME,
        deletedProgrammeMembership.timestamp());
  }
}
