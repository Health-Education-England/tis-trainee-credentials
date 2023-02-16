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

package uk.nhs.hee.tis.trainee.credentials.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.service.IssuedResourceService;

/**
 * A mapper to map between Claims data and Credential Log Data.
 */

@Component
public class CredentialMetadataMapper {

  private final IssuedResourceService issuedResourceService;
  private final ObjectMapper objectMapper;
  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  CredentialMetadataMapper(IssuedResourceService issuedResourceService, ObjectMapper objectMapper) {
    this.issuedResourceService = issuedResourceService;
    this.objectMapper = objectMapper;
  }

  /**
   * Map a claim to the equivalent credential metadata, and supplement with cached details and
   * trainee user ID from the authorization token.
   *
   * @param claims    The Claims data to map.
   * @param authToken The user's authorization token.
   * @return The mapped credential log entry.
   */

  public CredentialMetadata toCredentialMetadata(Claims claims, String authToken)
      throws IOException {

    String[] tokenSections = authToken.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));
    Map<?, ?> payload = objectMapper.readValue(payloadBytes, Map.class);
    String traineeTisId = (String) payload.get(TIS_ID_ATTRIBUTE);

    String credentialId = claims.get("SerialNumber", String.class);
    LocalDateTime issuedAt = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(claims.get("iat", Long.class)), ZoneOffset.UTC);
    LocalDateTime expiresAt = LocalDateTime.ofInstant(
        Instant.ofEpochSecond(claims.get("exp", Long.class)), ZoneOffset.UTC);

    UUID id = UUID.fromString(claims.get("nonce", String.class));
    Optional<CredentialMetadata> credentialMetadataCached = issuedResourceService.getFromCache(id);

    if (credentialMetadataCached.isPresent()) {
      credentialMetadataCached.get().setCredentialId(credentialId);
      credentialMetadataCached.get().setTraineeId(traineeTisId);
      credentialMetadataCached.get().setIssuedAt(issuedAt);
      credentialMetadataCached.get().setExpiresAt(expiresAt);
      return credentialMetadataCached.get();
    } else {
      throw new IllegalStateException("Credential not in cache");
    }
  }
}
