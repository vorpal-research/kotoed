import * as React from "react"
import SignInForm, {SignInFormProps} from "./SignInForm";
import {Tab, TabList, TabPanel, Tabs} from "react-tabs";
import 'react-tabs/style/react-tabs.less';
import SignUpForm from "./SignUpForm";
import {RouteComponentProps} from "react-router";

import "less/util.less"

export interface SignInOrUpProps {
    signInErrors: Array<string>
    signUpErrors: Array<string>
    disabled: boolean
    oAuthProviders: Array<string>
    oAuthAttempted?: string
}

export interface SignInOrUpCallbacks {
    onSignIn: (login: string, password: string) => void
    onSignUp: (login: string, password: string, email: string|null) => void
    onTabSelect: (index: number) => void
    onStartOAuth: (provider: string) => void
    onMount: () => void
}
export default class SignInOrUp extends React.Component<RouteComponentProps<{}> & SignInOrUpProps & SignInOrUpCallbacks> {

    componentDidMount() {
        this.props.onMount()
    }

    renderOAuthMessage() {
        return this.props.oAuthAttempted &&
            <div className="alert alert-success">
                <b>{this.props.oAuthAttempted}</b>{" "}
                did recognize you, but we didn't. Please introduce yourself and then we'll continue with{" "}
                <b>{this.props.oAuthAttempted}</b>{" "}
            </div>
    }

    render() {
        return (
            <div>
                {this.renderOAuthMessage()}
                <Tabs
                    defaultIndex={0}
                    onSelect={index => this.props.onTabSelect(index)}
                    selectedTabClassName="active"
                    disabledTabClassName="disabled">
                    <TabList className="nav nav-pills nav-justified">
                        <Tab className="" disabled={this.props.disabled}><a>Sign In</a></Tab>
                        <Tab className="" disabled={this.props.disabled}><a>Sign Up</a></Tab>
                    </TabList>
                    <div className="vspace-10"/>
                    <TabPanel>
                        <SignInForm
                            errors={this.props.signInErrors}
                            disabled={this.props.disabled}
                            onSignIn={this.props.onSignIn}
                            oAuthProviders={this.props.oAuthProviders}
                            onStartOAuth={this.props.onStartOAuth}
                        />
                    </TabPanel>
                    <TabPanel>
                        <SignUpForm
                            errors={this.props.signUpErrors}
                            disabled={this.props.disabled}
                            onSignUp={this.props.onSignUp}
                        />
                    </TabPanel>
                </Tabs>
            </div>)

    }
}
