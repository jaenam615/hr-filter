package org.hrfilter.resume.parser

internal class TikaResumeParser : ResumeParser {
    override fun parse(
        input: ByteArray,
        mimeType: String,
    ): String {
        TODO("Tika AutoDetectParser로 input 파싱 후 plain text 반환")
    }
}
