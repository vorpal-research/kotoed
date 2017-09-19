
import {WithId} from "./common";
import {BloatProject, ProjectToRead, WithProject} from "./project";
import {WithVerificationData} from "./verification";
import {WithBloatDenizen} from "./denizen";

export type SubmissionState = "pending" | "invalid" | "open" | "obsolete" | "closed"

export interface Submission {
    datetime: number
    parentSubmissionId?: number
    projectId: number
    state: SubmissionState
    revision: string
}

export interface SubmissionToRead extends Submission, WithId, WithSubmissionTags {}

export interface BloatSubmission extends SubmissionToRead {
    project: ProjectToRead & WithBloatDenizen
}

// This is here because of possible problems with cyclic imports
export interface JumboProject extends BloatProject {
    openSubmissions: Array<SubmissionToRead & WithVerificationData>
}

export interface CreateRequest {
    revision: string | null,
    projectId: number,
    parentSubmissionId: number | null
}

export interface TagRTI {
    id: number,
    text: string
}

export interface Tag {
    name: string
}

export interface SubmissionTag {
    tag: Tag
}

export interface WithSubmissionTags {
    submissionTags?: Array<SubmissionTag>
}
