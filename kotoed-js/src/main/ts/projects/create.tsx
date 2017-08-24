import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Modal, Radio} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {eventBus, SoftError} from "../eventBus";
import {RepoType} from "../data/project";
import {ChangeEvent, KeyboardEvent} from "react";
import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import {ErrorMessages} from "../login/util";

type LocalErrors = {
    emptyName: boolean
    emptyUrl: boolean,
    badUrl: boolean
}

interface ProjectCreateProps {
    onCreate: () => void
    courseId: number
}


interface ProjectCreateState {
    showModal: boolean
    remoteError?: string
    name: string
    repoType: RepoType
    repoUrl: string
}

export class ProjectCreate extends ComponentWithLocalErrors<ProjectCreateProps, ProjectCreateState, LocalErrors> {
    private urlField: HTMLInputElement;

    constructor(props: ProjectCreateProps) {
        super(props);
        this.state = {
            showModal: false,
            name: "",
            repoType: "git",
            repoUrl: "",
            localErrors: {
                emptyName: false,
                emptyUrl: false,
                badUrl: false
            }
        }
    }

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptyName: "Please enter project name",
        emptyUrl: "Please enter repo URL",
        badUrl: "Please enter valid repo URL"
    };

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
        this.unsetAllErrors();
        this.setState({
            name: "",
            repoUrl: "",
            repoType: "git",
            showModal: false
        });
    };

    dismissRemoteError = () => {
        this.setState({remoteError: undefined});
    };

    handleSubmit = () => {
        let ok = true;
        if (this.state.name === "") {
            this.setError("emptyName");
            ok = false;
        }

        if (this.state.repoUrl === "") {
            this.setError("emptyUrl");
            ok = false;
        }

        if (!this.urlField.checkValidity()) {
            this.setError("badUrl");
            ok = false;
        }

        if (ok)
            this.tryCreate();
    };

    tryCreate = async () => {
        try {
            await eventBus.send(Kotoed.Address.Api.Project.Create, {
                name: this.state.name,
                repoType: this.state.repoType,
                repoUrl: this.state.repoUrl,
                courseId: this.props.courseId
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

    handleRepoTypeChange = (changeEvent: ChangeEvent<any>) => {
        this.setState({
            repoType: changeEvent.target.value as RepoType
        });
    };

    handleEnter = (event: KeyboardEvent<FormControl>) => event.key === "Enter" && this.handleSubmit();

    render() {
        return <div>
            <Button bsSize="lg" bsStyle="success" onClick={this.showModal}>Create project</Button>
            <Modal show={this.state.showModal} onHide={this.hideModal}>
                <Modal.Header closeButton>
                    <Modal.Title>Create new project</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.renderErrors()}
                    <Form>
                        <FormGroup
                            controlId="project-name"
                            validationState={this.state.localErrors.emptyName ? "error" : undefined}>
                            <ControlLabel>Project name</ControlLabel>
                            <FormControl
                                type="text"
                                value={this.state.name}
                                placeholder="Name"
                                onChange={(e: ChangeEvent<any>)  => {
                                    this.unsetError("emptyName");
                                    this.setState({name: e.target.value as string || ""})
                                }}
                                onKeyPress={this.handleEnter}
                            />
                            <FormControl.Feedback />
                        </FormGroup>
                        <FormGroup controlId="project-repo-type">
                            <ControlLabel>Repo type</ControlLabel>
                            <Radio
                                name="repo-type"
                                value="git"
                                checked={this.state.repoType === "git"}
                                onChange={this.handleRepoTypeChange}>
                                Git
                            </Radio>
                            {" "}
                            <Radio
                                name="repo-type"
                                value="mercurial"
                                checked={this.state.repoType === "mercurial"}
                                onChange={this.handleRepoTypeChange}>
                                Mercurial
                            </Radio>
                        </FormGroup>
                        <FormGroup
                            controlId="repo-url"
                            validationState={
                                this.state.localErrors.emptyUrl  || this.state.localErrors.badUrl ?
                                    "error" :
                                    undefined}>
                            <ControlLabel>Repo URL</ControlLabel>
                            <FormControl
                                type="url"
                                value={this.state.repoUrl}
                                placeholder="URL"
                                inputRef={ref => this.urlField = ref!}
                                onChange={(e: ChangeEvent<any>)  => {
                                    this.unsetError("emptyUrl");
                                    this.unsetError("badUrl");
                                    this.setState({repoUrl: e.target.value as string || ""})
                                }}
                                onKeyPress={this.handleEnter}
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