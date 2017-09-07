import * as React from "react"
import {ChangeEvent, MouseEvent} from "react";
import {ErrorMessages} from "../util";
import {ComponentWithLocalErrors} from "../../views/components/ComponentWithLocalErrors";
import {Kotoed} from "../../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {PasswordErrors, PasswordInput} from "../../views/components/PasswordInput";

type LocalErrors = {
    emptyUsername: boolean
    emptyPassword: boolean
    emptyPassword2: boolean
    badEmail: boolean
    passwordsDoNotMatch: boolean
}

const defaultLocalErrors = {
    emptyUsername: false,
    emptyPassword: false,
    emptyPassword2: false,
    badEmail: false,
    passwordsDoNotMatch: false,
};

interface SignUpFormState {
    username: string
    password: string
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
        emptyPassword2: "Please re-enter your password",
        badEmail: "Please enter proper e-mail address",
        passwordsDoNotMatch: "Passwords do not match",
    };

    // This field does not directly affect rendering
    private passwordErrors: PasswordErrors = {
        emptyPassword: false,
        emptyPassword2: false,
        passwordsDoNotMatch: false,
    };

    constructor(props: SignUpFormProps) {
        super(props);
        this.state = {
            username: "",
            password: "",
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

    handlePasswordChange = (password: string, errors: PasswordErrors) => {
        this.setState({
            password
        });

        this.passwordErrors = errors;

        this.unsetError("emptyPassword");
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

        this.setState({localErrors: defaultLocalErrors});

        let ok = true;
        if (this.state.username === "") {
            this.setError("emptyUsername");
            ok = false;
        }

        let passwordOk = true;

        if (this.passwordErrors.emptyPassword) {
            this.setError("emptyPassword");
            ok = false;
            passwordOk = false;
        }

        if (this.passwordErrors.emptyPassword2) {
            this.setError("emptyPassword2");
            ok = false;
            passwordOk = false;
        }

        if (passwordOk && this.passwordErrors.passwordsDoNotMatch) {
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
            <div className={`form-group ${localErrors.emptyUsername && "has-error" || ""}`}>
                <label htmlFor="signup-input-login" className="sr-only">
                    Username
                </label>
                <input
                    required
                    type="text"
                    id="signup-input-username"
                    className="form-control input-lg"
                    name="username"
                    placeholder="Username"
                    onChange={this.handleUsernameChange}
                    value={this.state.username}
                    disabled={this.props.disabled}
                />
            </div>
            <div className={`form-group ${localErrors.badEmail && "has-error" || ""}`}>
                <label htmlFor="signup-input-email" className="sr-only">
                    E-mail
                </label>
                <input
                    ref={ref => this.eMailField = ref!}
                    type="email"
                    id="signup-input-email"
                    className="form-control input-lg"
                    name="email"
                    placeholder="E-Mail"
                    onChange={this.handleEmailChange}
                    value={this.state.email}
                    disabled={this.props.disabled}
                />
            </div>
            <PasswordInput
                onChange={this.handlePasswordChange}
                onEnter={() => {}}
                prefix="signup-"
                classNames={{
                    label: "sr-only",
                    input: "input-lg"
                }}
            />
            <button className="btn btn-lg btn-primary btn-block"
                    onClick={this.handleSignUpClick}
                    disabled={this.props.disabled}>
                Sign up
            </button>
        </form>
    }
}
