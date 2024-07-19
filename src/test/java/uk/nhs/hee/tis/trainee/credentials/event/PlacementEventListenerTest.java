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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.dto.DeleteEventDto;
import uk.nhs.hee.tis.trainee.credentials.dto.RecordDto;
import uk.nhs.hee.tis.trainee.credentials.dto.UpdateEventDto;
import uk.nhs.hee.tis.trainee.credentials.service.RevocationService;
import uk.nhs.hee.tis.trainee.credentials.utill.Md5Hash;

class PlacementEventListenerTest {

  private static final String TIS_ID = UUID.randomUUID().toString();

  private PlacementEventListener listener;
  private Md5Hash hash;
  private RevocationService service;

  @BeforeEach
  void setUp() {
    service = mock(RevocationService.class);
    hash = mock(Md5Hash.class);
    listener = new PlacementEventListener(service);
  }

  @Test
  void shouldDeletePlacement() {
    DeleteEventDto dto = new DeleteEventDto(TIS_ID);

    listener.deletePlacement(dto);

    verify(service).revoke(TIS_ID, CredentialType.TRAINING_PLACEMENT, null);
  }

  @Test
  void shouldUpdatePlacement() {
    Map<String, String> data = new HashMap<>();
    // Use the correct keys expected by the updateProgrammeMembership method
    data.put("specialty", "placementSpeciality");
    data.put("grade", "placementGrade");
    data.put("nationalPostNumber", "placementNatPostNum");
    data.put("employingBody", "placementEmployingBody");
    data.put("site", "placementSite");
    data.put("startDate", LocalDate.of(2023, 1, 1).toString());
    data.put("endDate", LocalDate.of(2024, 1, 1).toString());

    RecordDto recordDto = new RecordDto();
    recordDto.setData(data);

    UpdateEventDto dto = new UpdateEventDto(TIS_ID, recordDto);

    RecordDto recrd = dto.recrd();
    String placementSpeciality = recrd.getData().get("specialty");
    String placementGrade = recrd.getData().get("grade");
    String placementNatPostNum = recrd.getData().get("nationalPostNumber");
    String placementEmployingBody = recrd.getData().get("employingBody");
    String placementSite = recrd.getData().get("site");
    String dateFrom = String.valueOf(
        LocalDate.parse(recrd.getData().get("dateFrom")));
    String dateTo = String.valueOf(LocalDate.parse(recrd.getData().get("dateTo")));

    String placementMd5Hash = hash.createMd5Hash(placementSpeciality + placementGrade
        + placementNatPostNum + placementEmployingBody + placementSite + dateFrom + dateTo);

    listener.updatePlacement(dto);

    verify(service).revoke(TIS_ID, CredentialType.TRAINING_PLACEMENT, placementMd5Hash);
  }
}
