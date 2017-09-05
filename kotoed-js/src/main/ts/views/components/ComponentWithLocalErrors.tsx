import * as React from "react"
import {ErrorMessages} from "../../login/util";
import {typedKeys} from "../../util/common";

interface WithLocalErrors<E> {
    localErrors: E
}

export abstract class ComponentWithLocalErrors<P, S, E extends {[name: string]: boolean}> extends
    React.Component<P, S & WithLocalErrors<E>> {

    localErrorMessages: ErrorMessages<E>;

    constructor(props: P) {
        super(props);
    }

    getErrorMessages(): Array<string> {
        let messages = Array<string>();

        typedKeys(this.state.localErrors).forEach( key => {
            if (this.state.localErrors[key]) {
                messages.push(this.localErrorMessages[key])
            }
        });

        return messages;
    };

    renderErrors() {
        let messages = this.getErrorMessages();
        if (messages.length === 0)
            return null;

        if(messages.length === 1)
            return <div className="alert alert-danger">{messages[0]}</div>;

        return <div className="alert alert-danger">
            <ul>
                {this.getErrorMessages().map((msg, ix) => <li key={ix}>{msg}</li>)}
            </ul>
        </div>;
    };

    setError(error: keyof E) {
        this.setState((oldState) => {
            const newLE = Object.assign({}, oldState.localErrors);
            newLE[error] = true;
            return {
                localErrors: newLE
            }
        });
    }

    unsetError(error: keyof E) {
        this.setState((oldState) => {
            const newLE = Object.assign({}, oldState.localErrors);
            newLE[error] = false;
            return {
                localErrors: newLE
            }
        });
    }

    unsetAllErrors() {
        let empty: Partial<E> = {};


        typedKeys(this.state.localErrors).forEach((key) => {
            empty[key] = false;
        });

        this.setState({
            localErrors: empty as E
        })
    }

    hasErrors() {
        let errors = this.state.localErrors;
        return typedKeys(this.state.localErrors).reduce((acc, k) => acc || errors[k], false)
    }
}
