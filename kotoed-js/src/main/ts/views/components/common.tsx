import {defaultErrorHandler, eventBus} from "../../eventBus";
import {Generated} from "../../util/kotoed-generated";
import ApiBindingInputs = Generated.ApiBindingInputs;
import ApiBindingOutputs = Generated.ApiBindingOutputs;
import {PPartial} from "../../util/types";

export interface ErrorDesc {
    id: number
    data?: any
}

export type VerificationStatus =
    "Processed" | "NotReady" | "Invalid" | "Unknown"
export namespace VerificationStatus {
    const Processed : "Processed" = "Processed";
    const NotReady : "NotReady" = "NotReady";
    const Invalid : "Invalid" = "Invalid";
    const Unknown : "Unknown" = "Unknown";
}

export interface VerificationData {
    status: VerificationStatus
    errors: number[]
}

export interface IdRequest {
    id: number
}

export interface GenericResponse<ResultT> {
    records: ResultT[]
    verificationData: VerificationData
}

export function sendAsync<Address extends string>(address: Address, request: ApiBindingInputs[Address],
                                             onError?: typeof defaultErrorHandler): Promise<ApiBindingOutputs[Address]> {
    request = (request == null)? {} : request;
    return eventBus.awaitOpen().then(_ =>
        eventBus.send<PPartial<ApiBindingOutputs[Address]>, ApiBindingInputs[Address]>(address, request, undefined, onError)
    );
}

export function setStateAsync<S, P, U extends keyof S>(self: React.Component<P, S>, state: Pick<S, U>): Promise<void> {
    return new Promise(((resolve, _) => {
        self.setState(state, () => resolve())
    }));
}
