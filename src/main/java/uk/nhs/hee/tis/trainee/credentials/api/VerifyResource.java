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

import jakarta.validation.Valid;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;
import uk.nhs.hee.tis.trainee.credentials.service.VerificationService;

/**
 * API endpoints for verifying trainee digital credentials.
 */
@Slf4j
@RestController
@RequestMapping("/api/verify")
@Validated
public class VerifyResource {

  private static final String VERIFY_SCOPE = "openid Identity";

  private final VerificationService service;

  VerifyResource(VerificationService service) {
    this.service = service;
  }

  @PostMapping("/identity")
  ResponseEntity<String> verifyIdentity(@RequestHeader(HttpHeaders.AUTHORIZATION) String authToken,
      @RequestParam(required = false) String state,
      @Valid @RequestBody IdentityDataDto dto) {
    log.info("Received request to start identity verification.");
    URI uri = service.startIdentityVerification(authToken, dto, state);

    log.info("Identity verification successfully started.");
    return ResponseEntity.created(uri).body(uri.toString());
  }

  @GetMapping("/callback")
  ResponseEntity<String> handleVerification(@RequestParam String code,
      @UUID @RequestParam String state) {
    log.info("Received callback for credential verification.");
    URI uri = service.completeCredentialVerification(code, VERIFY_SCOPE, state);

    log.info("Credential verification completed.");
    return ResponseEntity.status(HttpStatus.FOUND).location(uri).build();
  }
}
