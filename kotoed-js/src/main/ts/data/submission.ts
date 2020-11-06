
import {WithId} from "./common";
import {BloatProject, ProjectToRead, WithProject} from "./project";
import {WithVerificationData} from "./verification";
import {WithBloatDenizen} from "./denizen";
import {CSSProperties} from "react";

export type SubmissionState = "pending" | "invalid" | "open" | "obsolete" | "closed" | "deleted"

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
    permanentAdjustment: number
    permanentAdjustmentSubmissions: Array<SubmissionToRead & WithVerificationData>
}

export interface CreateRequest {
    revision: string | null,
    projectId: number,
    parentSubmissionId: number | null
}

export interface Tag {
    id: number
    name: string
    style: CSSProperties
}

export interface SubmissionTag {
    tag: Tag
}

export interface WithSubmissionTags {
    submissionTags?: Array<SubmissionTag>
}
