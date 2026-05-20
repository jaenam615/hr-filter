package org.hrfilter.resume.resume

import org.hrfilter.resume.AuditProps
import org.hrfilter.resume.jobposting.JobPostingIdentity

interface ResumeProps {
    val applicantName: String
    val applicantEmail: String
    val objectKey: String
    val mimeType: String
    val status: ResumeStatus
}

interface ResumeModel :
    ResumeIdentity,
    JobPostingIdentity,
    ResumeProps,
    AuditProps
