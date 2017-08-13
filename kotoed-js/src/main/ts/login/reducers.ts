import {SignInOrUpState} from "./state";
import {Action} from "redux";
import {isType} from "typescript-fsa/dist/typescript-fsa";
import {resetErrors, signIn, signUp} from "./actions";

const initialState: SignInOrUpState = {
    disabled: false
};

export function reducer(state: SignInOrUpState = initialState, action: Action): SignInOrUpState {
    if (isType(action, signIn.started)) {
        return {...state, disabled: true}
    } else if (isType(action, signIn.failed)) {
        return {...state, disabled: false, signInError: action.payload.error}
    } else if (isType(action, signUp.started)) {
        return {...state, disabled: true}
    } else if (isType(action, signUp.failed)) {
        return {...state, disabled: false, signUpError: action.payload.error}
    } else if (isType(action, resetErrors)) {
        return {...state, disabled: false, signUpError: undefined, signInError: undefined}
    }
    return state;
}
