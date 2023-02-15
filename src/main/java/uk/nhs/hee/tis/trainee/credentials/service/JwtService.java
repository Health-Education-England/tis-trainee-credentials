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
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

  /**
   * Create a service providing JWT token functionality.
   *
   * @param mapper     The mapper to convert data.
   * @param properties The token properties.
   */
  JwtService(ObjectMapper mapper, TokenProperties properties) {
    this.mapper = mapper;
    this.properties = properties;
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
   * Get the claims from the given token, the signature will not be verified.
   *
   * @param token The token to get claims from.
   * @return The extracted claims.
   */
  public Claims getClaims(String token) {
    JwtParser jwtParser = Jwts.parserBuilder().build();

    // The signing key is not known, strip the signature so the parser can continue.
    token = token.substring(0, token.lastIndexOf(JwtParser.SEPARATOR_CHAR) + 1);
    Jwt<Header, Claims> headerClaimsJwt = jwtParser.parseClaimsJwt(token);

    return headerClaimsJwt.getBody();
  }
}
