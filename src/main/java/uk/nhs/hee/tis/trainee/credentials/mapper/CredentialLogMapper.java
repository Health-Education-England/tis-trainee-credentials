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
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialLogDto;
import uk.nhs.hee.tis.trainee.credentials.service.IssuedResourceService;

/**
 * A mapper to map between Claims data and Credential Log Data.
 */

public class CredentialLogMapper {

  private final IssuedResourceService issuedResourceService;
  private final ObjectMapper objectMapper;
  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  CredentialLogMapper(IssuedResourceService issuedResourceService, ObjectMapper objectMapper) {
    this.issuedResourceService = issuedResourceService;
    this.objectMapper = objectMapper;
  }

  /**
   * Map a claim to the equivalent credential log DTO, and supplement with cached details.
   *
   * @param claims    The Claims data to map.
   * @param authToken The user's authorization token.
   * @return The mapped credential log entry.
   */

  public CredentialLogDto toCredentialLogDto(Claims claims, String authToken) throws IOException {

    String[] tokenSections = authToken.split("\\.");
    byte[] payloadBytes = Base64.getUrlDecoder()
        .decode(tokenSections[1].getBytes(StandardCharsets.UTF_8));
    Map<?, ?> payload = objectMapper.readValue(payloadBytes, Map.class);
    String traineeTisId = (String) payload.get(TIS_ID_ATTRIBUTE);

    String credentialId = claims.get("SerialNumber", String.class);
    LocalDateTime issuedAt = LocalDateTime.from(Instant.ofEpochSecond(
        Long.parseLong(claims.get("iat", String.class))));
    LocalDateTime expiresAt = LocalDateTime.from(Instant.ofEpochSecond(
        Long.parseLong(claims.get("exp", String.class))));

    UUID id = UUID.fromString(claims.get("nonce", String.class));
    Optional<CredentialLogDto> credentialLogDtoCached = issuedResourceService.getFromCache(id);

    // TODO: is this valid? Or rather throw error?
    return credentialLogDtoCached.map(credentialLogDto -> new CredentialLogDto(credentialId,
        traineeTisId,
        credentialLogDto.credentialType(),
        credentialLogDto.tisId(),
        issuedAt,
        expiresAt)).orElseGet(() -> new CredentialLogDto(credentialId,
        traineeTisId,
        null,
        null,
        issuedAt,
        expiresAt));
  }
}
