import * as React from "react"
import {ChangeEvent, MouseEvent} from "react";
import {ErrorMessages} from "../util";
import {ComponentWithLocalErrors} from "../../views/components/ComponentWithLocalErrors";

type LocalErrors = {
    emptyUsername: boolean
    emptyPassword: boolean
    emptyPassword2: boolean
    badEmail: boolean
    passwordsDoNotMatch: boolean
}


interface SignUpFormState {
    username: string
    password: string
    password2: string
    email: string
}

export interface SignUpFormProps {
    errors: Array<string>
    disabled: boolean
    onSignUp: (login: string, password: string, email: string | null) => void
}

export default class SignUpForm extends
    ComponentWithLocalErrors<SignUpFormProps, SignUpFormState, LocalErrors> {

    eMailField: HTMLInputElement;

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptyUsername: "Please enter username",
        emptyPassword: "Please enter password",
        emptyPassword2: "Please retype your password",
        badEmail: "Please enter proper e-mail address",
        passwordsDoNotMatch: "Passwords do not match",
    };

    constructor(props: SignUpFormProps) {
        super(props);
        this.state = {
            username: "",
            password: "",
            password2: "",
            email: "",
            localErrors: {
                emptyUsername: false,
                emptyPassword: false,
                emptyPassword2: false,
                badEmail: false,
                passwordsDoNotMatch: false
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
        this.unsetError("passwordsDoNotMatch")
    };

    handlePassword2Change = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            password2: event.target.value
        });

        this.unsetError("emptyPassword2");
        this.unsetError("passwordsDoNotMatch")
    };

    handleEmailChange = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            email: event.target.value
        });

        this.unsetError("badEmail");
    };

    handleSignUpClick = (event: MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
        let ok = true;
        if (this.state.username === "") {
            this.setError("emptyUsername");
            ok = false;
        }

        let passwordOk = true;

        if (this.state.password === "") {
            this.setError("emptyPassword");
            ok = false;
            passwordOk = false;
        }

        if (this.state.password2 === "") {
            this.setError("emptyPassword2");
            ok = false;
            passwordOk = false;
        }

        if (passwordOk && this.state.password2 !== this.state.password) {
            this.setError("passwordsDoNotMatch");
            ok = false;
        }

        if (!this.eMailField.checkValidity()) {
            this.setError("badEmail");
            ok = false;
        }

        if (ok)
            this.props.onSignUp(
                this.state.username,
                this.state.password,
                this.state.email !== "" ? this.state.email : null);
    };

    render() {
        const {localErrors} = this.state;
        return <form className="form-signin">
            {this.renderErrors()}
            <div className={`form-group ${localErrors.emptyUsername && "has-error"}`}>
                <label htmlFor="signup-input-login" className="sr-only">
                    Username
                </label>
                <input
                    required
                    type="text"
                    id="signup-input-username"
                    className="form-control"
                    name="username"
                    placeholder="Username"
                    onChange={this.handleUsernameChange}
                    value={this.state.username}
                    disabled={this.props.disabled}
                />
            </div>
            <div className={`form-group ${localErrors.badEmail && "has-error"}`}>
                <label htmlFor="signup-input-email" className="sr-only">
                    E-mail
                </label>
                <input
                    ref={ref => this.eMailField = ref!}
                    type="email"
                    id="signup-input-email"
                    className="form-control"
                    name="email"
                    placeholder="E-Mail"
                    onChange={this.handleEmailChange}
                    value={this.state.email}
                    disabled={this.props.disabled}
                />
            </div>
            <div className={`form-group  ${(localErrors.emptyPassword || localErrors.passwordsDoNotMatch) && "has-error"}`}>
                <label htmlFor="signup-input-password" className="sr-only">
                    Password
                </label>
                <input
                    required
                    type="password"
                    id="signup-input-password"
                    className="form-control"
                    name="password"
                    placeholder="Password"
                    onChange={this.handlePasswordChange}
                    value={this.state.password}
                    disabled={this.props.disabled}
                />
            </div>
            <div className={`form-group  ${(localErrors.emptyPassword2 || localErrors.passwordsDoNotMatch) && "has-error"}`}>
                <label htmlFor="signup-input-password2" className="sr-only">
                    Retype password
                </label>
                <input
                    required
                    type="password"
                    id="signup-input-password2"
                    className="form-control"
                    name="password2"
                    placeholder="Retype password"
                    onChange={this.handlePassword2Change}
                    value={this.state.password2}
                    disabled={this.props.disabled}
                />
            </div>
            <button className="btn btn-lg btn-primary btn-block"
                    onClick={this.handleSignUpClick}
                    disabled={this.props.disabled}>
                Sign up
            </button>
        </form>
    }
}
