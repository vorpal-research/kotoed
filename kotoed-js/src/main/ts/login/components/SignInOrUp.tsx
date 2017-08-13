import * as React from "react"
import SignInForm, {SignInFormProps} from "./SignInForm";
import {Tab, TabList, TabPanel, Tabs} from "react-tabs";
import 'react-tabs/style/react-tabs.less';
import SignUpForm from "./SignUpForm";

export interface SignInOrUpProps {
    signInError?: string
    signUpError?: string
    disabled: boolean
}

export interface SignInOrUpCallbacks {
    onSignIn: (login: string, password: string) => void
    onSignUp: (login: string, password: string, email: string|null) => void
    onTabSelect: (index: number) => void
}
export default class SignInOrUp extends React.Component<SignInOrUpProps & SignInOrUpCallbacks> {
    render() {
        return (
            <Tabs
                defaultIndex={0}
                onSelect={index => this.props.onTabSelect(index)}
                selectedTabClassName="active"
                disabledTabClassName="disabled"
            >
                <TabList className="nav nav-pills nav-justified">
                    <Tab className="" disabled={this.props.disabled}><a>Sign In</a></Tab>
                    <Tab className="" disabled={this.props.disabled}><a>Sign Up</a></Tab>
                </TabList>
                <br/>
                <TabPanel>
                    <SignInForm
                        error={this.props.signInError}
                        disabled={this.props.disabled}
                        onSignIn={this.props.onSignIn}
                    />
                </TabPanel>
                <TabPanel>
                    <SignUpForm
                        error={this.props.signUpError}
                        disabled={this.props.disabled}
                        onSignUp={this.props.onSignUp}
                    />
                </TabPanel>
            </Tabs>)

    }
}
