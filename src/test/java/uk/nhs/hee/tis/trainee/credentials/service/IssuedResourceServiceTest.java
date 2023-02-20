package uk.nhs.hee.tis.trainee.credentials.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.nhs.hee.tis.trainee.credentials.config.GatewayProperties;
import uk.nhs.hee.tis.trainee.credentials.dto.IssueRequestDto;
import uk.nhs.hee.tis.trainee.credentials.mapper.CredentialMetadataMapper;
import uk.nhs.hee.tis.trainee.credentials.model.CredentialMetadata;
import uk.nhs.hee.tis.trainee.credentials.repository.CredentialMetadataRepository;

class IssuedResourceServiceTest {

  private static final URI PAR_ENDPOINT = URI.create(
      "https://credential.gateway/par/endpoint");
  private static final URI AUTHORIZE_ENDPOINT = URI.create(
      "https://credential.gateway/authorize/endpoint");
  private static final URI TOKEN_ENDPOINT = URI.create("https://credential.gateway/token/endpoint");
  private static final URI REDIRECT_URI = URI.create("https://credential.service/redirect-uri");
  private static final URI CALLBACK_URI = URI.create("https://credential.service/callback-uri");

  private static final String AUTH_TOKEN = "auth-token";
  private static final String CODE_PARAM = "code";
  private static final String CODE_VALUE = "some-code";
  private static final String STATE_PARAM = "state";
  private static final String STATE_VALUE = "some-state";
  private static final String ERROR_PARAM = "error";
  private static final String ERROR_VALUE = "some-error";
  private static final String ERROR_DESCRIPTION_PARAM = "error_description";
  private static final String ERROR_DESCRIPTION_VALUE = "some-error-description";

  private static final String TIS_ID_ATTRIBUTE = "custom:tisId";

  private static final String TIS_ID = "the-tis-id";
  private static final String TRAINEE_ID = "the-trainee-id";
  private static final String CREDENTIAL_ID = "123-456-789";
  private static final String CREDENTIAL_TYPE = "the-credential-type";
  private static final LocalDateTime ISSUED_AT = LocalDateTime.MIN.truncatedTo(ChronoUnit.SECONDS);
  private static final LocalDateTime EXPIRES_AT = LocalDateTime.MAX.truncatedTo(ChronoUnit.SECONDS);

  private IssuedResourceService issuedResourceService;
  private GatewayService gatewayService;
  private JwtService jwtService;
  private CachingDelegate cachingDelegate;
  private CredentialMetadataRepository credentialMetadataRepository;
  @SpyBean
  private CredentialMetadataMapper credentialMetadataMapper;

  @BeforeEach
  void setUp() {
    gatewayService = mock(GatewayService.class);
    jwtService = mock(JwtService.class);
    cachingDelegate = spy(CachingDelegate.class);
    credentialMetadataRepository = mock(CredentialMetadataRepository.class);
    credentialMetadataMapper = Mappers.getMapper(CredentialMetadataMapper.class);

    var tokenProperties = new GatewayProperties.IssuingProperties.TokenProperties(
        "audience", "issuer", "signingKey");

    var properties = new GatewayProperties.IssuingProperties(
        PAR_ENDPOINT.toString(),
        AUTHORIZE_ENDPOINT.toString(),
        TOKEN_ENDPOINT.toString(),
        tokenProperties,
        CALLBACK_URI.toString(),
        REDIRECT_URI.toString());

    issuedResourceService = new IssuedResourceService(
        gatewayService, credentialMetadataRepository, credentialMetadataMapper, cachingDelegate,
        properties, jwtService);
  }

  @Test
  void shouldCacheRequestDetailsWhenStartingIssuing() {
    // TODO - currently this is buried in the GatewayService
  }

  @Test
  void shouldNotSaveToRepositoryWhenCredentialNotIssued() {
    issuedResourceService.logIssuedResource(null, STATE_VALUE, ERROR_VALUE,
        ERROR_DESCRIPTION_VALUE, AUTH_TOKEN);
    verifyNoInteractions(credentialMetadataRepository);
  }

  @Test
  void shouldSaveToRepositoryWhenCredentialIssued() throws IOException {
    UUID uuid = UUID.randomUUID();
    Claims claimsIssued = new DefaultClaims();
    claimsIssued.put("nonce", uuid.toString());
    claimsIssued.put("SerialNumber", CREDENTIAL_ID);
    claimsIssued.put("iat", ISSUED_AT.toEpochSecond(ZoneOffset.UTC));
    claimsIssued.put("exp", EXPIRES_AT.toEpochSecond(ZoneOffset.UTC));

    IssueRequestDto issueRequestDto
        = new IssueRequestDto(CREDENTIAL_TYPE, TIS_ID);
    Map<String, String> authTokenMap = new HashMap<>();
    authTokenMap.put(TIS_ID_ATTRIBUTE, TRAINEE_ID);

    // TODO: this is quite brittle
    when(gatewayService.getTokenClaims(eq(TOKEN_ENDPOINT), eq(CALLBACK_URI), eq(CODE_VALUE), any()))
        .thenReturn(claimsIssued);
    when(cachingDelegate.getCredentialMetadata(uuid))
        .thenReturn(Optional.of(issueRequestDto));
    when(jwtService.getTokenBodyMap(AUTH_TOKEN))
        .thenReturn(authTokenMap);

    issuedResourceService.logIssuedResource(CODE_VALUE, STATE_VALUE, null, null, AUTH_TOKEN);

    ArgumentCaptor<CredentialMetadata> argument = ArgumentCaptor.forClass(CredentialMetadata.class);
    verify(credentialMetadataRepository).save(argument.capture());
    assertEquals(CREDENTIAL_ID, argument.getValue().getCredentialId());
    assertEquals(CREDENTIAL_TYPE, argument.getValue().getCredentialType());
    assertEquals(TIS_ID, argument.getValue().getTisId());
    assertEquals(TRAINEE_ID, argument.getValue().getTraineeId());
    assertEquals(ISSUED_AT, argument.getValue().getIssuedAt());
    assertEquals(EXPIRES_AT, argument.getValue().getExpiresAt());
  }
}
