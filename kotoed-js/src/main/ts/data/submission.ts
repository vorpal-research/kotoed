
import {WithId} from "./common";
import {BloatProject, WithProject} from "./project";
import {WithVerificationData} from "./verification";

export type SubmissionState = "pending" | "invalid" | "open" | "obsolete" | "closed"

export interface Submission {
    datetime: number
    parentSubmissionId?: number
    projectId: number
    state: SubmissionState
    revision: string
}

export interface SubmissionToRead extends Submission, WithId {}

export interface BloatSubmission extends SubmissionToRead, WithProject {}

// This is here because of possible problems with cyclic imports
export interface JumboProject extends BloatProject {
    openSubmissions: Array<SubmissionToRead & WithVerificationData>
}

export interface CreateRequest {
    revision: string | null,
    projectId: number,
    parentSubmissionId: number | null
}

export interface Tag {
    id: number,
    text: string
}
