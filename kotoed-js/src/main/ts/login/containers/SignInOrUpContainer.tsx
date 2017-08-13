
import SignInOrUp, {SignInOrUpCallbacks, SignInOrUpProps} from "../components/SignInOrUp";
import {SignInOrUpState} from "../state";
import {connect, Dispatch} from "react-redux";
import {performSignIn, performSignUp, resetErrors} from "../actions";

const mapStateToProps = function(store: SignInOrUpState): SignInOrUpProps {
    return {
        signInError: store.signInError,
        signUpError: store.signUpError,
        disabled: store.disabled
    };
};

const mapDispatchToProps = function(dispatch: Dispatch<SignInOrUpState>): SignInOrUpCallbacks {
    return {
        onSignIn: (username, password) => {
            dispatch(resetErrors({}));
            dispatch(performSignIn({
                username,
                password
            }));
        },
        onSignUp: (username, password, email) => {
            dispatch(resetErrors({}));
            dispatch(performSignUp({
                username,
                password,
                email
            }));
        },
        onTabSelect: () => {
            dispatch(resetErrors({}));
        }
    }
};

export default connect(
    mapStateToProps,
    mapDispatchToProps
)(SignInOrUp);