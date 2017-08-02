type ResponseStatus = "done" | "pending" | "failed"

export interface ResponseWithStatus {
    status: ResponseStatus
}

export interface SubmissionIdRequest {
    submissionId: number
}

export interface RequestWithId {
    id: number
}
