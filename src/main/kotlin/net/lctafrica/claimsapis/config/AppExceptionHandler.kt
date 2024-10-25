package net.lctafrica.claimsapis.config

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
class BadRequestException(message: String) : RuntimeException() {
	override val message: String
	init {
		this.message = message
	}
}
class NotFoundRequestException(message: String) : RuntimeException() {
	override val message: String
	init {
		this.message = message
	}
}
class AppException(httpStatus: HttpStatus, message: String) : RuntimeException() {
	val httpStatus: HttpStatus
	override val message: String
	init {
		this.httpStatus = httpStatus
		this.message = message
	}
}
data class ErrorResponse (
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
	var timestamp: LocalDateTime? = null,
	var code:Int = 0,
	var error: String? = null
)
@ControllerAdvice
class AppExceptionHandler : ResponseEntityExceptionHandler() {
	@ExceptionHandler(BadRequestException::class)
	@ResponseBody
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	fun badRequestExceptionHandler(ex: BadRequestException, request: WebRequest): ErrorResponse {
		return ErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(), ex.message)
	}
	@ExceptionHandler(NotFoundRequestException::class)
	@ResponseBody
	@ResponseStatus(HttpStatus.NOT_FOUND)
	fun notFoundRequestExceptionHandler(ex: NotFoundRequestException, request: WebRequest): ErrorResponse {
		return ErrorResponse(LocalDateTime.now(), HttpStatus.NOT_FOUND.value(), ex.message)
	}
	@ExceptionHandler(AppException::class)
	fun appExceptionHandler(ex: AppException, request : WebRequest): ResponseEntity<Any> {
		val error = ErrorResponse(LocalDateTime.now(),ex.httpStatus.value(),ex.message)
		return ResponseEntity.status(ex.httpStatus).body(error)
	}
}