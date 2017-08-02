import * as React from "react";
import {ChangeEvent} from "react";

interface CommentFormProps {
    onSubmit: (text: string) => void
}

interface CommentFormState {
    text: string
}

export class CommentForm extends React.Component<CommentFormProps, CommentFormState> {
    constructor(props: CommentFormProps) {
        super(props);
        this.state = {
            text: ""
        }
    }

    render() {
        return (<form>
                    <div className="form-group">
                        <label htmlFor="comment">Comment:</label>
                        <textarea
                            className="form-control"
                            rows={5}
                            id="comment"
                            value={this.state.text}
                            style={{
                                resize: "none"
                            }}
                            onChange={(event) => this.setState({text: event.target.value})}/>
                        <button type="button" className="btn btn-success"
                                onClick={() => this.props.onSubmit(this.state.text)}>
                            Send
                        </button>
                    </div>
                </form>);
    }
}