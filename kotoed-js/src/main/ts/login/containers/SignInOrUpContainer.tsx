import * as queryString from "query-string"

import SignInOrUp, {SignInOrUpCallbacks, SignInOrUpProps} from "../components/SignInOrUp";
import {SignInOrUpState} from "../state";
import {connect, Dispatch} from "react-redux";
import {fetchOAuthProviders, performSignIn, performSignUp, resetErrors} from "../actions";
import {Kotoed} from "../../util/kotoed-api";
import {RouteComponentProps} from "react-router";

const mapStateToProps = function(store: SignInOrUpState,
                                 ownProps: RouteComponentProps<{}>): SignInOrUpProps {
    let qs = queryString.parse(ownProps.location.search);
    let provider = qs.andThenOAuthWith || undefined;
    let conflict = qs.conflict || undefined;

    return {
        signInErrors: store.signInErrors,
        signUpErrors: store.signUpErrors,
        disabled: store.disabled,
        oAuthProviders: store.oAuthProviders,
        oAuthAttempted: provider,
        oAuthConflict: conflict
    };
};

const mapDispatchToProps = function(dispatch: Dispatch<SignInOrUpState>,
                                    ownProps: RouteComponentProps<{}>): SignInOrUpCallbacks {
    let qs = queryString.parse(ownProps.location.search);
    let provider = qs.andThenOAuthWith || undefined;
    return {
        onSignIn: (username, password) => {
            dispatch(resetErrors({}));
            dispatch(performSignIn({
                username,
                password,
                oAuthProvider: provider
            }));
        },
        onSignUp: (username, password, email) => {
            dispatch(resetErrors({}));
            dispatch(performSignUp({
                username,
                password,
                email,
                oAuthProvider: provider
            }));
        },
        onTabSelect: () => {
            dispatch(resetErrors({}));
        },
        onStartOAuth: (provider) => {
            window.location.href = Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Auth.OAuthStart, {
                providerName: provider
            });
        },
        onMount: () => {
            if (!provider)
                dispatch(fetchOAuthProviders())
        }
    }
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(SignInOrUp);