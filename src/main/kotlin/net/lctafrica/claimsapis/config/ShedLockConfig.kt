package net.lctafrica.claimsapis.config

import net.javacrumbs.shedlock.core.LockProvider
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource


@Configuration
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30m")
class ShedLockConfig {
	@Bean
	fun lockProvider(dataSource: DataSource?): LockProvider {
		return JdbcTemplateLockProvider(
			JdbcTemplateLockProvider.Configuration.builder()
				.withJdbcTemplate(JdbcTemplate(dataSource!!))
				.usingDbTime()
				.build()
		)
	}
}