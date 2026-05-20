package org.hrfilter.resume

import java.time.Instant

interface AuditProps {
    val createdAt: Instant
    val updatedAt: Instant
}
