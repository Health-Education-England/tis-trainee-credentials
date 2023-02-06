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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.trainee.credentials.dto.IdentityDataDto;

@Service
public class CacheService {

  private final Map<String, IdentityDataDto> identityCache = new HashMap<>();
  private final Map<String, Instant> verifiedIdentityJwts = new HashMap<>();

  private final Map<String, String> codeVerifierCache = new HashMap<>();

  public String cacheIdentityData(IdentityDataDto dto) {
    String cacheKey = UUID.randomUUID().toString();
    identityCache.put(cacheKey, dto);
    return cacheKey;
  }

  public Optional<IdentityDataDto> getCachedIdentityData(String cacheKey) {
    IdentityDataDto dto = identityCache.get(cacheKey);
    return Optional.ofNullable(dto);
  }

  public void cacheVerifiedIdentityJwt(String jwt) {
    verifiedIdentityJwts.put(jwt, Instant.now().plusSeconds(600));
  }

  public boolean hasVerifiedIdentity(String jwt) {
    Instant now = Instant.now();
    Instant expiry = verifiedIdentityJwts.getOrDefault(jwt, now);
    return expiry.isAfter(now);
  }

  public void cacheCodeVerifier(String state, String codeVerifier) {
    codeVerifierCache.put(state, codeVerifier);
  }

  public String getCachedCodeVerifier(String state) {
    return codeVerifierCache.get(state);
  }
}
