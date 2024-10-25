package net.lctafrica.claimsapis.controller

import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException

import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus


@ControllerAdvice
class ControllerConfig {
	@ExceptionHandler
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	fun handle(e: HttpMessageNotReadableException?) {

		//println( e)
		////log.warn("Returning HTTP 400 Bad Request", e)
		throw e!!
	}
}