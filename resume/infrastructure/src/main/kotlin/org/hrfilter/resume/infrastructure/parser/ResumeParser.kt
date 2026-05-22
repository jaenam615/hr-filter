package org.hrfilter.resume.infrastructure.parser

interface ResumeParser {
    fun parse(
        input: ByteArray,
        mimeType: String,
    ): String
}
