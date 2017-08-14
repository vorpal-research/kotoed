
export interface SignInOrUpState {
    disabled: boolean
    signInErrors: Array<string>
    signUpErrors: Array<string>
    oAuthProviders: Array<string>
    oAuthMsg?: string
}
