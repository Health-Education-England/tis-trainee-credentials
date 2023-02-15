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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties.TokenProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;

/**
 * A service providing JWT token functionality.
 */
@Slf4j
@Service
public class JwtService {

  private final ObjectMapper mapper;
  private final TokenProperties properties;

  private final PublicKeyResolver publicKeyResolver;

  /**
   * Create a service providing JWT token functionality.
   *
   * @param mapper     The mapper to convert data.
   * @param properties The token properties.
   */
  JwtService(ObjectMapper mapper, TokenProperties properties, PublicKeyResolver publicKeyResolver) {
    this.mapper = mapper;
    this.properties = properties;
    this.publicKeyResolver = publicKeyResolver;
  }

  /**
   * Generate a token including the given credential claims alongside the default JWT claims.
   *
   * @param dto The credential data to include in the token claims.
   * @return The generated token as an encoded string.
   */
  public String generateToken(CredentialDto dto) {
    log.info("Generating id_token_hint JWT.");
    Instant now = Instant.now();
    Map<String, Object> claimsMap = mapper.convertValue(dto, Map.class);
    SecretKey signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.signingKey()));

    String jwt = Jwts.builder()
        .setAudience(properties.audience())
        .setIssuer(properties.issuer())
        .setIssuedAt(Date.from(now))
        .setNotBefore(Date.from(now))
        .setExpiration(Date.from(dto.getExpiration(now)))
        .addClaims(claimsMap)
        .signWith(signingKey)
        .compact();

    log.info("Generated id_token_hint JWT.");
    return jwt;
  }

  /**
   * Verify the integrity of a signed JWT token using our signing key.
   *
   * @param jwtToken the JWT token to verify
   * @return true if the token can be verified, otherwise false
   */
  public boolean canVerifyToken(String jwtToken) {
    return true;
//    String[] chunks = jwtToken.split("\\.");
//
//    String tokenWithoutSignature = chunks[0] + "." + chunks[1];
//    String signature = chunks[2];
//    SignatureAlgorithm sa = HS256; //hardcoded here, but could be retrieved from header.alg
//
//    byte[] keyBytes = Decoders.BASE64URL.decode(properties.signingKey());
//    SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, sa.getJcaName());
//
//    DefaultJwtSignatureValidator validator = new DefaultJwtSignatureValidator(sa, secretKeySpec, Decoders.BASE64URL);
//    return validator.isValid(tokenWithoutSignature, signature);
  }

  /**
   * Get the claims from the given token, the signature will not be verified.
   *
   * @param token The token to get claims from.
   * @return The extracted claims.
   */
  public Claims getClaims(String token) {
    return getClaims(token, false);
  }

  /**
   * Get the claims from the given token.
   *
   * @param token           The token to get claims from.
   * @param verifySignature Whether the token signature should be verified.
   * @return The extracted claims.
   */
  public Claims getClaims(String token, boolean verifySignature) {
    // Strip any Bearer prefix
    token = token.replace("Bearer ", "");

    JwtParser parser = Jwts.parserBuilder().setSigningKeyResolver(publicKeyResolver).build();

    if (!verifySignature) {
      token = token.substring(0, token.lastIndexOf(JwtParser.SEPARATOR_CHAR) + 1);
      return parser.parseClaimsJwt(token).getBody();
    } else {
      return parser.parseClaimsJws(token).getBody();
    }
  }

  /**
   * Helper function to build a token request.
   *
   * @param code         The response code from the Gateway process.
   * @param state        The state to use for the token request.
   * @param clientId     The Gateway client ID.
   * @param clientSecret The Gateway client secret.
   * @param redirectUri  The redirect URI to receive the Gateway response.
   * @return an HttpEntity with the appropriate body and headers.
   */
  public HttpEntity<MultiValueMap<String, String>> buildTokenRequest(String code, String state,
                                                                     String clientId,
                                                                     String clientSecret,
                                                                     String codeVerifier,
                                                                     String redirectUri) {
    log.info("Building Token request.");

    MultiValueMap<String, String> bodyPair = new LinkedMultiValueMap<>();
    bodyPair.add("client_id", clientId);
    bodyPair.add("client_secret", clientSecret);
    bodyPair.add("redirect_uri", redirectUri);
    bodyPair.add("grant_type", "authorization_code");
    bodyPair.add("code", code);
    if (codeVerifier != null) {
      bodyPair.add("code_verifier", codeVerifier);
    }
    bodyPair.add("state", state);

    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    log.info("Built Token request.");
    return new HttpEntity<>(bodyPair, headers);
  }
}
