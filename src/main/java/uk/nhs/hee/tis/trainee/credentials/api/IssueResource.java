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

import java.net.URI;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto;
import uk.nhs.hee.tis.trainee.credentials.dto.TestCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService;

/**
 * API endpoints for issuing trainee digital credentials.
 */
@Slf4j
@RestController()
@RequestMapping("/api/issue")
public class IssueResource {

  private final GatewayService service;

  IssueResource(GatewayService service) {
    this.service = service;
  }

  @PostMapping("/programme-membership")
  ResponseEntity<String> issueProgrammeMembershipCredential(@RequestBody ProgrammeMembershipDto dto,
      @RequestParam(required = false) String state) {
    log.info("Received request to issue Programme Membership credential.");
    Optional<URI> credentialUri = service.getCredentialUri(dto, state);

    if (credentialUri.isPresent()) {
      URI uri = credentialUri.get();
      log.info("Programme Membership credential successfully issued.");
      return ResponseEntity.created(uri).body(uri.toString());
    } else {
      log.error("Could not issue Programme Membership credential.");
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/test")
  ResponseEntity<String> issueTestCredential(@RequestBody TestCredentialDto dto,
      @RequestParam(required = false) String state) {
    log.info("Received request to issue Test credential.");
    Optional<URI> credentialUri = service.getCredentialUri(dto, state);

    if (credentialUri.isPresent()) {
      URI uri = credentialUri.get();
      log.info("Test credential successfully issued.");
      return ResponseEntity.created(uri).body(uri.toString());
    } else {
      log.error("Could not issue Test credential.");
      return ResponseEntity.internalServerError().build();
    }
  }
}
