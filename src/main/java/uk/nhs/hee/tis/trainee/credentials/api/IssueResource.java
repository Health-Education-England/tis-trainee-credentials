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
import lombok.extern.slf4j.Slf4j;
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
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementDataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDataDto;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialDataMapper;
import uk.nhs.hee.tis.trainee.credentials.service.IssuanceService;

/**
 * API endpoints for issuing trainee digital credentials.
 */
@Slf4j
@RestController()
@RequestMapping("/api/issue")
public class IssueResource {

  private final IssuanceService issuanceService;
  private final CredentialDataMapper mapper;

  IssueResource(IssuanceService issuanceService, CredentialDataMapper mapper) {
    this.issuanceService = issuanceService;
    this.mapper = mapper;
  }

  @PostMapping("/programme-membership")
  ResponseEntity<String> issueProgrammeMembershipCredential(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
      @Validated @RequestBody ProgrammeMembershipDataDto dataDto,
      @RequestParam(required = false) String state) {
    log.info("Received request to issue Programme Membership credential.");
    ProgrammeMembershipCredentialDto credentialDto = mapper.toCredential(dataDto);
    URI uri = issuanceService.startCredentialIssuance(token, credentialDto, state);
    return ResponseEntity.created(uri).body(uri.toString());
  }

  @PostMapping("/placement")
  ResponseEntity<String> issuePlacementCredential(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
      @Validated @RequestBody PlacementDataDto dataDto,
      @RequestParam(required = false) String state) {
    log.info("Received request to issue Placement credential.");
    PlacementCredentialDto credentialDto = mapper.toCredential(dataDto);
    URI uri = issuanceService.startCredentialIssuance(token, credentialDto, state);
    return ResponseEntity.created(uri).body(uri.toString());
  }

  /**
   * A callback to log the outcome of the issued resource, and redirect to the redirect_uri.
   *
   * @param code  The code returned from the gateway.
   * @param state The internal state returned from the gateway.
   * @return The response entity redirecting to the issuing redirect_uri.
   */
  @GetMapping("/callback")
  ResponseEntity<String> logIssuedResource(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state) {

    log.info("Receiving callback for credential issuing.");

    URI redirectUri = issuanceService.completeCredentialIssuance(code, state);

    log.info("Redirecting after credential issuing process.");
    return ResponseEntity.status(HttpStatus.FOUND).location(redirectUri).build();
  }
}
