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
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

class CredentialMetadataServiceTest {

  private static final String TIS_ID_1 = UUID.randomUUID().toString();
  private static final String TIS_ID_2 = UUID.randomUUID().toString();

  private static final String TRAINEE_ID = "47165";
  private CredentialMetadataService service;

  private CredentialMetadataRepository repository;

  @BeforeEach
  public void setUp() {
    repository = mock(CredentialMetadataRepository.class);
    service = new CredentialMetadataService(repository);
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldGetTheLatestCredentialForEachResource(CredentialType type) {
    CredentialMetadata credential1Max = new CredentialMetadata();
    credential1Max.setTisId(TIS_ID_1);
    credential1Max.setIssuedAt(Instant.MAX);
    credential1Max.setCredentialId(UUID.randomUUID().toString());
    credential1Max.setTraineeId(TRAINEE_ID);
    credential1Max.setCredentialType(type.getDisplayName());

    CredentialMetadata credential1Min = new CredentialMetadata();
    credential1Min.setTisId(TIS_ID_1);
    credential1Min.setIssuedAt(Instant.MIN);
    credential1Min.setCredentialId(UUID.randomUUID().toString());
    credential1Min.setTraineeId(TRAINEE_ID);
    credential1Min.setCredentialType(type.getDisplayName());

    CredentialMetadata credential2 = new CredentialMetadata();
    credential2.setTisId(TIS_ID_2);
    credential2.setIssuedAt(Instant.MAX);
    credential2.setCredentialId(UUID.randomUUID().toString());
    credential2.setTraineeId(TRAINEE_ID);
    credential2.setCredentialType(type.getDisplayName());

    when(repository.findByCredentialTypeAndTraineeId(type.getIssuanceScope(),
        TRAINEE_ID)).thenReturn(List.of(credential1Max, credential1Min, credential2));

    List<CredentialMetadata> latestCredentials = service.getLatestCredentialsForType(type,
        TRAINEE_ID);

    assertThat("Unexpected credential count", latestCredentials.size(), is(2));
    assertThat("latestCredentials incorrect contents", latestCredentials,
        hasItems(credential1Max, credential2));
  }

  @ParameterizedTest
  @EnumSource(CredentialType.class)
  void shouldThrowErrorGetLatestCredentialsWhenIssuedAtNull(CredentialType type) {
    CredentialMetadata credential = new CredentialMetadata();
    credential.setTisId(TIS_ID_2);
    credential.setIssuedAt(null);
    credential.setCredentialId(UUID.randomUUID().toString());
    credential.setTraineeId(TRAINEE_ID);
    credential.setCredentialType(type.getDisplayName());

    when(repository.findByCredentialTypeAndTraineeId(type.getIssuanceScope(),
        TRAINEE_ID)).thenReturn(List.of(credential));

    assertThrows(NoSuchElementException.class,
        () -> service.getLatestCredentialsForType(type, TRAINEE_ID));
  }
}
