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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

class CredentialMetadataServiceTest {

  private static final String TIS_ID_1 = UUID.randomUUID().toString();
  private static final String TIS_ID_2 = UUID.randomUUID().toString();

  private static final String TRAINEE_ID = "47165";

  private static final String PLACEMENT_DISPLAY_NAME =
      CredentialType.TRAINING_PLACEMENT.getDisplayName();
  private static final String PROGRAMME_DISPLAY_NAME =
      CredentialType.TRAINING_PROGRAMME.getDisplayName();

  private CredentialMetadata placement1Cred1;
  private CredentialMetadata placement1Cred2;
  private CredentialMetadata placement2Cred1;

  private CredentialMetadata programmeCred1;
  private CredentialMetadata programmeCred2;

  private CredentialMetadataService service;
  @Mock
  CredentialMetadataRepository repository;


  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    service = new CredentialMetadataService(repository);

    placement1Cred1 = new CredentialMetadata();
    placement1Cred1.setTisId(TIS_ID_1);
    placement1Cred1.setIssuedAt(Instant.MAX);
    placement1Cred1.setCredentialId(UUID.randomUUID().toString());
    placement1Cred1.setTraineeId(TRAINEE_ID);
    placement1Cred1.setCredentialType(PLACEMENT_DISPLAY_NAME);

    placement1Cred2 = new CredentialMetadata();
    placement1Cred2.setTisId(TIS_ID_1);
    placement1Cred2.setIssuedAt(Instant.MIN);
    placement1Cred2.setCredentialId(UUID.randomUUID().toString());
    placement1Cred2.setTraineeId(TRAINEE_ID);
    placement1Cred2.setCredentialType(PLACEMENT_DISPLAY_NAME);

    placement2Cred1 = new CredentialMetadata();
    placement2Cred1.setTisId(TIS_ID_2);
    placement2Cred1.setIssuedAt(Instant.MAX);
    placement2Cred1.setCredentialId(UUID.randomUUID().toString());
    placement2Cred1.setTraineeId(TRAINEE_ID);
    placement2Cred1.setCredentialType(PLACEMENT_DISPLAY_NAME);

    programmeCred1 = new CredentialMetadata();
    programmeCred1.setTisId(TIS_ID_1);
    programmeCred1.setIssuedAt(Instant.MAX);
    programmeCred1.setCredentialId(UUID.randomUUID().toString());
    programmeCred1.setTraineeId(TRAINEE_ID);
    programmeCred1.setCredentialType(PROGRAMME_DISPLAY_NAME);

    programmeCred2 = new CredentialMetadata();
    programmeCred2.setTisId(TIS_ID_1);
    programmeCred2.setIssuedAt(Instant.MIN);
    programmeCred2.setCredentialId(UUID.randomUUID().toString());
    programmeCred2.setTraineeId(TRAINEE_ID);
    programmeCred2.setCredentialType(PROGRAMME_DISPLAY_NAME);
  }

  @Test
  void getTheLatestPlacementCredential() {
    CredentialType type = CredentialType.TRAINING_PLACEMENT;

    when(repository.findByCredentialTypeAndTraineeId(type.getIssuanceScope(), TRAINEE_ID))
        .thenReturn(List.of(placement1Cred1, placement1Cred2, placement2Cred1));

    List<CredentialMetadata> latestCredentials =
        service.getLatestCredentialsForType(type, TRAINEE_ID);

    assertThat("Unexpected credential count", latestCredentials.size(), is(2));
    assertThat("latestCredentials incorrect contents", latestCredentials,
        hasItems(placement1Cred1, placement2Cred1));
  }

  @Test
  void getTheLatestProgrammeCredential() {
    CredentialType type = CredentialType.TRAINING_PROGRAMME;

    when(repository.findByCredentialTypeAndTraineeId(type.getIssuanceScope(),
        TRAINEE_ID)).thenReturn(List.of(programmeCred1, programmeCred2));

    List<CredentialMetadata> latestCredentials =
        service.getLatestCredentialsForType(type, TRAINEE_ID);

    assertThat("Unexpected credential count", latestCredentials.size(), is(1));
    assertThat("Unexpected credential", latestCredentials, hasItem(programmeCred1));
  }

  @Test
  void testGetLatestCredentialsForTypeWithError() {
    CredentialType type = CredentialType.TRAINING_PROGRAMME;
    programmeCred1.setIssuedAt(null);

    when(repository.findByCredentialTypeAndTraineeId(type.getIssuanceScope(),
        TRAINEE_ID))
        .thenReturn(List.of(programmeCred1));

    assertThrows(NoSuchElementException.class, () -> {
      service.getLatestCredentialsForType(
          type, TRAINEE_ID);
    });
  }
}
