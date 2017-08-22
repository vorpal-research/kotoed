import actionCreatorFactory from 'typescript-fsa';
import {Dispatch} from "react-redux";
import {SignInOrUpState} from "./state";
import {OAuthProvidersRequest,
    OAuthProvidersResponse,
    signIn as signInRemote,
    signUp as signUpRemote,
    fetchOAuthProviders as fetchOAuthProvidersRemote} from "./remote";
import {Kotoed} from "../util/kotoed-api";
const actionCreator = actionCreatorFactory();

interface SignInPayload {
    username: string
    password: string
    oAuthProvider?: string
}

interface SignUpPayload {
    username: string
    password: string
    email: string|null
    oAuthProvider?: string
}


export const signIn = actionCreator.async<{}, {}, string>('SIGN_IN');
export const signUp = actionCreator.async<{}, {}, string>('SIGN_UP');
export const oAuthProvidersFetch = actionCreator.async<OAuthProvidersRequest, OAuthProvidersResponse, {}>('OAUTH_FETCH');

export const resetErrors = actionCreator<{}>('RESET_ERRORS');

export function performSignIn(payload: SignInPayload) {
    return async (dispatch: Dispatch<SignInOrUpState>) => {
        dispatch(signIn.started({}));
        try {
            await signInRemote(payload.username, payload.password)
        } catch (e) {
            dispatch(signIn.failed({
                params: {},
                error: e.message
            }));
            return
        }
        if (payload.oAuthProvider) {
            window.location.href = Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Auth.OAuthStart, {
                providerName: payload.oAuthProvider
            });
        } else {
            window.location.href = Kotoed.UrlPattern.Auth.LoginDone + window.location.hash
        }
    }
}

export function performSignUp(payload: SignUpPayload) {
    return async (dispatch: Dispatch<SignInOrUpState>) => {
        dispatch(signUp.started({}));
        try {
            await signUpRemote(payload.username, payload.password, payload.email)
        } catch (e) {
            dispatch(signUp.failed({
                params: {},
                error: e.message
            }));
            return
        }
        if (payload.oAuthProvider) {
            window.location.href = Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Auth.OAuthStart, {
                providerName: payload.oAuthProvider
            });
        } else {
            window.location.href = Kotoed.UrlPattern.Auth.LoginDone + window.location.hash
        }
    }
}

export function fetchOAuthProviders() {
    return async (dispatch: Dispatch<SignInOrUpState>) => {
        dispatch(oAuthProvidersFetch.started({}));
        let providers = await fetchOAuthProvidersRemote();
        dispatch(oAuthProvidersFetch.done({
            params: {},
            result: providers
        }));
    }
}