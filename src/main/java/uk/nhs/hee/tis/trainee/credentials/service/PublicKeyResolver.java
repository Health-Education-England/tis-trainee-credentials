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
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.service.PublicKeyResolver.Jwks.Jwk;

/**
 * A signing key resolver which uses cached public keys before requesting via a JWKs endpoints.
 */
@Component
public class PublicKeyResolver extends SigningKeyResolverAdapter {

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
    if (header.getKeyId() == null) {
      throw new IllegalArgumentException("No key id in headers.");
    }

    // Gateway verification tokens append the algorithm to the key id.
    String algorithm = header.getAlgorithm();
    String keyId = header.getKeyId().replaceFirst(algorithm + "$", "");

    Optional<PublicKey> cachedPublicKey = cachingDelegate.getPublicKey(keyId);

    if (cachedPublicKey.isPresent()) {
      return cachedPublicKey.get();
    }

    Jwks jwks = getJwks(claims);

    PublicKey publicKey = Arrays.stream(jwks.getKeys())
        .filter(key -> key.getKeyId().equals(keyId))
        .map(this::getPublicKey)
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Can not resolve public key for given token."));

    return cachingDelegate.cachePublicKey(keyId, publicKey);
  }

  /**
   * Get the JWKs document associated with the claims.
   *
   * @param claims The token claims, issuer must be populated.
   * @return The Jwks document.
   */
  private Jwks getJwks(Claims claims) {
    String issuer = claims.getIssuer();

    if (Objects.equals(issuer, properties.host()) || Objects.equals(issuer,
        properties.issuing().token().audience())) {
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
    byte[] modulusBytes = Base64.getUrlDecoder().decode(key.getModulus());
    BigInteger modulus = new BigInteger(1, modulusBytes);

    byte[] exponentBytes = Base64.getUrlDecoder().decode(key.getExponent());
    BigInteger exponent = new BigInteger(1, exponentBytes);

    try {
      return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * A JWKs document, with an array of JWK keys.
   */
  @Data
  static class Jwks {

    private Jwk[] keys;

    /**
     * A representation of a JWK, unused fields have been excluded for simplicity.
     */
    @Data
    static class Jwk {

      @JsonProperty("kid")
      private String keyId;

      @JsonProperty("n")
      private String modulus;

      @JsonProperty("e")
      private String exponent;
    }
  }
}
