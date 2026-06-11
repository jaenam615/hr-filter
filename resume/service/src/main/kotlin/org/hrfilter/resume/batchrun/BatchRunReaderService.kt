package org.hrfilter.resume.batchrun

import org.hrfilter.resume.batchrun.repository.BatchRunRepository

interface BatchRunReaderService {
    fun getLatest(limit: Int): List<BatchRun>
}

internal class BatchRunReaderServiceImpl(
    private val batchRunRepository: BatchRunRepository,
) : BatchRunReaderService {
    override fun getLatest(limit: Int): List<BatchRun> = batchRunRepository.findLatestN(limit = limit, offset = 0)
}
