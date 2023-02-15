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

import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
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
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService;
import uk.nhs.hee.tis.trainee.credentials.service.IssuedResourceService;

/**
 * API endpoints for issuing trainee digital credentials.
 */
@Slf4j
@RestController()
@RequestMapping("/api/issue")
public class IssueResource {

  private final GatewayService service;
  private final CredentialDataMapper mapper;
  private final IssuedResourceService issuedResourceService;
  private final CredentialMetadataMapper credentialMetadataMapper;

  IssueResource(GatewayService service, CredentialDataMapper mapper,
                IssuedResourceService issuedResourceService,
                CredentialMetadataMapper credentialMetadataMapper) {
    this.service = service;
    this.mapper = mapper;
    this.issuedResourceService = issuedResourceService;
    this.credentialMetadataMapper = credentialMetadataMapper;
  }

  @PostMapping("/programme-membership")
  ResponseEntity<String> issueProgrammeMembershipCredential(
      @Validated @RequestBody ProgrammeMembershipDataDto dataDto,
      @RequestParam(required = false) String state) {
    log.info("Received request to issue Programme Membership credential.");
    ProgrammeMembershipCredentialDto credentialDto = mapper.toCredential(dataDto);
    Optional<URI> credentialUri = service.getCredentialUri(credentialDto, state);

    if (credentialUri.isPresent()) {
      URI uri = credentialUri.get();
      log.info("Programme Membership credential successfully issued.");
      return ResponseEntity.created(uri).body(uri.toString());
    } else {
      log.error("Could not issue Programme Membership credential.");
      return ResponseEntity.internalServerError().build();
    }
  }

  @PostMapping("/placement")
  ResponseEntity<String> issuePlacementCredential(
      @Validated @RequestBody PlacementDataDto dataDto,
      @RequestParam(required = false) String state) {
    log.info("Received request to issue Placement credential.");
    PlacementCredentialDto credentialDto = mapper.toCredential(dataDto);
    Optional<URI> credentialUri = service.getCredentialUri(credentialDto, state);

    if (credentialUri.isPresent()) {
      URI uri = credentialUri.get();
      log.info("Placement credential successfully issued.");
      return ResponseEntity.created(uri).body(uri.toString());
    } else {
      log.error("Could not issue Placement credential.");
      return ResponseEntity.internalServerError().build();
    }
  }

  /**
   * Used as a callback from PAR to log the outcome of the issued resource, and then to
   * redirect to the redirect_uri.
   *
   * @param code             The code returned from the gateway.
   * @param state            The internal state returned from the gateway.
   * @param error            The error text, if the credential was not issued.
   * @param errorDescription The error description, if the credential was not issued.
   * @return The response entity redirecting to the issuing redirect_uri.
   */
  @GetMapping("/callback")
  ResponseEntity<String> logIssuedResource(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) String state,
      @RequestParam(required = false) String error,
      @RequestParam(required = false, value = "error_description") String errorDescription,
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token) {

    CredentialMetadata credentialMetadata = null;
    if (error == null) {
      log.info("Credential was issued successfully.");
      Claims claims = service.getIssuedTokenClaims(code, state);
      try {
        credentialMetadata = credentialMetadataMapper.toCredentialMetadata(claims, token);
      } catch (IOException e) {
        log.warn("Unable to read trainee tisId from token.", e);
        // TODO - allow to continue? or return ResponseEntity.badRequest().build();
      }
    } else {
      log.info("Credential was not issued.");
    }
    URI redirectUri = issuedResourceService.logIssuedResource(credentialMetadata, code, state,
        error, errorDescription);

    log.info("Redirecting after credential issuing process.");
    return ResponseEntity.status(HttpStatus.OK).location(redirectUri).build();
  }
}
