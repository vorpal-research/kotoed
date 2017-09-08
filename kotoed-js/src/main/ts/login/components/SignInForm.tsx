import * as React from "react"
import {ChangeEvent, MouseEvent, KeyboardEvent} from "react";
import {ErrorMessages} from "../util";
import {ComponentWithLocalErrors} from "../../views/components/ComponentWithLocalErrors";
import SocialButton from "./SocialButton";

import "less/util.less"
import {Kotoed} from "../../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {PasswordErrors, PasswordInput} from "../../views/components/PasswordInput";

type LocalErrors = {
    emptyUsername: boolean
    emptyPassword: boolean
}


interface SignInFormState {
    username: string
    password: string
}

export interface SignInFormProps {
    errors: Array<string>
    disabled: boolean
    onSignIn: (login: string, password: string) => void
    oAuthProviders: Array<string>
    onStartOAuth: (provider: string) => void
}

export default class SignInForm extends
    ComponentWithLocalErrors<SignInFormProps, SignInFormState, LocalErrors> {

    // This field does not directly affect rendering
    private passwordErrors: PasswordErrors = {
        emptyPassword: false,
        emptyPassword2: false,
        passwordsDoNotMatch: false,
    };

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptyUsername: "Please enter username",
        emptyPassword: "Please enter password",
    };

    constructor(props: SignInFormProps) {
        super(props);
        this.state = {
            username: "",
            password: "",
            localErrors: {
                emptyUsername: false,
                emptyPassword: false
            }
        }
    }

    getErrorMessages(): Array<string> {
        let messages = super.getErrorMessages();
        for (let error of this.props.errors)
            messages.push(error);

        return messages;
    };

    handleUsernameChange = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            username: event.target.value
        });

        this.unsetError("emptyUsername");
    };

    handlePasswordChange = (password: string, errors: PasswordErrors) => {
        this.setState({
            password
        });
        this.passwordErrors = errors;
        this.unsetError("emptyPassword");
    };

    handleSignIn = () => {
        let ok = true;
        if (this.state.username === "") {
            this.setError("emptyUsername");
            ok = false;
        }

        if (this.passwordErrors.emptyPassword) {
            this.setError("emptyPassword");
            ok = false;
        }

        if (ok)
            this.props.onSignIn(this.state.username, this.state.password);
    };

    handleSignInClick = (event: MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
        this.handleSignIn();
    };

    handleEnter = (event: KeyboardEvent<HTMLInputElement>) => event.key === "Enter" && this.handleSignIn();

    renderOAuthButtons = (): JSX.Element => {
        return <div className="btn-toolbar text-center social-btn-toolbar">
            {this.props.oAuthProviders.map((provider: string) => {
                return <SocialButton key={provider} provider={provider} onClick={this.props.onStartOAuth}/>
            })}
        </div>;
    };

    render() {
        return <div>
            <form className="form-signin">
                {this.renderErrors()}
                <div className={`form-group ${this.state.localErrors.emptyUsername && "has-error" || ""}`}>
                    <label htmlFor="signin-input-login" className="sr-only">
                        Username
                    </label>
                    <input
                        required
                        type="text"
                        id="signin-input-username"
                        className="form-control input-lg"
                        name="username"
                        placeholder="Username"
                        onChange={this.handleUsernameChange}
                        value={this.state.username}
                        disabled={this.props.disabled}
                        onKeyPress={this.handleEnter}
                    />
                </div>
                <PasswordInput
                    disabled={this.props.disabled}
                    onChange={this.handlePasswordChange}
                    onEnter={this.handleSignIn}
                    setPassword={false}
                    prefix="signin-"
                    classNames={{
                        label: "sr-only",
                        input: "input-lg"
                    }}
                />
            </form>
            <button key="sign-in" className="btn btn-lg btn-primary btn-block"
                    onClick={this.handleSignInClick}
                    disabled={this.props.disabled}>
                Sign in
            </button>
            <a className="pull-right" href={UrlPattern.Auth.ResetPassword}>Forgot password?</a>
            <div className="clearfix" />
            <div className="vspace-10" />
            {this.renderOAuthButtons()}
        </div>


    }
}
