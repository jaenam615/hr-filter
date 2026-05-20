package org.hrfilter.resume.infrastructure.parser

interface ResumeParser {
    fun parse(
        input: ByteArray,
        mimeTYpe: String,
    ): String
}
