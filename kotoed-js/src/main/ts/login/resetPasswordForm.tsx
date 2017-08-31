import * as React from "react"
import {render} from "react-dom";

import "less/kotoed-bootstrap/bootstrap.less"
import "less/login.less"

import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import {ChangeEvent, MouseEvent} from "react";
import {resetPassword} from "./remote";
import {Kotoed} from "../util/kotoed-api";
import UrlPattern = Kotoed.UrlPattern;
import {ErrorMessages} from "./util";

type LocalErrors = {
    emptyUsername: boolean
    badEmail: boolean
    illegalCombination: boolean
}

interface ResetPasswordFormState {
    username: string,
    email: string,
    done: boolean
}

interface ResetPasswordFormProps {
}

class ResetPasswordForm extends ComponentWithLocalErrors<ResetPasswordFormProps, ResetPasswordFormState, LocalErrors> {

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptyUsername: "Please enter username",
        badEmail: "Incorrect email address",
        illegalCombination: "Incorrect username/email combination"
    };

    constructor(props: ResetPasswordFormProps) {
        super(props);
        this.state = {
            username: "",
            email: "",
            done: false,
            localErrors: {
                emptyUsername: false,
                badEmail: false,
                illegalCombination: false,
            }
        }
    }

    eMailField: HTMLInputElement;

    handleUsernameChange = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            username: event.target.value
        });

        this.unsetError("emptyUsername");
        this.unsetError("illegalCombination");
    };

    handleEmailChange = (event: ChangeEvent<HTMLInputElement>) => {
        this.setState({
            email: event.target.value
        });

        this.unsetError("badEmail");
        this.unsetError("illegalCombination");
    };

    handleResetClick = (event: MouseEvent<HTMLButtonElement>) => {
        event.preventDefault();
        let ok = true;
        if (this.state.username === "") {
            this.setError("emptyUsername");
            ok = false;
        }

        if (!this.eMailField.checkValidity()) {
            this.setError("badEmail");
            ok = false;
        }

        if (ok) {
            resetPassword(this.state.username, this.state.email).then(
                _ => this.setState({done: true})
            ).catch(
                e => this.setError("illegalCombination")
            );
        }

    };

    render() {
        const {localErrors} = this.state;
        if (!this.state.done)
            return <div><form className="form-signin">
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
                    />
                </div>
                <button className="btn btn-lg btn-primary btn-block"
                        onClick={this.handleResetClick}>
                    Reset my password!
                </button>
            </form></div>;
        else return <div>
            <div className="alert alert-success">
                An email to {this.state.email} has been sent
            </div>
            <a className="btn btn-lg btn-primary btn-block"
               href={UrlPattern.Auth.Index}>
                Back to login page
            </a>
        </div>
    }
}

render(
    <div className="login-wrapper"><ResetPasswordForm/></div>,
    document.getElementById("reset-password-app"));
