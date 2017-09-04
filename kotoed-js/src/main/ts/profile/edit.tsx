import * as React from "react";
import {render} from "react-dom";

import "less/kotoed-bootstrap/bootstrap.less"
import {Denizen, WithDenizen} from "../data/denizen";
import {Kotoed} from "../util/kotoed-api";
import {eventBus} from "../eventBus";
import {sendAsync, setStateAsync} from "../views/components/common";
import {DbRecordWrapper} from "../data/verification";
import {WithId} from "../data/common";
import ComponentWithLoading, {LoadingProperty} from "../views/components/ComponentWithLoading";
import UrlPattern = Kotoed.UrlPattern;
import SocialButton from "../login/components/SocialButton";
import {ChangeEvent, InputHTMLAttributes, MouseEvent} from "react";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import {Simulate} from "react-dom/test-utils";
import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import Address = Kotoed.Address;
import {ErrorMessages} from "../login/util";
import {typedKeys} from "../util/common";

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Profile.Edit, window.location.pathname) || new Map();
let userId = parseInt(params.get("id")) || -1;

interface EditableProfileInfo {
    id: number
    denizenId?: string
    email?: string
    oauth?: [string, number | null][]
    firstName?: string
    lastName?: string
    group?: string
}

interface ProfileComponentProps {
    denizen: EditableProfileInfo
}

let noErrors = { badEmail: false };
type LocalErrors = typeof noErrors

interface ProfileComponentState {
    denizen: EditableProfileInfo
    pendingErrors: LocalErrors
    disabled: boolean
}

export class ProfileComponent extends ComponentWithLocalErrors<ProfileComponentProps, ProfileComponentState, LocalErrors> {
    constructor(props: ProfileComponentProps) {
        super(props);
        this.state = {
            denizen: this.props.denizen,
            disabled: false,
            localErrors: noErrors,
            pendingErrors: noErrors
        }
    }

    localErrorMessages: ErrorMessages<LocalErrors> = {
        badEmail: "Incorrect email"
    };

    setPendingError = (error: keyof LocalErrors) =>
        this.setState({pendingErrors: { ...this.state.pendingErrors, [error]: true }});

    setError(error: keyof LocalErrors) {
        super.setError(error);
        this.setState({pendingErrors: { ...this.state.pendingErrors, [error]: false }});
    }

    unsetError(error: keyof LocalErrors) {
        super.unsetError(error);
        this.setState({pendingErrors: { ...this.state.pendingErrors, [error]: false }});
    }

    commitErrorsAsync = async () => {
        setStateAsync(this, { localErrors: this.state.pendingErrors, pendingErrors: noErrors })
    };

    onEmailChanged = (e: ChangeEvent<HTMLInputElement>) => {
        this.unsetError("badEmail");
        if(e.target.checkValidity()) {
            this.setState({denizen: {...this.state.denizen, email: e.target.value}})
        } else this.setPendingError("badEmail");
    };
    onFirstNameChanged = (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({denizen: {...this.state.denizen, firstName: e.target.value}})
    };
    onLastNameChanged = (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({denizen: {...this.state.denizen, lastName: e.target.value}})
    };
    onGroupChanged = (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({denizen: {...this.state.denizen, group: e.target.value}})
    };
    onSave = async (e: MouseEvent<HTMLAnchorElement>) => {
        e.preventDefault();
        await this.commitErrorsAsync();
        if(this.state.localErrors.badEmail) return;
        await sendAsync(Address.Api.Denizen.Profile.Update, this.state.denizen)
    };

    renderBody = () => {
        return <div className="panel">
            <div className="panel-heading">
                <h2>{this.props.denizen!.denizenId}</h2>
            </div>
            { this.renderErrors() }
            <div className={`panel-body ${this.state.disabled? "disabled":""}`}>
                <form className="form-horizontal">
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputEmail">Email</label>
                        <div className="col-sm-10">
                            <input
                                className="form-control"
                                id="inputEmail"
                                type="email"
                                value={this.props.denizen!.email}
                                placeholder="not specified"
                                onChange={this.onEmailChanged}
                            />
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputFirstName">First name</label>
                        <div className="col-sm-10">
                            <input
                                className="form-control"
                                id="inputFirstName"
                                value={this.props.denizen!.firstName}
                                placeholder="not specified"
                                onChange={this.onFirstNameChanged}
                            />
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputLastName">Last name</label>
                        <div className="col-sm-10">
                            <input
                                className="form-control"
                                id="inputLastName"
                                value={this.props.denizen!.lastName}
                                placeholder="not specified"
                                onChange={this.onLastNameChanged}
                            />
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputGroup">Group #</label>
                        <div className="col-sm-10">
                            <input
                                className="form-control"
                                id="inputGroup"
                                value={this.props.denizen!.group}
                                placeholder="not specified"
                                onChange={this.onGroupChanged}
                            />
                        </div>
                    </div>
                    <div className="form-group">
                        <div className="col-sm-offset-2 col-sm-10">
                            <a className="btn btn-default" onClick={this.onSave}>Save</a>
                        </div>
                    </div>
                    <div className="form-group">
                        <div className="col-sm-offset-2 col-sm-10">
                            <a href={UrlPattern.Auth.ResetPassword} className="btn btn-default">Password reset</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    };

    render() {
        return <div className="row">{this.renderBody()}</div>
    }
}

interface ProfileWrapperState {
    denizen?: EditableProfileInfo
}

class ProfileWrapper extends React.Component<{}, ProfileWrapperState> {
    constructor(props: {}) {
        super(props);
        this.state = {}
    }

    loadDenizen = async () => {
        let profile =
            await sendAsync<WithId, EditableProfileInfo>(Kotoed.Address.Api.Denizen.Profile.Read,
                {id: userId});
        this.setState({denizen: profile})
    };

    componentDidMount() {
        this.loadDenizen()
    }

    render() {
        return this.state.denizen ?
            <ProfileComponent denizen={this.state.denizen}/> : <SpinnerWithVeil/>;
    }
}

render(
    <ProfileWrapper/>,
    document.getElementById('profile-app')
);
