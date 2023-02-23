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

package uk.nhs.hee.tis.trainee.credentials.service;

import io.jsonwebtoken.Claims;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;

/**
 * A service providing credential issuance functionality.
 */
@Service
public class IssuanceService {

  private final GatewayService gatewayService;
  private final JwtService jwtService;
  private final CachingDelegate cachingDelegate;

  /**
   * Create a service providing credential issuance functionality.
   *
   * @param gatewayService  The service to handle all credential gateway interactions.
   * @param jwtService      The JWT service to use.
   * @param cachingDelegate The caching delegate for caching data between requests.
   */
  IssuanceService(GatewayService gatewayService, JwtService jwtService,
      CachingDelegate cachingDelegate) {
    this.gatewayService = gatewayService;
    this.jwtService = jwtService;
    this.cachingDelegate = cachingDelegate;
  }

  /**
   * Start the issuance of a credential.
   *
   * @param authToken   The request's authorization token.
   * @param dto         The user's identity data.
   * @param clientState The state sent by client when making the request to start verification.
   * @return The URI to continue the issuing via credential gateway, or empty if issuing failed.
   */
  public Optional<URI> startCredentialIssuance(String authToken, CredentialDto dto,
      @Nullable String clientState) {
    // Cache the provided identity data against the nonce.
    UUID nonce = UUID.randomUUID();
    cachingDelegate.cacheCredentialData(nonce, dto);

    // Generate new state for the internal request/callback and cache the client state.
    UUID internalState = UUID.randomUUID();
    cachingDelegate.cacheClientState(internalState, clientState);

    // Cache an ID from the token to represent the session.
    Claims authClaims = jwtService.getClaims(authToken);
    String traineeId = authClaims.get("custom:tisId", String.class);
    cachingDelegate.cacheTraineeIdentifier(internalState, traineeId);

    return gatewayService.getCredentialUri(dto, nonce.toString(), internalState.toString());
  }
}
