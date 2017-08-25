import * as React from "react"
import {ChangeEvent, MouseEvent, KeyboardEvent} from "react";
import {ErrorMessages} from "../util";
import {ComponentWithLocalErrors} from "../../views/components/ComponentWithLocalErrors";
import SocialButton from "./SocialButton";

import "less/util.less"

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

    handlePasswordChange = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            password: event.target.value
        });

        this.unsetError("emptyPassword");
    };

    handleSignIn = () => {
        let ok = true;
        if (this.state.username === "") {
            this.setError("emptyUsername");
            ok = false;
        }

        if (this.state.password === "") {
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
                <div className={`form-group ${this.state.localErrors.emptyUsername && "has-error"}`}>
                    <label htmlFor="signin-input-login" className="sr-only">
                        Username
                    </label>
                    <input
                        required
                        type="text"
                        id="signin-input-username"
                        className="form-control"
                        name="username"
                        placeholder="Username"
                        onChange={this.handleUsernameChange}
                        value={this.state.username}
                        disabled={this.props.disabled}
                        onKeyPress={this.handleEnter}
                    />
                </div>
                <div className={`form-group  ${this.state.localErrors.emptyPassword && "has-error"}`}>
                    <label htmlFor="signin-input-password" className="sr-only">
                        Password
                    </label>
                    <input
                        required
                        type="password"
                        id="signin-input-password"
                        className="form-control"
                        name="password"
                        placeholder="Password"
                        onChange={this.handlePasswordChange}
                        value={this.state.password}
                        disabled={this.props.disabled}
                        onKeyPress={this.handleEnter}
                    />
                </div>
            </form>
            <button key="sign-in" className="btn btn-lg btn-primary btn-block"
                    onClick={this.handleSignInClick}
                    disabled={this.props.disabled}>
                Sign in
            </button>
            <div className="vspace-10"/>
            {this.renderOAuthButtons()}
        </div>


    }
}
