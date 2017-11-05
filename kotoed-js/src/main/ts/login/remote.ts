import axios from "axios"
import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase, keysToSnakeCase} from "../util/stringCase";
import {eventBus} from "../eventBus";
import UrlPattern = Kotoed.UrlPattern;
import {setTimeout} from "timers";

export interface SignInResponse {
    succeeded: boolean
    error: string | null
}

export interface SignUpResponse {
    succeeded: boolean
    error: string | null
}

export interface ResetPasswordResponse {
    succeeded: boolean
    error: string | null
}

export type OAuthProvidersRequest = {}
export type OAuthProvidersResponse = Array<string>

export async function signIn(username: string, password: string) {
    let resp = await axios.post(Kotoed.UrlPattern.Auth.DoLogin, keysToSnakeCase({
        denizenId: username.trim(),
        password: password
    }));
    let logResp = keysToCamelCase(resp.data) as SignInResponse;

    if (!logResp.succeeded)
        throw new Error(logResp.error || "Unknown remoteError")
}

export async function signUp(username: string, password: string, email: string|null) {
    let resp = await axios.post(Kotoed.UrlPattern.Auth.DoSignUp, keysToSnakeCase({
        denizenId: username.trim(),
        password: password,
        email: email
    }));
    let logResp = keysToCamelCase(resp.data) as SignUpResponse;

    if (!logResp.succeeded)
        throw new Error(logResp.error || "Unknown remoteError")
}

export async function resetPassword(username: string, email: string) {
    let resp = await axios.post(Kotoed.UrlPattern.Auth.ResetPassword, keysToSnakeCase({
        denizenId: username,
        email: email
    }));
    let logResp = keysToCamelCase(resp.data) as ResetPasswordResponse;

    if (!logResp.succeeded)
        throw new Error(logResp.error || "Unknown remoteError")
}

export async function changePassword(secret: string, username: string, password: string) {
    let address = UrlPattern.reverse(
        Kotoed.UrlPattern.Auth.RestorePassword,
        { uid: secret }
    );
    let resp = await axios.post(
        address, keysToSnakeCase({
        denizenId: username,
        password: password
    }));
    let logResp = keysToCamelCase(resp.data) as ResetPasswordResponse;

    if (!logResp.succeeded)
        throw new Error(logResp.error || "Unknown remoteError")
}

function asyncCached<T>(body: () => Promise<T>): () => Promise<T> {
    let cache: T | undefined;
    return async function () {
        if(!cache) {
            cache = await body()
        }
        return cache
    };
}

export let myDatabaseId = asyncCached(
    async function () {
        let resp = await axios.get(Kotoed.UrlPattern.AuthHelpers.WhoAmI);
        return keysToCamelCase(resp.data).id as string;
    }
);

export async function fetchOAuthProviders(): Promise<OAuthProvidersResponse> {
    return await
        eventBus.send<OAuthProvidersRequest, OAuthProvidersResponse>(Kotoed.Address.Api.OAuthProvider.List, {});

}
