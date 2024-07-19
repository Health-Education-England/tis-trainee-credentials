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

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.dto.DeleteEventDto;
import uk.nhs.hee.tis.trainee.credentials.dto.RecordDto;
import uk.nhs.hee.tis.trainee.credentials.dto.UpdateEventDto;
import uk.nhs.hee.tis.trainee.credentials.service.RevocationService;
import uk.nhs.hee.tis.trainee.credentials.utill.Md5Hash;

/**
 * An event listener for placement deletes / updates.
 */
@Slf4j
@Component
public class PlacementEventListener {

  private final RevocationService revocationService;

  PlacementEventListener(RevocationService revocationService) {
    this.revocationService = revocationService;
  }

  /**
   * Listener for delete events.
   *
   * @param deletedPlacement The deleted placement.
   */
  @SqsListener("${application.aws.sqs.delete-placement}")
  void deletePlacement(DeleteEventDto deletedPlacement) {
    log.info("Received delete event for placement {}.", deletedPlacement);
    revocationService.revoke(deletedPlacement.tisId(), CredentialType.TRAINING_PLACEMENT, null);
  }

  /**
   * Listener for update events.
   *
   * @param updatedPlacement The updated placement.
   */
  @SqsListener("${application.aws.sqs.update-placement}")
  void updatePlacement(UpdateEventDto updatedPlacement) {
    log.info("Received update event for placement {}.", updatedPlacement);

    RecordDto recrd = updatedPlacement.recrd();
    String specialty = recrd.getData().get("specialty");
    String grade = recrd.getData().get("grade");
    String nationalPostNumber = recrd.getData().get("nationalPostNumber");
    String employingBody = recrd.getData().get("employingBody");
    String site = recrd.getData().get("site");
    String dateFrom = String.valueOf(
        LocalDate.parse(recrd.getData().get("dateFrom")));
    String dateTo = String.valueOf(LocalDate.parse(recrd.getData().get("dateTo")));


    String placementMd5Hash = Md5Hash.createMd5Hash(specialty + grade + nationalPostNumber
                                                      + employingBody + site + dateFrom + dateTo);

    revocationService.revoke(updatedPlacement.tisId(), CredentialType.TRAINING_PLACEMENT,
        placementMd5Hash);
  }


}
