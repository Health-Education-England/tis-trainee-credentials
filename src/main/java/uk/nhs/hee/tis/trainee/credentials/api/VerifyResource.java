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
import java.net.URI;
import java.time.LocalDate;
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
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;
import uk.nhs.hee.tis.trainee.credentials.service.CacheService;
import uk.nhs.hee.tis.trainee.credentials.service.GatewayService;

@Slf4j
@RestController
@RequestMapping("/api/verify")
public class VerifyResource {

  private final CacheService cacheService;
  private final GatewayService gatewayService;

  VerifyResource(CacheService cacheService, GatewayService gatewayService) {
    this.cacheService = cacheService;
    this.gatewayService = gatewayService;
  }

  @PostMapping("/identity")
  ResponseEntity<String> verifyIdentity(
      @Validated @RequestBody IdentityDataDto dto,
      @RequestParam(required = false) String state) {

    String cacheKey = cacheService.cacheIdentityData(dto);
    URI identityVerificationUri = gatewayService.getIdentityVerificationUri(cacheKey, state);
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .location(identityVerificationUri)
        .body(identityVerificationUri.toString());
  }

  @GetMapping("/identity/callback")
  ResponseEntity<String> verifyIdentity(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
      @RequestParam String code,
      @RequestParam String state) {
    Claims claims = gatewayService.getVerificationTokenClaims(code, state);
    String nonce = claims.get("nonce", String.class);
    Optional<IdentityDataDto> optionalDto = cacheService.getCachedIdentityData(nonce);

    if (optionalDto.isPresent()) {
      String legalFirstName = claims.get("Identity.ID-LegalFirstName", String.class);
      String legalSurname = claims.get("Identity.ID-LegalSurname", String.class);
      LocalDate birthDate = LocalDate.parse(claims.get("Identity.ID-BirthDate", String.class));

      IdentityDataDto dto = optionalDto.get();

      if (dto.givenName().equals(legalFirstName) && dto.familyName().equals(legalSurname)
          && dto.birthDate().equals(birthDate)) {
        cacheService.cacheVerifiedIdentityJwt(token);
        // TODO: forward back to the app.
//        return ResponseEntity.status(HttpStatus.FOUND)
//            .location(URI.create("http://local.tis-selfservice.com/identity-verified")).build();
        return ResponseEntity.ok("Identity Verified.");
      }
    }

    return ResponseEntity.badRequest().body("Unable to verify identity.");
  }
}
