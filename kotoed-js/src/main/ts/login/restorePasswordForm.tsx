import * as React from "react"
import {render} from "react-dom";

import "sass/kotoed-bootstrap/bootstrap.sass";
import "sass/login.sass"
import "sass/util.sass"

import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import {ChangeEvent, MouseEvent} from "react";
import {changePassword, resetPassword} from "./remote";
import {Kotoed} from "../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {ErrorMessages} from "./util";
import {RedirectToRoot} from "../code/containers/CodeReviewContainer";
import {Redirect} from "react-router";
import {PasswordErrors, PasswordInput} from "../views/components/PasswordInput";

type LocalErrors = {
    emptyUsername: boolean
    emptyPassword: boolean
    emptyPassword2: boolean
    passwordsDoNotMatch: boolean
    illegalCombination: boolean
}

interface RestorePasswordFormState {
    username: string,
    password: string,
    password2: string,
    done: boolean
}

interface RestorePasswordFormProps {
    secret: string
}

class RestorePasswordForm extends ComponentWithLocalErrors<RestorePasswordFormProps, RestorePasswordFormState, LocalErrors> {

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptyUsername: "Please enter username",
        emptyPassword: "Please enter password",
        emptyPassword2: "Please re-enter password",
        passwordsDoNotMatch: "Passwords do not match",
        illegalCombination: "Your password change request has expired. Please reset your password again"
    };

    // This field does not directly affect rendering
    private passwordErrors: PasswordErrors = {
        emptyPassword: false,
        emptyPassword2: false,
        passwordsDoNotMatch: false,
    };


    constructor(props: RestorePasswordFormProps) {
        super(props);
        this.state = {
            username: "",
            password: "",
            password2: "",
            done: false,
            localErrors: {
                emptyUsername: false,
                emptyPassword: false,
                emptyPassword2: false,
                passwordsDoNotMatch: false,
                illegalCombination: false,
            }
        }
    }

    handleUsernameChange = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            username: event.target.value
        });

        this.unsetError("emptyUsername");
        this.unsetError("illegalCombination");
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

    handleSignUpClick = (event: MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
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

        if (ok) {
            let secret = this.props.secret;

            changePassword(secret, this.state.username, this.state.password).then(
                _ => this.setState({ done: true })
            ).catch(
                e => this.setError("illegalCombination")
            );
        }

    };

    render() {
        const {localErrors} = this.state;
        if(!this.state.done) {
            return <div><form className="form-signin">
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
                        onClick={this.handleSignUpClick}>
                    Change my password!
                </button>
            </form></div>
        } else return <div>
            <div className="alert alert-success">
                Your password has been reset successfully
            </div>
            <a className="btn btn-lg btn-primary btn-block"
               href={UrlPattern.Auth.Index}>
                Back to login page
            </a>
        </div>

    }
}

let secret = document.getElementById("restore-password-app")!.getAttribute("data-secret");

if(secret)
    render (
    <div className="login-wrapper"><RestorePasswordForm secret={secret}/></div>,
        document.getElementById("restore-password-app")
    );
