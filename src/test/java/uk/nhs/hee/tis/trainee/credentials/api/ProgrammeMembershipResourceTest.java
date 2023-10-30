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

package uk.nhs.hee.tis.trainee.credentials.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.nhs.hee.tis.trainee.credentials.TestJwtUtil;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapperImpl;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.service.CredentialMetadataService;

@ContextConfiguration(classes = {CredentialMetadataMapperImpl.class})
@ExtendWith(SpringExtension.class)
@WebMvcTest(ProgrammeMembershipResource.class)
class ProgrammeMembershipResourceTest {

  private MockMvc mockMvc;
  @Autowired
  private MappingJackson2HttpMessageConverter jacksonMessageConverter;
  @Mock
  private CredentialMetadataService service;
  private CredentialMetadata cred1 = new CredentialMetadata();
  private CredentialMetadata cred2 = new CredentialMetadata();
  private static final String CREDENTIAL_ID_1 = "e2a26a50-2c51-4327-8692-1cb05b80af07";
  private static final String TRAINEE_ID_1 = "47165";
  private static final String TIS_ID_1 = "2bd3f009-9c7a-4352-bcc6-9dff8eac07d6";
  private static final String CREDENTIAL_TYPE_1 = "TRAINING_PROGRAMME";
  private static final String ISSUED_AT_1 = "2017-02-03T10:37:30Z";
  private static final String REVOKED_AT_1 = "2018-02-03T10:37:30Z";
  private static final String CREDENTIAL_ID_2 = "e2a26a50-2c51-4327-8692-1cb05g32af22";
  private static final String TRAINEE_ID_2 = "47165";
  private static final String TIS_ID_2 = "2bd3f009-9c7a-4352-bcc6-9dff8eac07u9";
  private static final String CREDENTIAL_TYPE_2 = "TRAINING_PROGRAMME";
  private static final String ISSUED_AT_2 = "2011-02-03T10:37:30Z";
  private static final Instant REVOKED_AT_2 = null;

  /**
   * Set up mocks before each test.
   */
  @BeforeEach
  void setup() {
    ProgrammeMembershipResource programmeMembershipResource = new ProgrammeMembershipResource(
        service, new CredentialMetadataMapperImpl());
    this.mockMvc = MockMvcBuilders.standaloneSetup(programmeMembershipResource)
        .setMessageConverters(jacksonMessageConverter)
        .build();

    cred1.setCredentialId(CREDENTIAL_ID_1);
    cred1.setTraineeId(TRAINEE_ID_1);
    cred1.setTisId(TIS_ID_1);
    cred1.setCredentialType(CREDENTIAL_TYPE_1);
    cred1.setIssuedAt(Instant.parse(ISSUED_AT_1));
    cred1.setRevokedAt(Instant.parse(REVOKED_AT_1));

    cred2.setCredentialId(CREDENTIAL_ID_2);
    cred2.setTraineeId(TRAINEE_ID_2);
    cred2.setTisId(TIS_ID_2);
    cred2.setCredentialType(CREDENTIAL_TYPE_2);
    cred2.setIssuedAt(Instant.parse(ISSUED_AT_2));
    cred2.setRevokedAt(REVOKED_AT_2);
  }

  @Test
  void testSuccessfullyCallTheService() throws Exception {

    String tisId = "47165";
    String token = TestJwtUtil.generateTokenForTisId(tisId);

    when(service.getLatestCredentialsForType(CredentialType.TRAINING_PROGRAMME, tisId))
        .thenReturn(List.of(cred1));

    mockMvc.perform(get("/api/programme-membership")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.[*].credentialId").value(CREDENTIAL_ID_1))
        .andExpect(jsonPath("$.[*].traineeId").value(TRAINEE_ID_1))
        .andExpect(jsonPath("$.[*].tisId").value(TIS_ID_1))
        .andExpect(jsonPath("$.[*].credentialType").value(CREDENTIAL_TYPE_1))
        .andExpect(jsonPath("$.[*].issuedAt").value(ISSUED_AT_1))
        .andExpect(jsonPath("$.[*].revokedAt").value(REVOKED_AT_1));
  }

  @Test
  void testReturnListOfResultsWithCorrectValues() throws Exception {
    String tisId = "47165";
    String token = TestJwtUtil.generateTokenForTisId(tisId);

    when(service.getLatestCredentialsForType(CredentialType.TRAINING_PROGRAMME, tisId))
        .thenReturn(List.of(cred1, cred2));

    mockMvc.perform(get("/api/programme-membership")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.[0].credentialId").value(CREDENTIAL_ID_1))
        .andExpect(jsonPath("$.[0].traineeId").value(TRAINEE_ID_1))
        .andExpect(jsonPath("$.[0].tisId").value(TIS_ID_1))
        .andExpect(jsonPath("$.[0].credentialType").value(CREDENTIAL_TYPE_1))
        .andExpect(jsonPath("$.[0].issuedAt").value(ISSUED_AT_1))
        .andExpect(jsonPath("$.[0].revokedAt").value(REVOKED_AT_1))
        .andExpect(jsonPath("$.[1].credentialId").value(CREDENTIAL_ID_2))
        .andExpect(jsonPath("$.[1].traineeId").value(TRAINEE_ID_2))
        .andExpect(jsonPath("$.[1].tisId").value(TIS_ID_2))
        .andExpect(jsonPath("$.[1].credentialType").value(CREDENTIAL_TYPE_2))
        .andExpect(jsonPath("$.[1].issuedAt").value(ISSUED_AT_2))
        .andExpect(jsonPath("$.[1].revokedAt").value(REVOKED_AT_2));
  }

  @Test
  void getShouldReturnBadRequestWhenTokenNotMap() throws Exception {
    String token = TestJwtUtil.generateToken("[]");

    this.mockMvc.perform(get("/api/programme-membership")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isBadRequest());
  }
}
