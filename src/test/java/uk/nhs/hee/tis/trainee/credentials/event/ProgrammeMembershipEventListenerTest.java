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

class ProgrammeMembershipEventListenerTest {

  private static final String TIS_ID = UUID.randomUUID().toString();

  private ProgrammeMembershipEventListener listener;
  private Md5Hash hash;
  private RevocationService service;

  @BeforeEach
  void setUp() {
    service = mock(RevocationService.class);
    hash = mock(Md5Hash.class);
    listener = new ProgrammeMembershipEventListener(service);
  }

  @Test
  void shouldDeleteProgrammeMembership() {
    DeleteEventDto dto = new DeleteEventDto(TIS_ID);

    listener.deleteProgrammeMembership(dto);

    verify(service).revoke(TIS_ID, CredentialType.TRAINING_PROGRAMME, null);
  }

  @Test
  void shouldUpdateProgrammeMembership() {
    Map<String, String> data = new HashMap<>();
    // Use the correct keys expected by the updateProgrammeMembership method
    data.put("programmeName", "programmeNameValue");
    data.put("startDate", LocalDate.of(2023, 1, 1).toString());
    data.put("endDate", LocalDate.of(2024, 1, 1).toString());

    RecordDto recordDto = new RecordDto();
    recordDto.setData(data);

    UpdateEventDto dto = new UpdateEventDto(TIS_ID, recordDto);

    RecordDto recrd = dto.recrd();
    String programmeName = recrd.getData().get("programmeName");
    String startDate = String.valueOf(
        LocalDate.parse(recrd.getData().get("startDate")));
    String endDate = String.valueOf(LocalDate.parse(recrd.getData().get("endDate")));

    String programmeMd5Hash = hash.createMd5Hash(programmeName + startDate + endDate);

    listener.updateProgrammeMembership(dto);

    verify(service).revoke(TIS_ID, CredentialType.TRAINING_PROGRAMME, programmeMd5Hash);
  }


}
