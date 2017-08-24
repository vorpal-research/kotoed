import * as React from "react"
import {ErrorMessages} from "../../login/util";
import {typedKeys} from "../../util/common";

interface WithLocalErrors<E> {
    localErrors: E
}

export abstract class ComponentWithLocalErrors<P, S, E> extends
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
}
