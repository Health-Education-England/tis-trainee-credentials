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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.DefaultClaims;
import io.jsonwebtoken.impl.DefaultJwsHeader;
import io.jsonwebtoken.security.Keys;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.IssuingProperties.TokenProperties;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties.VerificationProperties;
import uk.nhs.hee.tis.trainee.credentials.service.PublicKeyResolver.Jwks;
import uk.nhs.hee.tis.trainee.credentials.service.PublicKeyResolver.Jwks.Jwk;

class PublicKeyResolverTest {

  private static final String HOST = "https://credential.gateway";
  private static final String HOST_ISSUER = "https://credential.gateway/oidc";
  private static final String ISSUING_TOKEN_AUDIENCE = "https://credential.gateway/issuer";
  private static final String JWKS_ENDPOINT = "https://credential.gateway/.well-known/openid-configuration/jwks";

  private static final String KEY_ID = "key-id";

  private PublicKeyResolver resolver;
  private CachingDelegate cachingDelegate;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    cachingDelegate = spy(CachingDelegate.class);
    restTemplate = mock(RestTemplate.class);

    TokenProperties tokenProperties = new TokenProperties(ISSUING_TOKEN_AUDIENCE, "", "");
    IssuingProperties issuingProperties = new IssuingProperties("", "", "", tokenProperties, "");
    VerificationProperties verificationProperties
        = new VerificationProperties(HOST_ISSUER, "", "", "");
    GatewayProperties gatewayProperties = new GatewayProperties(HOST, "", "", "", JWKS_ENDPOINT,
        issuingProperties, verificationProperties, null);
    resolver = new PublicKeyResolver(cachingDelegate, restTemplate, gatewayProperties);
  }

  @Test
  void shouldThrowExceptionWhenNoKeyId() {
    DefaultJwsHeader header = new DefaultJwsHeader();
    Claims claims = new DefaultClaims().setIssuer(HOST);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldReturnCachedValueWhenPublicKeyCached() {
    PublicKey cachedKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    when(cachingDelegate.getPublicKey(KEY_ID)).thenReturn(Optional.of(cachedKey));

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    Key key = resolver.resolveSigningKey(header, claims);

    assertThat("Unexpected resolved signing key.", key, is(cachedKey));
    verifyNoInteractions(restTemplate);
  }

  @ParameterizedTest
  @ValueSource(strings = {HOST_ISSUER, ISSUING_TOKEN_AUDIENCE})
  void shouldGetWellKnownPublicKeyWhenPublicKeyNotCached(String tokenIssuer) {
    KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    String modulus = Base64.getUrlEncoder().encodeToString(publicKey.getModulus().toByteArray());
    String exponent = Base64.getUrlEncoder()
        .encodeToString(publicKey.getPublicExponent().toByteArray());

    Jwk jwk = new Jwk();
    jwk.setKeyId(KEY_ID);
    jwk.setModulus(modulus);
    jwk.setExponent(exponent);
    Jwks jwks = new Jwks();
    jwks.setKeys(new Jwk[]{jwk});
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(jwks);

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(tokenIssuer);

    Key resolvedKey = resolver.resolveSigningKey(header, claims);
    assertThat("Unexpected resolved signing key.", resolvedKey, is(publicKey));
  }

  @ParameterizedTest
  @ValueSource(strings = {HOST_ISSUER, ISSUING_TOKEN_AUDIENCE})
  void shouldCachePublicKeyWhenRetrievedFromWellKnown(String tokenIssuer) {
    KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
    RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
    String modulus = Base64.getUrlEncoder().encodeToString(publicKey.getModulus().toByteArray());
    String exponent = Base64.getUrlEncoder()
        .encodeToString(publicKey.getPublicExponent().toByteArray());

    Jwk jwk = new Jwk();
    jwk.setKeyId(KEY_ID);
    jwk.setModulus(modulus);
    jwk.setExponent(exponent);
    Jwks jwks = new Jwks();
    jwks.setKeys(new Jwk[]{jwk});
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(jwks);

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(tokenIssuer);

    resolver.resolveSigningKey(header, claims);
    verify(cachingDelegate).cachePublicKey(KEY_ID, publicKey);
  }

  @Test
  void shouldRemoveAlgorithmFromKeyIdWhenAlgorithmIncluded() {
    PublicKey cachedKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    when(cachingDelegate.getPublicKey(KEY_ID)).thenReturn(Optional.of(cachedKey));

    String algorithm = "an-algorithm";
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.ALGORITHM, algorithm,
        JwsHeader.KEY_ID, KEY_ID + algorithm));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    Key key = resolver.resolveSigningKey(header, claims);

    assertThat("Unexpected resolved signing key.", key, is(cachedKey));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "https://not.credential.gateway")
  void shouldThrowExceptionWhenIssuerNotMatchesHost(String issuer) {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(issuer);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsNull() {
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(null);

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsNoKeys() {
    Jwks jwks = new Jwks();
    jwks.setKeys(new Jwk[0]);
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(jwks);

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsNoMatchingKeys() {
    Jwk jwk = new Jwk();
    jwk.setKeyId("not-key-id");
    Jwks jwks = new Jwks();
    jwks.setKeys(new Jwk[]{jwk});
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(jwks);

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsInvalidKey() {
    String modulus = Base64.getUrlEncoder().encodeToString(BigInteger.ONE.toByteArray());
    String exponent = Base64.getUrlEncoder().encodeToString(BigInteger.TWO.toByteArray());

    Jwk jwk = new Jwk();
    jwk.setKeyId(KEY_ID);
    jwk.setModulus(modulus);
    jwk.setExponent(exponent);
    Jwks jwks = new Jwks();
    jwks.setKeys(new Jwk[]{jwk});
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(jwks);

    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(JwsHeader.KEY_ID, KEY_ID));
    Claims claims = new DefaultClaims().setIssuer(HOST_ISSUER);

    Throwable cause = assertThrows(IllegalArgumentException.class,
        () -> resolver.resolveSigningKey(header, claims)).getCause();
    assertThat("Unexpected exception cause.", cause,
        CoreMatchers.instanceOf(InvalidKeySpecException.class));
  }
}
