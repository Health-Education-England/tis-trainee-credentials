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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.trainee.credentials.SignatureTestUtil;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService;

@WebMvcTest(IssueResource.class)
class IssueResourceTest {

  public static final String UNSIGNED_DATA = """
      {
            "givenName": "Anthony",
            "familyName": "Gilliam",
            "birthDate": "1991-11-11",
            "signature": {
              "signedAt": "%s",
              "validUntil": "%s"
            }
          }
      """.formatted(Instant.MIN, Instant.MAX);

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private GatewayService service;

  @MockBean
  private RestTemplateBuilder restTemplateBuilder;

  @Value("${application.signature.secret-key}")
  private String secretKey;

  @Test
  void shouldForbidUnsignedRequests() throws Exception {
    mockMvc.perform(
            post("/api/issue/test")
                .content(UNSIGNED_DATA)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  void shouldReturnErrorWhenCredentialUriNotAvailable() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    when(service.getCredentialUri(any(), any(), eq("issue.TestCredential"))).thenReturn(
        Optional.empty());

    mockMvc.perform(
            post("/api/issue/test")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void shouldReturnCreatedWhenCredentialUriAvailable() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    String credentialUriString = "the-credential-uri";
    URI credentialUri = URI.create(credentialUriString);
    when(service.getCredentialUri(any(), any(), eq("issue.TestCredential"))).thenReturn(
        Optional.of(credentialUri));

    mockMvc.perform(
            post("/api/issue/test")
                .content(signedData)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.LOCATION, credentialUriString))
        .andExpect(content().string(credentialUriString));
  }

  @Test
  void shouldPassStateDownstreamWhenStateGiven() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    URI credentialUri = URI.create("the-credential-uri");
    when(service.getCredentialUri(any(), any(), eq("issue.TestCredential"))).thenReturn(
        Optional.of(credentialUri));

    mockMvc.perform(
        post("/api/issue/test")
            .queryParam("state", "some-state-value")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(service).getCredentialUri(any(), stateCaptor.capture(), eq("issue.TestCredential"));

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, is("some-state-value"));
  }

  @Test
  void shouldNotPassStateDownstreamWhenNoStateGiven() throws Exception {
    String signedData = SignatureTestUtil.signData(UNSIGNED_DATA, secretKey);

    mockMvc.perform(
        post("/api/issue/test")
            .content(signedData)
            .contentType(MediaType.APPLICATION_JSON));

    ArgumentCaptor<String> stateCaptor = ArgumentCaptor.forClass(String.class);
    verify(service).getCredentialUri(any(), stateCaptor.capture(), eq("issue.TestCredential"));

    String state = stateCaptor.getValue();
    assertThat("Unexpected state.", state, nullValue());
  }
}
