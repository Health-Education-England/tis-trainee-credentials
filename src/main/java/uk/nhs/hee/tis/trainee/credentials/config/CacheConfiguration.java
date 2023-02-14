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

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.CacheKeyPrefix;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager.RedisCacheManagerBuilder;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configuration for caching behaviour.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

  public static final String VERIFICATION_REQUEST_DATA = "verificationRequestCacheManager";
  public static final String VERIFIED_SESSION_DATA = "verifiedSessionCacheManager";
  public static final String LOG_EVENT_DATA = "logEventCacheManager";

  private final CacheProperties properties;

  CacheConfiguration(CacheProperties properties) {
    this.properties = properties;
  }

  /**
   * Create a default cache manager for anything not covered by the overrides.
   *
   * @param factory The Redis connection factory.
   * @return The built cache manager.
   */
  @Primary
  @Bean
  public CacheManager defaultCacheManager(RedisConnectionFactory factory) {
    return buildCacheManager(factory, Duration.ofSeconds(0));
  }

  /**
   * Create a cache manager for caching of verification request data.
   *
   * @param factory The Redis connection factory.
   * @return The built cache manager.
   */
  @Bean
  public CacheManager verificationRequestCacheManager(RedisConnectionFactory factory) {
    return buildCacheManager(factory, properties.timeToLive().verificationRequest());
  }

  /**
   * Create a cache manager for verified session caching.
   *
   * @param factory The Redis connection factory.
   * @return The built cache manager.
   */
  @Bean
  public CacheManager verifiedSessionCacheManager(RedisConnectionFactory factory) {
    return buildCacheManager(factory, properties.timeToLive().verifiedSession());
  }

  /**
   * Build a cache manager using the given factory, with the given TTL.
   *
   * @param factory The Redis connection factory.
   * @param ttl     The time-to-live to apply to the cache manager.
   * @return The built cache manager.
   */
  private CacheManager buildCacheManager(RedisConnectionFactory factory, Duration ttl) {
    RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl)
        .prefixCacheNameWith(properties.keyPrefix() + CacheKeyPrefix.SEPARATOR);

    return RedisCacheManagerBuilder.fromConnectionFactory(factory)
        .cacheDefaults(configuration)
        .build();
  }
}
