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
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
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
import org.testcontainers.shaded.org.bouncycastle.asn1.ASN1Sequence;
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.X500Name;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.testcontainers.shaded.org.bouncycastle.cert.X509CertificateHolder;
import org.testcontainers.shaded.org.bouncycastle.cert.X509v1CertificateBuilder;
import org.testcontainers.shaded.org.bouncycastle.operator.ContentSigner;
import org.testcontainers.shaded.org.bouncycastle.operator.OperatorCreationException;
import org.testcontainers.shaded.org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.service.PublicKeyResolver.Jwks;
import uk.nhs.hee.tis.trainee.credentials.service.PublicKeyResolver.Jwks.Jwk;

class PublicKeyResolverTest {

  private static final String HOST = "https://credential.gateway";
  private static final String JWKS_ENDPOINT = "https://credential.gateway/.well-known/openid-configuration/jwks";

  private static final String CERTIFICATE_THUMBPRINT = "cert-thumb";

  private PublicKeyResolver resolver;
  private CachingDelegate cachingDelegate;
  private RestTemplate restTemplate;

  @BeforeEach
  void setUp() {
    cachingDelegate = spy(CachingDelegate.class);
    restTemplate = mock(RestTemplate.class);

    GatewayProperties properties = new GatewayProperties(HOST, "", "", JWKS_ENDPOINT, null, null);
    resolver = new PublicKeyResolver(cachingDelegate, restTemplate, properties);
  }

  @Test
  void shouldThrowExceptionWhenNoCertificateThumbprint() {
    DefaultJwsHeader header = new DefaultJwsHeader();
    Claims claims = new DefaultClaims().setIssuer(HOST);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldReturnCachedValueWhenPublicKeyCached() {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    PublicKey cachedKey = Keys.keyPairFor(SignatureAlgorithm.RS256).getPublic();
    when(cachingDelegate.getPublicKey(CERTIFICATE_THUMBPRINT)).thenReturn(Optional.of(cachedKey));

    Key key = resolver.resolveSigningKey(header, claims);

    assertThat("Unexpected resolved signing key.", key, is(cachedKey));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldGetWellKnownPublicKeyWhenPublicKeyNotCached()
      throws IOException, OperatorCreationException {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
    String x5c = generateValidX5c(keyPair, SignatureAlgorithm.RS256);
    Jwk jwk = new Jwk("", "", "", "", new String[]{x5c}, CERTIFICATE_THUMBPRINT, "", "");
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(new Jwks(new Jwk[]{jwk}));

    Key resolvedKey = resolver.resolveSigningKey(header, claims);
    assertThat("Unexpected resolved signing key.", resolvedKey, is(keyPair.getPublic()));
  }

  @Test
  void shouldCachePublicKeyWhenRetrievedFromWellKnown()
      throws IOException, OperatorCreationException {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
    String x5c = generateValidX5c(keyPair, SignatureAlgorithm.RS256);
    Jwk jwk = new Jwk("", "", "", "", new String[]{x5c}, CERTIFICATE_THUMBPRINT, "", "");
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(new Jwks(new Jwk[]{jwk}));

    resolver.resolveSigningKey(header, claims);
    verify(cachingDelegate).cachePublicKey(CERTIFICATE_THUMBPRINT, keyPair.getPublic());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = "https://not.credential.gateway")
  void shouldThrowExceptionWhenIssuerNotMatchesHost(String issuer) {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(issuer);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
    verifyNoInteractions(restTemplate);
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsNull() {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(null);

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsNoKeys() {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(new Jwks(new Jwk[0]));

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsNoMatchingKeys() {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    Jwk jwk = new Jwk("", "", "", "", null, "not-cert-thumb", "", "");
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(new Jwks(new Jwk[]{jwk}));

    assertThrows(IllegalArgumentException.class, () -> resolver.resolveSigningKey(header, claims));
  }

  @Test
  void shouldThrowExceptionWhenJwksEndpointReturnsInvalidCertificate() {
    DefaultJwsHeader header = new DefaultJwsHeader(Map.of(
        JwsHeader.X509_CERT_SHA1_THUMBPRINT, CERTIFICATE_THUMBPRINT));
    Claims claims = new DefaultClaims().setIssuer(HOST);

    String x5c = Base64.getEncoder()
        .encodeToString("invalid-certificate".getBytes(StandardCharsets.UTF_8));
    Jwk jwk = new Jwk("", "", "", "", new String[]{x5c}, CERTIFICATE_THUMBPRINT, "", "");
    when(restTemplate.getForObject(JWKS_ENDPOINT, Jwks.class)).thenReturn(new Jwks(new Jwk[]{jwk}));

    Throwable cause = assertThrows(IllegalArgumentException.class,
        () -> resolver.resolveSigningKey(header, claims)).getCause();
    assertThat("Unexpected exception cause.", cause,
        CoreMatchers.instanceOf(CertificateException.class));
  }

  /**
   * Generate a valid self-signed certificate to use as a JWKs x5c cert.
   *
   * @param keyPair The key pair to use for generating a x5c certificate.
   * @param alg     The algorithm used to generate the {@link KeyPair}.
   * @return The generated certificate.
   */
  private String generateValidX5c(KeyPair keyPair, SignatureAlgorithm alg)
      throws OperatorCreationException, IOException {
    ASN1Sequence asn1Sequence = ASN1Sequence.getInstance(keyPair.getPublic().getEncoded());
    SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(asn1Sequence);

    ContentSigner sigGen = new JcaContentSignerBuilder(alg.getJcaName())
        .build(keyPair.getPrivate());

    X509v1CertificateBuilder certBuilder = new X509v1CertificateBuilder(
        new X500Name("CN=Test"),
        BigInteger.ONE,
        Date.from(Instant.now()),
        Date.from(Instant.now().plus(Duration.ofHours(1))),
        new X500Name("CN=Test"),
        subjectPublicKeyInfo
    );
    X509CertificateHolder certHolder = certBuilder.build(sigGen);
    return Base64.getEncoder().encodeToString(certHolder.getEncoded());
  }
}
