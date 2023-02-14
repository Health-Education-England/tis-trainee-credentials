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

package uk.nhs.hee.tis.trainee.credentials.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A representation of the credential gateway properties.
 *
 * @param host         The gateway host.
 * @param clientId     The gateway client ID.
 * @param clientSecret The gateway client secret.
 * @param jwksEndpoint The endpoint for JWKS document.
 * @param issuing      The issuing child properties.
 * @param verification The verification child properties.
 */
@ConfigurationProperties(prefix = "application.gateway")
public record GatewayProperties(
    String host,
    String clientId,
    String clientSecret,
    String jwksEndpoint,
    IssuingProperties issuing,
    VerificationProperties verification) {

  /**
   * A representation of the gateway's issuing properties.
   *
   * @param parEndpoint       The gateway's PAR endpoint URI.
   * @param authorizeEndpoint The gateway's issue authorize endpoint URI.
   * @param tokenEndpoint     The gateway's issue token endpoint URI.
   * @param token             The issuing token child properties.
   * @param callbackUri       The URI to use to log the issuing event outcome.
   * @param redirectUri       The URI to redirect to after issuing a credential.
   */
  @ConfigurationProperties(prefix = "application.gateway.issuing")
  public record IssuingProperties(
      String parEndpoint,
      String authorizeEndpoint,
      String tokenEndpoint,
      TokenProperties token,
      String callbackUri,
      String redirectUri) {

    /**
     * A representation of the issuing token properties.
     *
     * @param audience   The audience of the credential token.
     * @param issuer     The issuer of the credential token.
     * @param signingKey The key to use for signing the credential token.
     */
    @ConfigurationProperties(prefix = "application.gateway.issuing.token")
    public record TokenProperties(String audience, String issuer, String signingKey) {

    }
  }

  /**
   * A representation of the gateway verification properties.
   *
   * @param authorizeEndpoint The gateway's connect authorize endpoint URI.
   * @param tokenEndpoint     The gateway's connect token endpoint URI.
   * @param redirectUri       The URI to redirect to after providing a credential.
   */
  @ConfigurationProperties(prefix = "application.gateway.verification")
  public record VerificationProperties(
      String authorizeEndpoint,
      String tokenEndpoint,
      String redirectUri) {

  }
}
