type ResponseStatus = "done" | "pending" | "failed"

export interface ResponseWithStatus {
    status: ResponseStatus
}

export interface SubmissionIdRequest {
    submission_id: number
}

export interface RequestWithId {
    id: number
}
