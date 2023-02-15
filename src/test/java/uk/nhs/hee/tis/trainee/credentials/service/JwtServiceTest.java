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
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties.TokenProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.CredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.PlacementCredentialDto;
import uk.nhs.hee.tis.trainee.credentials.dto.ProgrammeMembershipCredentialDto;

class JwtServiceTest {

  private static final String PROGRAMME_NAME = "Programme One";
  private static final LocalDate START_DATE = LocalDate.now().minusYears(1);
  private static final LocalDate END_DATE = LocalDate.now().plusYears(1);

  private static final String PLACEMENT_SPECIALTY = "placement specialty";
  private static final String PLACEMENT_GRADE = "placement grade";
  private static final String PLACEMENT_NATIONAL_POST_NUMBER = "placement NPN";
  private static final String PLACEMENT_EMPLOYING_BODY = "placement employing body";
  private static final String PLACEMENT_SITE = "placement site";

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
    record EmptyData() implements CredentialDto {

      @Override
      public Instant getExpiration(Instant issuedAt) {
        return issuedAt.plus(Duration.ofDays(30));
      }

      @Override
      public String getScope() {
        return "issue.Empty";
      }
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

    Date expectedExpiration = Date.from(issuedAt.toInstant().plus(Duration.ofDays(30)));
    assertThat("Unexpected token exp.", tokenClaims.getExpiration(), is(expectedExpiration));
  }

  @Test
  void shouldGenerateTokenWithProgrammeMembershipClaims() {
    var dto = new ProgrammeMembershipCredentialDto(PROGRAMME_NAME, START_DATE, END_DATE);

    String tokenString = service.generateToken(dto);

    Jwt<?, Claims> token = parser.parse(tokenString);
    Claims tokenClaims = token.getBody();

    assertThat("Unexpected number of claims.", tokenClaims.size(), is(DEFAULT_CLAIM_COUNT + 3));
    assertThat("Unexpected programme name.", tokenClaims.get("TPR-Name"), is(PROGRAMME_NAME));
    assertThat("Unexpected programme start date.", tokenClaims.get("TPR-ProgrammeStartDate"),
        is(START_DATE.toString()));
    assertThat("Unexpected programme end date.", tokenClaims.get("TPR-ProgrammeEndDate"),
        is(END_DATE.toString()));

    Instant issuedAt = tokenClaims.getIssuedAt().toInstant();
    Instant expectedExpiration = dto.getExpiration(issuedAt).truncatedTo(ChronoUnit.SECONDS);
    Instant expiration = tokenClaims.getExpiration().toInstant();
    assertThat("Unexpected token exp.", expiration, is(expectedExpiration));
  }

  @Test
  void shouldGenerateTokenWithPlacementClaims() {
    var dto = new PlacementCredentialDto(
        PLACEMENT_SPECIALTY, PLACEMENT_GRADE, PLACEMENT_NATIONAL_POST_NUMBER,
        PLACEMENT_EMPLOYING_BODY, PLACEMENT_SITE, START_DATE, END_DATE);

    String tokenString = service.generateToken(dto);

    Jwt<?, Claims> token = parser.parse(tokenString);
    Claims tokenClaims = token.getBody();

    assertThat("Unexpected number of claims.", tokenClaims.size(), is(DEFAULT_CLAIM_COUNT + 7));
    assertThat("Unexpected placement specialty.", tokenClaims.get("TPL-Specialty"),
        is(PLACEMENT_SPECIALTY));
    assertThat("Unexpected placement grade.", tokenClaims.get("TPL-Grade"),
        is(PLACEMENT_GRADE));
    assertThat("Unexpected placement NPN.", tokenClaims.get("TPL-PlacementNPN"),
        is(PLACEMENT_NATIONAL_POST_NUMBER));
    assertThat("Unexpected placement employing body.",
        tokenClaims.get("TPL-EmployingBodyOfPost"),
        is(PLACEMENT_EMPLOYING_BODY));
    assertThat("Unexpected placement site.", tokenClaims.get("TPL-Site"),
        is(PLACEMENT_SITE));
    assertThat("Unexpected placement start date.", tokenClaims.get("TPL-PlacementStartDate"),
        is(START_DATE.toString()));
    assertThat("Unexpected placement end date.", tokenClaims.get("TPL-PlacementEndDate"),
        is(END_DATE.toString()));

    Instant issuedAt = tokenClaims.getIssuedAt().toInstant();
    Instant expectedExpiration = dto.getExpiration(issuedAt).truncatedTo(ChronoUnit.SECONDS);
    Instant expiration = tokenClaims.getExpiration().toInstant();
    assertThat("Unexpected token exp.", expiration, is(expectedExpiration));
  }

  @Test
  void shouldGetClaimsFromSignedToken() throws NoSuchAlgorithmException {
    byte[] keyBytes = new byte[32];
    SecureRandom.getInstanceStrong().nextBytes(keyBytes);
    SecretKey key = Keys.hmacShaKeyFor(keyBytes);

    String token = Jwts.builder().signWith(key)
        .claim("claim1", "value1")
        .claim("claim2", "value2")
        .compact();

    Claims claims = service.getClaims(token);
    assertThat("Unexpected claim count.", claims.size(), is(2));
    assertThat("Unexpected claim value.", claims.get("claim1"), is("value1"));
    assertThat("Unexpected claim value.", claims.get("claim2"), is("value2"));
  }

  @Test
  void shouldGetClaimsFromUnsignedToken() {
    String token = Jwts.builder()
        .claim("claim1", "value1")
        .claim("claim2", "value2")
        .compact();

    Claims claims = service.getClaims(token);
    assertThat("Unexpected claim count.", claims.size(), is(2));
    assertThat("Unexpected claim value.", claims.get("claim1"), is("value1"));
    assertThat("Unexpected claim value.", claims.get("claim2"), is("value2"));
  }
}
