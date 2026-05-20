package org.hrfilter.resume.batchrun

interface BatchRunIdentity {
    companion object

    val batchRunId: Long
}

internal data class SimpleBatchRunIdentity(
    override val batchRunId: Long,
) : BatchRunIdentity

fun BatchRunIdentity.Companion.of(batchRunId: Long): BatchRunIdentity = SimpleBatchRunIdentity(batchRunId)
