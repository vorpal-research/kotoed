
import {WithId} from "./common";
import {BloatProject, WithProject} from "./project";

type SubmissionState = "pending" | "invalid" | "open" | "obsolete" | "closed"

export interface Submission {
    dateime: number
    parentSubmissionId?: number
    projectId: number
    state: SubmissionState
    revision: string
}

export interface SubmissionToRead extends Submission, WithId {}

export interface BloatSubmission extends SubmissionToRead, WithProject {}

// This is here because of possible problems with cyclic imports
export interface JumboProject extends BloatProject {
    openSubmissions: Array<SubmissionToRead>
}