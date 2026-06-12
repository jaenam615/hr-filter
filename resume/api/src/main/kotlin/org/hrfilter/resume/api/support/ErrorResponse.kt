package org.hrfilter.resume.api.support

data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
)
