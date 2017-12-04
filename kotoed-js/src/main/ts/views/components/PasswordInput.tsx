import * as React from "react"
import {OverlayTrigger, Tooltip} from "react-bootstrap"
import {ChangeEvent, KeyboardEvent} from "react";

export interface PasswordErrors {
    emptyPassword: boolean
    emptyPassword2: boolean
    passwordsDoNotMatch: boolean
}

interface FieldClasses {
    formGroup?: string
    label?: string
    inputWrapper?: string
    input?: string
}

export interface PasswordInputProps {
    disabled?: boolean
    prefix?: string,
    setPassword?: boolean
    onChange: (password: string, errors: PasswordErrors) => void
    onEnter: () => void
    classNames?: FieldClasses
    classNamesRepeat?: FieldClasses,
    placeholder?: string,
    placeholderRepeat?: string
    label?: string
    labelRepeat?: string,
    error?: false,
    errorRepeat?: false
}

interface PasswordInputState {
    blindMode: boolean
    password: string,
    password2: string,
}

const defaultProps: Partial<PasswordInputProps> = {
    disabled: false,
    prefix: "",
    setPassword: true,
    classNames: {},
    classNamesRepeat: undefined,
    placeholder: "Password",
    placeholderRepeat: "Repeat password",
    label: "Password",
    labelRepeat: "Repeat Password"
};

export class PasswordInput extends React.Component<PasswordInputProps, PasswordInputState> {


    constructor(props: PasswordInputProps) {
        super(props);
        this.state = {
            blindMode: true,
            password: "",
            password2: ""
        }
    }

    private getProperty<T>(prop: keyof PasswordInputProps): T {
        let explicit = this.props[prop];

        if (explicit !== undefined)
            return explicit as T;

        return defaultProps[prop] as T;
    }

    private getStringProperty(prop: keyof PasswordInputProps): string {
        return this.getProperty<string>(prop) as string || ""
    }


    private getClassName(key: keyof FieldClasses, repeat: boolean = false): string {
        let classNames: FieldClasses | undefined = undefined;

        if (repeat)
            classNames = this.props.classNamesRepeat;

        if (classNames === undefined)
            classNames = this.props.classNames;

        if (classNames === undefined)
            classNames = defaultProps.classNames;  // Definitely

        return classNames![key] as string || "";
    }

    callOnChange = () => {
        this.props.onChange(this.state.password, {
            emptyPassword: this.state.password === "",
            emptyPassword2: this.getProperty('setPassword') && this.state.blindMode && this.state.password2 === "",
            passwordsDoNotMatch: this.getProperty('setPassword')
                && this.state.blindMode && (this.state.password !== this.state.password2)
        });
    };

    handlePasswordChange = (event: ChangeEvent<HTMLInputElement>) => {
        let password = event.target.value;
        this.setState({
            password
        }, () => {
            this.callOnChange();
        });
    };


    handlePassword2Change = (event: ChangeEvent<HTMLInputElement>) => {
        let password2 = event.target.value;
        this.setState({
            password2
        }, () => {
            this.callOnChange();
        });
    };

    getInputType = () => this.state.blindMode ? "password" : "text";

    toggleBlindMode = () => this.setState((oldState) => {
        return {
            blindMode: !oldState.blindMode,
            password2: ""
        }
    }, () => {
        this.callOnChange();
    });

    getEyeState = () => this.state.blindMode? "open" : "close";

    renderRepeat = (): JSX.Element | null => {
        if (!this.getProperty("setPassword"))
            return null;
        if (!this.state.blindMode)
            return null;

        return <div className={`form-group ${this.getClassName("formGroup", true)} ${this.props.errorRepeat ? "has-error" : ""}`}>
            <label
                className={this.getClassName("label", true)}
                htmlFor={`${this.getStringProperty("prefix")}password-repeat`}
            >
                {this.getStringProperty("labelRepeat")}
            </label>
            <div className={this.getClassName("inputWrapper", true)}>
                <input type="password"
                       disabled={this.props.disabled}
                       className={`form-control ${this.getClassName("input", true)}`}
                       placeholder={this.getStringProperty("placeholderRepeat")}
                       value={this.state.password2}
                       onChange={this.handlePassword2Change}
                       name={`${this.getStringProperty("prefix")}password-repeat`}
                       onKeyPress={this.handleEnter}
                />
            </div>
        </div>

    };

    handleEnter = (event: KeyboardEvent<HTMLInputElement>) => event.key === "Enter" && this.props.onEnter();


    render() {
        return <div>
            {/*Always having a password field with stars to make browser always offer to store password*/}
            <input
                style={{display: "none"}}
                type="password"
                value={this.state.password}
                name={`${this.getStringProperty("prefix")}shadow-password`}
            />
            <div className={`form-group ${this.getClassName("formGroup")} ${this.props.error ? "has-error" : ""}`}>
                <label
                    className={this.getClassName("label")}
                    htmlFor={`${this.getStringProperty("prefix")}password`}
                >
                    {this.getStringProperty("label")}
                    </label>
                <div className={this.getClassName("inputWrapper")}>
                    <div className="has-feedback">
                        <input type={this.getInputType()}
                               disabled={this.props.disabled}
                               className={`form-control ${this.getClassName("input")}`}
                               placeholder={this.getStringProperty("placeholder")}
                               value={this.state.password}
                               onChange={this.handlePasswordChange}
                               name={`${this.getStringProperty("prefix")}password`}
                               onKeyPress={this.handleEnter}
                        />
                        <OverlayTrigger placement="top" overlay={<Tooltip id="tooltip">
                            {`${this.state.blindMode ? "Show" : " Hide"} password`}
                        </Tooltip>}>
                            <span
                                onClick={this.toggleBlindMode}
                                className={`glyphicon glyphicon-eye-${this.getEyeState()} form-control-feedback grayed-out clickable-feedback`}/>
                        </OverlayTrigger>
                    </div>
                </div>
            </div>
            {this.renderRepeat()}
        </div>
    }
}