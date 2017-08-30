import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Modal, SplitButton, MenuItem} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {eventBus, SoftError} from "../eventBus";
import {ChangeEvent, KeyboardEvent} from "react";

interface SubmissionCreateProps {
    onCreate: () => void
    projectId: number
    parentSubmission?: number
}


interface SubmissionCreateState {
    showModal: boolean
    remoteError?: string
    revision: string
}

// TODO try to make controlled version
export class SubmissionCreate extends React.Component<SubmissionCreateProps, SubmissionCreateState> {
    constructor(props: SubmissionCreateProps) {
        super(props);
        this.state = {
            showModal: false,
            revision: ""
        }
    }

    renderErrors() {
        if (this.state.remoteError === undefined)
            return null;

        return <div className="alert alert-danger">
            {this.state.remoteError}
        </div>;
    };


    showModal = () => {
        this.setState({showModal: true})
    };

    hideModal = () => {
        this.dismissRemoteError();
        this.setState({
            showModal: false,
            revision: ""
        });
    };

    dismissRemoteError = () => {
        this.setState({remoteError: undefined});
    };

    tryCreate = async (revision: string | null = null) => {
        if (revision !== null && revision.trim() === "")
            revision = null;

        try {
            await eventBus.send(Kotoed.Address.Api.Submission.Create, {
                revision: revision,
                projectId: this.props.projectId,
                parentSubmission: this.props.parentSubmission || null
            });
            this.props.onCreate();
            this.hideModal();
        } catch (error) {
            if (!(error instanceof SoftError))
                this.hideModal();
            else {
                this.setState({
                    remoteError: error.message
                });
            }
            throw error;
        }
    };



    handleEnter = (event: KeyboardEvent<FormControl>) => event.key === "Enter" && this.tryCreate(this.state.revision);

    render() {
        return <SplitButton
                bsSize="lg"
                bsStyle="success"
                title={this.props.parentSubmission === undefined ? "Submit" : "Resubmit"}
                onClick={() => this.tryCreate()} id="submit-dropdown">
                <MenuItem eventKey="specify-revision" onClick={this.showModal}>Specify revision</MenuItem>
                <Modal show={this.state.showModal} onHide={this.hideModal}>
                    <Modal.Header closeButton>
                        <Modal.Title>Create new submission</Modal.Title>
                    </Modal.Header>
                    <Modal.Body>
                        {this.renderErrors()}
                        <Form>
                            <FormGroup
                                controlId="revision">
                                <ControlLabel>Revision hash</ControlLabel>
                                <FormControl
                                    type="text"
                                    value={this.state.revision}
                                    placeholder="Revision hash"
                                    onChange={(e: ChangeEvent<any>)  => {
                                        this.setState({revision: e.target.value as string || ""})
                                    }}
                                    onKeyPress={this.handleEnter}
                                />
                                <FormControl.Feedback />
                            </FormGroup>
                        </Form>
                    </Modal.Body>
                    <Modal.Footer>
                        <Button bsStyle="success" onClick={() => this.tryCreate(this.state.revision)}>Create</Button>
                    </Modal.Footer>
                </Modal>
            </SplitButton>
            ;
    }
}