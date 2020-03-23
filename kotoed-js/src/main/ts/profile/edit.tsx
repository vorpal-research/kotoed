import * as React from "react";
import {render} from "react-dom";

import "less/kotoed-bootstrap/bootstrap.less"
import {Kotoed} from "../util/kotoed-api";
import {sendAsync, setStateAsync} from "../views/components/common";
import {WithId} from "../data/common";
import UrlPattern = Kotoed.UrlPattern;
import {ChangeEvent, InputHTMLAttributes, MouseEvent} from "react";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import Address = Kotoed.Address;
import {ErrorMessages} from "../login/util";
import {fallThroughErrorHandler} from "../eventBus";
import {PasswordErrors, PasswordInput} from "../views/components/PasswordInput";
import {pick, typedKeys} from "../util/common";
import * as _ from "lodash";
import "less/profile.less"
import "less/modal.less"

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Profile.Edit, window.location.pathname) || new Map();
let userId = parseInt(params.get("id")) || -1;

interface EditableProfileInfo {
    id: number
    denizenId?: string
    email?: string
    oauth?: [string, string | null][]
    firstName?: string
    lastName?: string
    group?: string
    powerMode: boolean
    emailNotifications: boolean
}

interface EditablePasswordInfo {
    targetId: number
    initiatorPassword?: string
    newPassword?: string
}

interface ProfileComponentProps {
    denizen: EditableProfileInfo
}

let noErrors = {badEmail: false, passwordsDontMatch: false, emptyPassword: false, incorrectPassword: false};
type LocalErrors = typeof noErrors;

interface ProfileComponentState {
    denizen: EditableProfileInfo
    password: EditablePasswordInfo
    success: boolean
    disabled: boolean
}

export class ProfileComponent extends ComponentWithLocalErrors<ProfileComponentProps, ProfileComponentState, LocalErrors> {
    constructor(props: ProfileComponentProps) {
        super(props);
        this.state = {
            denizen: this.props.denizen,
            password: {
                targetId: this.props.denizen.id,
                initiatorPassword: "",
                newPassword: ""
            },
            disabled: false,
            success: false,
            localErrors: {...noErrors}
        }
    }

    // not the react way, but whatever
    shadowErrors: LocalErrors = {...noErrors};

    localErrorMessages: ErrorMessages<LocalErrors> = {
        badEmail: "Incorrect email",
        passwordsDontMatch: "Passwords don't match",
        emptyPassword: "One of your password fields is empty",
        incorrectPassword: "Your current password is incorrect"
    };

    setError(error: keyof LocalErrors) {
        this.shadowErrors[error] = true;
    }

    unsetError(error: keyof LocalErrors) {
        this.shadowErrors[error] = false;
    }

    commitErrorsAsync = async (...picker: (keyof LocalErrors)[]) => {
        picker = picker || typedKeys(this.shadowErrors);
        let newErrors = {...this.state.localErrors, ...pick(this.shadowErrors, picker)};
        await setStateAsync(this, {localErrors: newErrors});
    };

    setDenizen = <K extends keyof EditableProfileInfo>(pick: Pick<EditableProfileInfo, K>) => {
        this.setState({success: false, denizen: Object.assign(this.state.denizen, pick)});
    };

    setPassword = <K extends keyof EditablePasswordInfo>(pick: Pick<EditablePasswordInfo, K>, errors: PasswordErrors) => {
        this.setState({success: false, password: Object.assign(this.state.password, pick)},
            () => {
                this.unsetError("emptyPassword");
                this.unsetError("passwordsDontMatch");
                this.unsetError("incorrectPassword");
                if (errors.emptyPassword || errors.emptyPassword2) {
                    this.setError("emptyPassword")
                }
                if (errors.passwordsDoNotMatch) {
                    this.setError("passwordsDontMatch");
                }
            });
    };

    bindInput = <K extends keyof EditableProfileInfo>(key: K) => (e: ChangeEvent<HTMLInputElement>) => {
        this.setDenizen({[key]: e.target.value} as Pick<EditableProfileInfo, K>)
    };

    bindPassword = <K extends keyof EditablePasswordInfo>(key: K) => (password: string, errors: PasswordErrors) => {
        this.setPassword({[key]: password} as Pick<EditablePasswordInfo, K>, errors)
    };

    bindCheckbox = <K extends keyof EditableProfileInfo>(key: K) => (e: ChangeEvent<HTMLInputElement>) => {
        this.setDenizen({[key]: e.target.checked} as Pick<EditableProfileInfo, K>)
    };

    onEmailChanged = (e: ChangeEvent<HTMLInputElement>) => {
        this.unsetError("badEmail");
        if (e.target.value && !e.target.checkValidity()) this.setError("badEmail");
        this.setDenizen({email: e.target.value || undefined})
    };

    onSave = async (e: MouseEvent<HTMLAnchorElement>) => {
        e.preventDefault();
        await this.commitErrorsAsync("badEmail");
        if (this.hasErrors()) return;

        await setStateAsync(this, {disabled: true, success: false});
        await sendAsync(Address.Api.Denizen.Profile.Update, this.state.denizen);
        await setStateAsync(this, {disabled: false, success: true});
    };

    onSavePassword = async (e: MouseEvent<HTMLAnchorElement>) => {
        e.preventDefault();
        await this.commitErrorsAsync("emptyPassword", "incorrectPassword", "passwordsDontMatch");
        if (this.hasErrors()) return;

        await setStateAsync(this, {disabled: true, success: false});

        try {
            await sendAsync(Address.Api.Denizen.Profile.UpdatePassword, this.state.password, fallThroughErrorHandler);
            await setStateAsync(this, {disabled: false, success: true})
        } catch (_) {
            this.setError("incorrectPassword");
            await this.commitErrorsAsync("emptyPassword", "incorrectPassword", "passwordsDontMatch");
            await setStateAsync(this, {disabled: false, success: false});
        }
    };

    renderSuccess = () => {
        if (this.state.success) {
            return <div className="alert alert-success">The profile updated successfully</div>;
        } else return null;
    };

    mkInputFor = (label: string, field: keyof EditableProfileInfo, type = "input", onChange = this.bindInput(field)) =>
        <div className="form-group">
            <label className="control-label col-sm-2"
                   htmlFor={`input-${field}`}>{label}</label>
            <div className="col-sm-10">
                <input
                    className="form-control"
                    id={`input-${field}`}
                    type={type}
                    value={this.state.denizen![field] as string || ""}
                    placeholder="not specified"
                    onChange={onChange}
                />
            </div>
        </div>;

    renderCheckBox = () => {
        const field = "emailNotifications";
        return <div className="form-group">
            <span className="col-sm-2"/>
            <div className="col-sm-10">
                <div className="checkbox">
                    <label htmlFor={`input-${field}`}>
                        <input
                            id={`input-${field}`}
                            type="checkbox"
                            value={field}
                            placeholder="not specified"
                            onChange={this.bindCheckbox(field)}
                            defaultChecked={this.state.denizen.emailNotifications}
                        />
                        Receive notifications using this email
                    </label>
                </div>
            </div>
        </div>;
    };

    renderBody = () => {
        return <div className="panel">
            <div className="panel-heading">
                <div className="row">
                    <div className="col-sm-12 text-center">
                        <h2>{this.props.denizen!.denizenId}</h2>
                    </div>
                </div>
            </div>
            <div className={`panel-body`}>
                <form className={`form-horizontal ${this.state.disabled ? "disabled" : ""}`}>
                    <div className="text-center">
                        {this.renderSuccess()}
                        {this.renderErrors()}
                    </div>
                    {this.mkInputFor("Email", "email", "email", this.onEmailChanged)}
                    {this.renderCheckBox()}
                    {this.mkInputFor("First name", "firstName")}
                    {this.mkInputFor("Last name", "lastName")}
                    {this.mkInputFor("Group #", "group")}
                    <div className="form-group">
                        <div className="text-center">
                            <a className="btn btn-default" onClick={this.onSave}>Save</a>
                        </div>
                    </div>
                    <PasswordInput
                        setPassword={false}
                        prefix="initiator-"
                        onChange={this.bindPassword("initiatorPassword")}
                        onEnter={() => {
                        }}
                        classNames={{
                            label: "control-label col-sm-2",
                            inputWrapper: "col-sm-10"
                        }}
                        label="Your password"
                        placeholder="not specified"
                    />
                    <PasswordInput
                        setPassword={true}
                        prefix="target-"
                        onChange={this.bindPassword("newPassword")}
                        onEnter={() => {
                        }}
                        classNames={{
                            label: "control-label col-sm-2",
                            inputWrapper: "col-sm-10"
                        }}
                        label="New password"
                        placeholder="not specified"
                        placeholderRepeat="not specified"
                    />
                    <div className="form-group">
                        <div className="text-center">
                            <a className="btn btn-default" onClick={this.onSavePassword}>Change password</a>
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
