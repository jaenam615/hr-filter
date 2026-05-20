package org.hrfilter.resume.resume

interface ResumeService {
    fun get(identity: ResumeIdentity): Resume

    fun
}