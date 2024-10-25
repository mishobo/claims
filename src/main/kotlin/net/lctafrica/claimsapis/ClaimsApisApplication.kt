package net.lctafrica.claimsapis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient


@SpringBootApplication
@ComponentScan("net.lctafrica.claimsapis")
@EnableScheduling
class ClaimsApisApplication

fun main(args: Array<String>) {
	runApplication<ClaimsApisApplication>(*args)
}
