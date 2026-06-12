package org.hrfilter.resume.api.support

import jakarta.validation.ConstraintViolationException
import org.hrfilter.resume.exception.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 도메인/검증 예외를 일관된 에러 응답으로 매핑하는 웹 경계.
 * - 도메인은 HTTP를 모른다(NotFoundException 등 순수 예외만 던짐) → 여기서 상태코드로 번역.
 * - 예기치 못한 예외는 내부 메시지/스택을 노출하지 않고 일반 메시지로 응답(로그에만 원인 기록).
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(e: NotFoundException): ErrorResponse =
        ErrorResponse(status = 404, error = "Not Found", message = e.message ?: "Not found")

    @ExceptionHandler(MethodArgumentNotValidException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleBodyValidation(e: MethodArgumentNotValidException): ErrorResponse =
        ErrorResponse(
            status = 400,
            error = "Bad Request",
            message = "입력값 검증에 실패했습니다",
            fieldErrors = e.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "invalid") },
        )

    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleParamValidation(e: ConstraintViolationException): ErrorResponse =
        ErrorResponse(
            status = 400,
            error = "Bad Request",
            message = "입력값 검증에 실패했습니다",
            fieldErrors = e.constraintViolations.associate { it.propertyPath.toString().substringAfterLast('.') to it.message },
        )

    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleUnexpected(e: Exception): ErrorResponse {
        log.error("Unhandled exception", e)
        return ErrorResponse(status = 500, error = "Internal Server Error", message = "서버 오류가 발생했습니다")
    }
}
