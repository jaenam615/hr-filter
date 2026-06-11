package org.hrfilter.resume.jobposting

import org.hrfilter.resume.jobposting.repository.JobPostingRepository
import java.time.Instant

interface JobPostingRegistrationService {
    fun register(command: JobPostingRegistrationCommand): JobPosting
}

class JobPostingRegistrationCommand(
    val title: String,
    val description: String,
    val requirements: String,
)

internal class JobPostingRegistrationServiceImpl(
    private val jobPostingRepository: JobPostingRepository,
) : JobPostingRegistrationService {
    override fun register(command: JobPostingRegistrationCommand): JobPosting {
        val now = Instant.now()
        return jobPostingRepository.save(
            jobPosting =
                JobPosting(
                    jobPostingId = 0L,
                    title = command.title,
                    description = command.description,
                    requirements = command.requirements,
                    createdAt = now,
                    updatedAt = now,
                ),
        )
    }
}
