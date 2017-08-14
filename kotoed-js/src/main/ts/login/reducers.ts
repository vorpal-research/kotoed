import {SignInOrUpState} from "./state";
import {Action} from "redux";
import {isType} from "typescript-fsa/dist/typescript-fsa";
import {oAuthProvidersFetch, resetErrors, signIn, signUp} from "./actions";

const initialState: SignInOrUpState = {
    disabled: false,
    oAuthProviders: [],
    signInErrors: [],
    signUpErrors: []
};

export function reducer(state: SignInOrUpState = initialState, action: Action): SignInOrUpState {
    if (isType(action, signIn.started)) {
        return {...state, disabled: true}
    } else if (isType(action, signIn.failed)) {
        return {...state, disabled: false, signInErrors: [...state.signInErrors, action.payload.error]}
    } else if (isType(action, signUp.started)) {
        return {...state, disabled: true}
    } else if (isType(action, signUp.failed)) {
        return {...state, disabled: false, signUpErrors: [...state.signUpErrors, action.payload.error]}
    } else if (isType(action, resetErrors)) {
        return {...state, disabled: false, signUpErrors: [], signInErrors: []}
    } else if (isType(action, oAuthProvidersFetch.done)) {
        return {...state, oAuthProviders: action.payload.result}
    }
    return state;
}
