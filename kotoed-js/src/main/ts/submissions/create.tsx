import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Modal, SplitButton, MenuItem} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {eventBus, SoftError} from "../eventBus";
import {ChangeEvent, KeyboardEvent} from "react";
import {CreateRequest, SubmissionToRead} from "../data/submission";
import {DbRecordWrapper} from "../data/verification";
import {sendAsync} from "../views/components/common";
import "less/newSubmission.less"

interface SubmissionCreateProps {
    onCreate: (newId: number) => void
    projectId: number
    parentSubmission?: number
}


interface SubmissionCreateState {
    showRevisionModal: boolean
    showAreYouSureModal: boolean,
    suggestedParent?: number,
    remoteError?: string
    revision: string
    disabled: boolean
}


export class SubmissionCreate extends React.Component<SubmissionCreateProps, SubmissionCreateState> {
    constructor(props: SubmissionCreateProps) {
        super(props);
        this.state = {
            showRevisionModal: false,
            showAreYouSureModal: false,
            revision: "",
            suggestedParent: undefined,
            disabled: false
        }
    }

    renderErrors() {
        if (this.state.remoteError === undefined)
            return null;

        return <div className="alert alert-danger">
            {this.state.remoteError}
        </div>;
    };


    showRevisionModal = () => {
        this.setState({showRevisionModal: true})
    };

    hideRevisionModal = () => {
        this.dismissRemoteError();
        this.setState({
            showRevisionModal: false
        });
    };

    showAreYouSureModal = (suggestedParent: number | undefined) => {
        this.setState({
            showAreYouSureModal: true,
            suggestedParent
        })
    };

    hideAreYouSureModal = () => {
        this.setState({
            showAreYouSureModal: false,
            suggestedParent: undefined
        });
    };

    dismissRemoteError = () => {
        this.setState({remoteError: undefined});
    };

    dismissState = () => {
        this.setState({
            revision: "",
            suggestedParent: undefined
        })
    };


    checkExistingOrTryCreate = async (revision: string | undefined = undefined) => {
        if (this.props.parentSubmission)
            await this.tryCreate(revision);

        let existing = await sendAsync(Kotoed.Address.Api.Submission.List, {
            pageSize: 2,
            currentPage: 0,
            text: "",
            find: {
                projectId: this.props.projectId,
                stateIn: [
                    "open",
                    "pending"
                ]
            }
        });

        if (existing.length == 0)
            await this.tryCreate();
        else {
            let suggestion = existing.length == 1 ? existing[0].id : null;
            this.hideRevisionModal();
            this.showAreYouSureModal(suggestion);
        }
    };

    tryCreate = async (revision: string | undefined = undefined, parentSubmission: number | undefined = undefined) => {
        if (revision != null && revision.trim() === "")
            revision = undefined;

        try {
            let newSub = await sendAsync(Kotoed.Address.Api.Submission.Create, {
                revision: revision,
                projectId: this.props.projectId,
                parentSubmissionId: parentSubmission || this.props.parentSubmission
            });
            this.hideRevisionModal();
            this.hideAreYouSureModal();
            this.setState({
                disabled: true
            });
            this.dismissState();
            this.props.onCreate(newSub.record.id);
        } catch (error) {
            this.dismissState();
            this.setState({
                disabled: false
            });
            if (!(error instanceof SoftError)) {
                this.hideRevisionModal();
                this.hideAreYouSureModal();
            }
            else {
                this.setState({
                    remoteError: error.message
                });
            }
            throw error;
        }
    };



    handleEnter = (event: KeyboardEvent<FormControl>) =>
        event.key === "Enter" && !this.state.disabled && this.checkExistingOrTryCreate(this.state.revision);

    render() {
        return <SplitButton
                    disabled={this.state.disabled}
                    bsStyle="success"
                    title={this.props.parentSubmission === undefined ? "Submit" : "Resubmit"}
                    onClick={() => this.checkExistingOrTryCreate()} id="submit-dropdown">
            <MenuItem eventKey="specify-revision" onClick={this.showRevisionModal}>Specify revision</MenuItem>
            <Modal show={this.state.showRevisionModal} onHide={() => {
                this.hideRevisionModal();
                this.dismissState();
            }}>
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
                        <Button disabled={this.state.disabled}
                                bsStyle="success"
                                onClick={() => this.checkExistingOrTryCreate(this.state.revision)}>Create</Button>
                    </Modal.Footer>
                </Modal>
            <Modal show={this.state.showAreYouSureModal} onHide={() => {
                this.hideAreYouSureModal();
                this.dismissState();
            }}>
                <Modal.Header closeButton>
                    <Modal.Title>Are you sure?</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <p>
                        It seems like you already have an open submission. You probably should resubmit it instead
                        of creating a new one.
                    </p>
                </Modal.Body>
                <Modal.Footer>
                    {this.state.suggestedParent &&
                    <Button disabled={this.state.disabled} bsStyle="success" onClick={() => this.tryCreate(this.state.revision, this.state.suggestedParent)}>
                        {`Resubmit to #${this.state.suggestedParent}`}
                    </Button>}
                    <Button disabled={this.state.disabled} bsStyle="danger" onClick={() => this.tryCreate(this.state.revision)}>
                        Submit anyway
                    </Button>
                    <div className="vspace-10">
                    </div>
                    <Button onClick={() => {
                        this.hideAreYouSureModal();
                        this.dismissState();
                    }}>
                        Cancel
                    </Button>

                </Modal.Footer>
            </Modal>
            </SplitButton>;
    }
}