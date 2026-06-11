package org.hrfilter.resume.parser

import org.apache.tika.metadata.HttpHeaders
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.AutoDetectParser
import org.apache.tika.parser.ParseContext
import org.apache.tika.sax.BodyContentHandler
import java.io.ByteArrayInputStream

internal class TikaResumeParser : ResumeParser {
    private val parser = AutoDetectParser()

    override fun parse(
        input: ByteArray,
        mimeType: String,
    ): String {
        val handler = BodyContentHandler(NO_WRITE_LIMIT)
        val metadata =
            Metadata().apply {
                set(HttpHeaders.CONTENT_TYPE, mimeType)
            }
        ByteArrayInputStream(input).use { stream ->
            parser.parse(stream, handler, metadata, ParseContext())
        }
        return handler.toString().trim()
    }

    private companion object {
        const val NO_WRITE_LIMIT = -1
    }
}
