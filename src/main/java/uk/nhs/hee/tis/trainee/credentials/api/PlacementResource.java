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

import com.amazonaws.xray.spring.aop.XRayEnabled;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.trainee.credentials.api.util.AuthTokenUtil;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialMetadataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialType;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.service.CredentialMetadataService;

/**
 * API endpoints for trainee Placement credentials.
 */
@Slf4j
@RestController
@RequestMapping("/api/placement")
@XRayEnabled
public class PlacementResource {

  private final CredentialMetadataService service;
  private final CredentialMetadataMapper mapper;

  PlacementResource(CredentialMetadataService service, CredentialMetadataMapper mapper) {
    this.service = service;
    this.mapper = mapper;
  }

  @GetMapping
  ResponseEntity<List<CredentialMetadataDto>> getLatestPlacement(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

    String tisId;
    try {
      tisId = AuthTokenUtil.getTraineeTisId(token);
    } catch (IOException e) {
      log.warn("Unable to read tisId from token.", e);
      return ResponseEntity.badRequest().build();
    }

    log.info("Getting latest Training Placement credentials for trainee {}", tisId);
    List<CredentialMetadata> latestCredential =
        service.getLatestCredentialsForType(CredentialType.TRAINING_PLACEMENT, tisId);

    log.info("Credential retrieved.");
    return ResponseEntity.ok(mapper.toDtos(latestCredential));
  }
}
