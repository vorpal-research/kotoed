import axios from "axios"
import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase, keysToSnakeCase} from "../util/stringCase";

export interface SignInRequest {
    denizenId: string
    password: string
}

export interface SignUpRequest {
    denizenId: string
    password: string
}

export interface SignInResponse {
    succeeded: boolean
    error: string | null
}

export interface SignUpResponse {
    succeeded: boolean
    error: string | null
}

export async function signIn(username: string, password: string) {
    let resp = await axios.post(Kotoed.UrlPattern.Auth.DoLogin, keysToSnakeCase({
        denizenId: username,
        password: password
    }));
    let logResp = keysToCamelCase(resp.data) as SignInResponse;

    if (!logResp.succeeded)
        throw new Error(logResp.error || "Unknown error")
}

export async function signUp(username: string, password: string, email: string|null) {
    let resp = await axios.post(Kotoed.UrlPattern.Auth.DoSignUp, keysToSnakeCase({
        denizenId: username,
        password: password,
        email: email
    }));
    let logResp = keysToCamelCase(resp.data) as SignUpResponse;

    if (!logResp.succeeded)
        throw new Error(logResp.error || "Unknown error")
}