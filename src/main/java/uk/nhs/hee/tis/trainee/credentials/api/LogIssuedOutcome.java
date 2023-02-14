package uk.nhs.hee.tis.trainee.credentials.api;

import io.jsonwebtoken.Claims;
import java.io.IOException;
import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService;
import uk.nhs.hee.tis.trainee.credentials.service.IssuedResourceService;

@Slf4j
@RestController()
@RequestMapping("/api/issue")
public class LogIssuedOutcome {

  private final GatewayService gatewayService;
  private final IssuedResourceService issuedResourceService;
  private final CredentialMetadataMapper mapper;


  LogIssuedOutcome(GatewayService service, IssuedResourceService issuedResourceService,
                   CredentialMetadataMapper mapper) {
    this.gatewayService = service;
    this.issuedResourceService = issuedResourceService;
    this.mapper = mapper;
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
      Claims claims = gatewayService.getIssuedTokenClaims(code, state);
      try {
        credentialMetadata = mapper.toCredentialMetadata(claims, token);
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
