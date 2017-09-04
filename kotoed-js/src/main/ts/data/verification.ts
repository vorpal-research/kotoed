
export type VerificationStatus = "NotReady" | "Invalid" | "Unknown" | "Processed"

export interface VerificationData {
    status: VerificationStatus
}

export interface WithVerificationData {
    verificationData: VerificationData
}



export interface DbRecordWrapper<T> {
    record: T,
    verificationData: VerificationData
}


const finalStatuses: Array<VerificationStatus> = ["Invalid", "Processed"];

export function isStatusFinal(status: VerificationStatus) {
    return finalStatuses.includes(status);
}