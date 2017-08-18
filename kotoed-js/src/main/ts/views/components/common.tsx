import {eventBus} from "../../eventBus";

export interface ErrorDesc {
    id: number
    data: any
}

export enum VerificationStatus {
    Processed,
    NotReady,
    Invalid,
    Unknown
}

export interface VerificationData {
    status: string
    errors: number[]
}

export interface IdRequest {
    id: number
}

export interface GenericResponse<ResultT> {
    records: ResultT[]
    verificationData: VerificationData
}

export function sendAsync<Request, Response>(address: string, request: Request): Promise<Response> {
    return eventBus.awaitOpen().then(_ =>
        eventBus.send<Request, Response>(address, request)
    );
}
