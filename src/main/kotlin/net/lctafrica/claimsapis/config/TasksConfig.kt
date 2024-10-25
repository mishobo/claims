package net.lctafrica.claimsapis.config

import java.time.LocalDateTime
import net.lctafrica.claimsapis.util.Constants
import org.springframework.cache.annotation.CacheEvict
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled


@Configuration
@EnableScheduling
class TasksConfig {
    @CacheEvict(allEntries = true, value = [Constants.CLAIMS_CACHE])
    @Scheduled(fixedDelay = 10 * 60 * 1000, initialDelay = 500)
    fun cacheEvict() {
        //println("Flush Cache " + LocalDateTime.now())
    }
}