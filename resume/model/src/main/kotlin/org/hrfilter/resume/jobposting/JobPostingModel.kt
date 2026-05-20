package org.hrfilter.resume.jobposting

import org.hrfilter.resume.AuditProps

interface JobPostingProps {
    val title: String
    val description: String
    val requirements: String
}

interface JobPostingModel :
    JobPostingIdentity,
    JobPostingProps,
    AuditProps
