package org.hrfilter.resume.parser

interface ResumeParser {
    fun parse(
        input: ByteArray,
        mimeType: String,
    ): String
}
