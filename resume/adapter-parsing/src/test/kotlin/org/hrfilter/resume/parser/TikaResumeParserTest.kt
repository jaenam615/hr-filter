package org.hrfilter.resume.parser

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain

// Tika는 in-process로 동작하므로 외부 의존성 없이 단위 테스트 가능.
class TikaResumeParserTest : DescribeSpec({
    val parser = TikaResumeParser()

    describe("parse") {
        it("plain text 바이트에서 텍스트를 추출한다") {
            val content = "홍길동\n경력: 코틀린 백엔드 4년".toByteArray()

            val result = parser.parse(input = content, mimeType = "text/plain")

            result shouldContain "홍길동"
            result shouldContain "코틀린 백엔드"
        }
    }
})
