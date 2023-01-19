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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties.TokenProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDataDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipDto;

class JwtServiceTest {

  private static final String PROGRAMME_NAME = "Programme One";
  private static final LocalDate START_DATE = LocalDate.now().minusYears(1);
  private static final LocalDate END_DATE = LocalDate.now().plusYears(1);

  private static final String AUDIENCE = "https://test.tis.nhs.uk/audience";
  private static final String ISSUER = "some-identifier";

  private static final int DEFAULT_CLAIM_COUNT = 5;

  private JwtService service;
  private JwtParser parser;

  @BeforeEach
  void setUp() throws NoSuchAlgorithmException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    byte[] keyBytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(keyBytes);
    String keyString = Base64.getEncoder().encodeToString(keyBytes);
    SecretKey key = Keys.hmacShaKeyFor(keyBytes);
    parser = Jwts.parserBuilder().setSigningKey(key).build();

    TokenProperties properties = new TokenProperties(AUDIENCE, ISSUER, keyString);
    service = new JwtService(mapper, properties);
  }

  @Test
  void shouldGenerateTokenWithDefaultClaims() {
    record EmptyData() implements CredentialDataDto {

    }

    final Instant now = Instant.now();
    String tokenString = service.generateToken(new EmptyData());

    Jwt<?, Claims> token = parser.parse(tokenString);
    Claims tokenClaims = token.getBody();

    assertThat("Unexpected number of claims.", tokenClaims.size(), is(DEFAULT_CLAIM_COUNT));
    assertThat("Unexpected token aud.", tokenClaims.getAudience(), is(AUDIENCE));
    assertThat("Unexpected token iss.", tokenClaims.getIssuer(), is(ISSUER));

    Date issuedAt = tokenClaims.getIssuedAt();
    long issuedAtDiff = Duration.between(issuedAt.toInstant(), now).toMinutes();
    assertThat("Unexpected difference between now and iat.", issuedAtDiff, is(0L));
    assertThat("Unexpected token nbf.", tokenClaims.getNotBefore(), is(issuedAt));

    Date expectedExpiry = Date.from(
        issuedAt.toInstant().atOffset(ZoneOffset.UTC).plusYears(1).toInstant());
    assertThat("Unexpected token exp.", tokenClaims.getExpiration(), is(expectedExpiry));
  }

  @Test
  void shouldGenerateTokenWithProgrammeMembershipClaims() {
    ProgrammeMembershipDto dto = new ProgrammeMembershipDto(
        "123", PROGRAMME_NAME, START_DATE, END_DATE);

    String tokenString = service.generateToken(dto);

    Jwt<?, Claims> token = parser.parse(tokenString);
    Claims tokenClaims = token.getBody();

    assertThat("Unexpected number of claims.", tokenClaims.size(), is(DEFAULT_CLAIM_COUNT + 3));
    assertThat("Unexpected claim.", tokenClaims.get("tisId"), nullValue());
    assertThat("Unexpected programme name.", tokenClaims.get("programmeName"), is(PROGRAMME_NAME));
    assertThat("Unexpected programme start date.", tokenClaims.get("startDate"),
        is(START_DATE.toString()));
    assertThat("Unexpected programme end date.", tokenClaims.get("endDate"),
        is(END_DATE.toString()));
  }
}
