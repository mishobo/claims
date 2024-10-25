package net.lctafrica.claimsapis.config

import net.lctafrica.claimsapis.util.Constants.CLAIMS_CACHE
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CachingConfigurerSupport
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
@EnableCaching
class Config : CachingConfigurerSupport() {
    @Bean
    override fun cacheManager(): CacheManager? {
        return ConcurrentMapCacheManager(CLAIMS_CACHE)
    }
}