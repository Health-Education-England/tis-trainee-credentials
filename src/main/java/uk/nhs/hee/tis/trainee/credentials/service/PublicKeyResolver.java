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
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import java.io.ByteArrayInputStream;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.service.PublicKeyResolver.Jwks.Jwk;

/**
 * A signing key resolver which uses cached public keys before requesting via a JWKs endpoints.
 */
@Component
public class PublicKeyResolver extends SigningKeyResolverAdapter {

  private static final String CERTIFICATE_TYPE = "X.509";

  private final CachingDelegate cachingDelegate;
  private final RestTemplate restTemplate;
  private final GatewayProperties properties;

  PublicKeyResolver(CachingDelegate cachingDelegate, RestTemplate restTemplate,
      GatewayProperties properties) {
    this.cachingDelegate = cachingDelegate;
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @Override
  public Key resolveSigningKey(JwsHeader header, Claims claims) {
    String tokenThumbprint = (String) header.get(JwsHeader.X509_CERT_SHA1_THUMBPRINT);

    if (tokenThumbprint == null) {
      throw new IllegalArgumentException("Not certificate thumbprint in headers.");
    }

    Optional<PublicKey> cachedPublicKey = cachingDelegate.getPublicKey(tokenThumbprint);

    if (cachedPublicKey.isPresent()) {
      return cachedPublicKey.get();
    }

    Jwks jwks = getJwks(claims);

    PublicKey publicKey = Arrays.stream(jwks.keys())
        .filter(key -> key.x5t().equals(tokenThumbprint))
        .map(this::getPublicKey)
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Can not resolve public key for given token."));

    return cachingDelegate.cachePublicKey(tokenThumbprint, publicKey);
  }

  /**
   * Get the JWKs document associated with the claims.
   *
   * @param claims The token claims, issuer must be populated.
   * @return The Jwks document.
   */
  private Jwks getJwks(Claims claims) {
    String issuer = claims.getIssuer();

    if (Objects.equals(issuer, properties.host())) {
      Jwks jwks = restTemplate.getForObject(properties.jwksEndpoint(), Jwks.class);

      if (jwks != null) {
        return jwks;
      }
    }

    String message = String.format("Invalid token issuer '%s'.", issuer);
    throw new IllegalArgumentException(message);
  }

  /**
   * Get the {@link PublicKey} from the given {@link Jwk}s certificate.
   *
   * @param key The JWK to get the public key from.
   * @return The public key.
   */
  private PublicKey getPublicKey(Jwk key) {
    byte[] keyBytes = Base64.getDecoder().decode(key.x5c[0]);

    try {
      CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
      Certificate certificate = certificateFactory.generateCertificate(
          new ByteArrayInputStream(keyBytes));
      return certificate.getPublicKey();
    } catch (CertificateException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * A JWKs document, with an array of JWK keys.
   *
   * @param keys The JWKs
   */
  record Jwks(Jwk[] keys) {

    /**
     * A representation of a JWK.
     *
     * @param kty The key type, identifies the cryptographic algorithm family used with the key.
     * @param use Public key use, identifies the intended use of the public key.
     * @param alg The algorithm, identifies the algorithm intended for use with the key.
     * @param kid The key identifier, used to match a specific key.
     * @param x5c X.509 certificate chain.
     * @param x5t X.509 certificate SHA-1 thumbprint.
     * @param e   The "e" (exponent) parameter, contains the exponent value for the RSA public key.
     * @param n   The "n" (modulus) parameter, contains the modulus value for the RSA public key.
     */
    record Jwk(String kty, String use, String alg, String kid, String[] x5c, String x5t, String e,
               String n) {

    }
  }
}
