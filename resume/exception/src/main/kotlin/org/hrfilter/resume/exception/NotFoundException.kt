package org.hrfilter.resume.exception

// 도메인 "리소스 없음" 예외의 공통 베이스. 웹 경계(@RestControllerAdvice)에서 404로 일괄 매핑.
abstract class NotFoundException(message: String) : RuntimeException(message)
