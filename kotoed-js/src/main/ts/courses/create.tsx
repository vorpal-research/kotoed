import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Modal} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {eventBus, SoftError} from "../eventBus";
import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import {ErrorMessages} from "../login/util";

type LocalErrors = {
    emptyName: false
}

interface CourseCreateProps {
    onCreate: () => void
}

interface CourseCreateState {
    showModal: boolean
    remoteError?: string
    name: string
}

export class CourseCreate extends ComponentWithLocalErrors<CourseCreateProps, CourseCreateState, LocalErrors> {

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptyName: "Please enter course name"
    };


    constructor(props: CourseCreateProps) {
        super(props);
        this.state = {
            showModal: false,
            name: "",
            localErrors: {
                emptyName: false
            }
        }
    }

    getErrorMessages(): Array<string> {
        let messages = super.getErrorMessages();

        if (this.state.remoteError)
            messages.push(this.state.remoteError);

        return messages;
    };

    showModal = () => {
        this.setState({showModal: true})
    };

    hideModal = () => {
        this.dismissRemoteError();
        this.setState({name: ""});
        this.setState({showModal: false})
    };

    dismissRemoteError = () => {
        this.setState({remoteError: undefined});
    };

    handleSubmit = () => {
        if (this.state.name === "") {
            this.setError("emptyName");
            return
        }

        this.tryCreate();
    };


    tryCreate = async () => {
        try {
            await eventBus.send(Kotoed.Address.Api.Course.Create, {
                name: this.state.name
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

    render() {
        return <div>
            <Button bsSize="lg" bsStyle="success" onClick={this.showModal}>Create course</Button>
            <Modal show={this.state.showModal} onHide={this.hideModal}>
                <Modal.Header closeButton>
                    <Modal.Title>Create new course</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.renderErrors()}
                    <Form>
                        <FormGroup
                            controlId="course-name"
                            validationState={this.state.localErrors.emptyName ? "error" : undefined}>
                            <ControlLabel>Course name</ControlLabel>
                            <FormControl
                                type="text"
                                value={this.state.name}
                                placeholder="Name"
                                onChange={(e: any) => {
                                    this.unsetError("emptyName");
                                    this.setState({name: e.target.value as string || ""})
                                }}
                            />
                            <FormControl.Feedback />
                        </FormGroup>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="success" onClick={() => this.handleSubmit()}>Create</Button>
                </Modal.Footer>
            </Modal>
        </div>;
    }
}