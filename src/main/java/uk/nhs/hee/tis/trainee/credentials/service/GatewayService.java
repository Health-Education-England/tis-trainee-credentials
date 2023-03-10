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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;

/**
 * A service providing credential gateway functionality.
 */
@Slf4j
@Service
public class GatewayService {

  private final RestTemplate restTemplate;
  private final JwtService jwtService;
  private final GatewayProperties properties;

  /**
   * Create a service providing credential gateway functionality.
   *
   * @param restTemplate The rest template for sending requests to the gateway.
   * @param jwtService   The service to use to build JWT.
   * @param properties   The gateway application configuration.
   */
  GatewayService(RestTemplate restTemplate, JwtService jwtService, GatewayProperties properties) {
    this.restTemplate = restTemplate;
    this.jwtService = jwtService;
    this.properties = properties;
  }

  /**
   * Get a URI to be used to issue a credential for the given credential data.
   *
   * @param dto   The credential data to be issued.
   * @param nonce The nonce.
   * @param state The state.
   * @return The URI to issue the credential.
   */
  public Optional<URI> getCredentialUri(CredentialDto dto, String nonce, String state) {
    HttpEntity<MultiValueMap<String, String>> request = buildParRequest(dto, nonce, state);

    log.info("Sending PAR request.");
    ResponseEntity<ParResponse> response = restTemplate.postForEntity(
        properties.issuing().parEndpoint(), request, ParResponse.class);
    log.info("Received PAR response.");

    return buildCredentialUri(response);
  }

  /**
   * Build a request to send to the PAR endpoint.
   *
   * @param dto   The dto to build a credential for.
   * @param state The state.
   * @return The built request.
   */
  private HttpEntity<MultiValueMap<String, String>> buildParRequest(CredentialDto dto, String nonce,
      String state) {
    log.info("Building PAR request.");
    String idTokenHint = jwtService.generateToken(dto);

    MultiValueMap<String, String> bodyPair = new LinkedMultiValueMap<>();
    bodyPair.add("client_id", properties.clientId());
    bodyPair.add("client_secret", properties.clientSecret());
    bodyPair.add("redirect_uri", properties.issuing().redirectUri());
    bodyPair.add("scope", dto.getScope());
    bodyPair.add("id_token_hint", idTokenHint);
    bodyPair.add("nonce", nonce);
    bodyPair.add("state", state);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    log.info("Built PAR request.");
    return new HttpEntity<>(bodyPair, headers);
  }

  /**
   * Build a credential URI from the response of a PAR request.
   *
   * @param response The response to process.
   * @return The credential URI, or empty optional if the response was invalid.
   */
  private Optional<URI> buildCredentialUri(ResponseEntity<ParResponse> response) {
    log.info("Building credential URI.");
    URI credentialUri = null;

    if (response.getStatusCode().equals(HttpStatus.CREATED)) {
      ParResponse parResponse = response.getBody();

      if (parResponse != null) {
        credentialUri = UriComponentsBuilder.fromUriString(properties.issuing().authorizeEndpoint())
            .queryParam("request_uri", parResponse.requestUri())
            .build()
            .toUri();
        log.info("Built credential URI.");
      } else {
        log.error("PAR response was empty.");
      }
    } else {
      log.error("PAR request unsuccessful, status code {}.", response.getStatusCode());
    }

    return Optional.ofNullable(credentialUri);
  }

  /**
   * A record representing a successful response to a PAR request.
   *
   * @param requestUri The returned request_uri value.
   */
  record ParResponse(@JsonProperty("request_uri") String requestUri) {

  }

  /**
   * Call a token code endpoint and extract the claims from the resulting token.
   *
   * @param endpoint     The token endpoint.
   * @param redirectUri  The redirect endpoint sent with the initial request.
   * @param code         The code resulting from the initial request.
   * @param codeVerifier The code PKCE code verifier matching the challenge sent with the initial
   *                     request.
   * @return The extracted claims.
   */
  public Claims getTokenClaims(URI endpoint, URI redirectUri, String code, String codeVerifier) {
    String state = UUID.randomUUID().toString();
    HttpEntity<MultiValueMap<String, String>> request = buildTokenRequest(redirectUri, code,
        codeVerifier, state);

    log.info("Sending token request.");
    ResponseEntity<TokenResponse> tokenResponse = restTemplate.postForEntity(endpoint, request,
        TokenResponse.class);

    if (tokenResponse.getStatusCode().isError()) {
      log.error("Token request failed with code {}.", tokenResponse.getStatusCode());
      return new DefaultClaims();
    }
    log.info("Received token response.");

    TokenResponse body = tokenResponse.getBody();

    if (body == null) {
      log.error("Token response was empty.");
      return new DefaultClaims();
    }

    String signedToken = body.idToken();
    return jwtService.getClaims(signedToken, true);
  }

  /**
   * Build a request to get a token for the given code.
   *
   * @param redirectUri  The redirect endpoint sent with the initial request.
   * @param code         The code resulting from the initial request.
   * @param codeVerifier The code PKCE code verifier matching the challenge sent with the initial
   *                     request.
   * @param state        The state to include in the request.
   * @return The built token request.
   */
  private HttpEntity<MultiValueMap<String, String>> buildTokenRequest(URI redirectUri, String code,
      String codeVerifier, String state) {
    log.info("Building Token request.");

    MultiValueMap<String, String> bodyPair = new LinkedMultiValueMap<>();
    bodyPair.add("client_id", properties.clientId());
    bodyPair.add("client_secret", properties.clientSecret());
    bodyPair.add("redirect_uri", redirectUri.toString());
    bodyPair.add("grant_type", "authorization_code");
    bodyPair.add("code", code);
    bodyPair.add("code_verifier", codeVerifier);
    bodyPair.add("state", state);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    log.info("Built Token request.");
    return new HttpEntity<>(bodyPair, headers);
  }

  record TokenResponse(@JsonProperty("id_token") String idToken) {

  }
}
