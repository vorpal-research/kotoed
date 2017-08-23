
export type VerificationStatus = "NotReady" | "Invalid" | "Unknown" | "Processed"

export interface VerificationData {
    status: VerificationStatus
}

export interface WithVerificationData {
    verificationData: VerificationData
}
