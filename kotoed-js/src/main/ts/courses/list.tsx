import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Thumbnail, Row, Col, Modal, Label} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {Course} from "./data";
import {imagePath} from "../images";
import {SearchTable} from "../views/components/search";
import {eventBus, isSnafu, SoftError} from "../eventBus";
import {fetchPermissions} from "./remote";
import {
    isStatusFinal,
    SearchTableWithVerificationData,
    WithVerificationDataResp
} from "../views/components/searchWithVerificationData";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import Planks from "../views/components/Planks";

type CourseWithVer = Course & WithVerificationDataResp

class CourseComponent extends React.PureComponent<CourseWithVer> {
    constructor(props: CourseWithVer) {
        super(props);

        this.state = {}
    }

    renderSpinner = () => {
        if (!isStatusFinal(this.props.verificationData.status))
            return <SpinnerWithVeil/>;
        else
            return null;
    };

    renderPlanks = () => {
        if (this.props.verificationData.status === "Invalid")
            return <Planks/>;
        else
            return null;
    };

    render() {
        return (
            <div>
                {this.renderSpinner()}
                {this.renderPlanks()}
                <Thumbnail src={imagePath("kotoed3.png")} alt="242x200">
                    <h3>{this.props.name || <span className="text-danger">Unnamed</span>}</h3>
                    <p>
                        <Button
                            disabled={this.props.verificationData.status !== "Processed"}
                            href={Kotoed.UrlPattern.NotImplemented} bsSize="large" bsStyle="primary"
                            block>Open</Button>
                    </p>
                </Thumbnail>
            </div>
        )
    }

}

interface CourseCreateProps {
    onCreate: () => void
}

interface CourseCreateState {
    showModal: boolean
    error?: string
    name: string
}

class CourseCreate extends React.Component<CourseCreateProps, CourseCreateState> {


    constructor(props: CourseCreateProps) {
        super(props);
        this.state = {
            showModal: false,
            name: ""
        }
    }

    showModal = () => {
        this.setState({showModal: true})
    };

    hideModal = () => {
        this.dismissError();
        this.setState({name: ""});
        this.setState({showModal: false})
    };

    dismissError = () => {
        this.setState({error: undefined});
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
                    error: error.message
                });
            }
            throw error;
        }
    };

    renderError = () => this.state.error && <Alert bsStyle="danger">{this.state.error}</Alert>;

    render() {
        return <div>
            <Button bsSize="lg" bsStyle="success" onClick={this.showModal}>Create course</Button>
            <Modal show={this.state.showModal} onHide={this.hideModal}>
                <Modal.Header closeButton>
                    <Modal.Title>Create new course</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    {this.renderError()}
                    <Form>
                        <FormGroup
                            controlId="formBasicText">
                            <ControlLabel>Course name</ControlLabel>
                            <FormControl
                                type="text"
                                value={this.state.name}
                                placeholder="Name"
                                onChange={(e: any) =>
                                    this.setState({name: e.target.value as string || ""})}
                            />
                            <FormControl.Feedback />
                        </FormGroup>
                    </Form>
                </Modal.Body>
                <Modal.Footer>
                    <Button bsStyle="success" onClick={() => this.tryCreate()}>Create</Button>
                </Modal.Footer>
            </Modal>
        </div>;
    }
}

class CoursesSearch extends React.Component<{}, {canCreateCourse: boolean}> {


    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateCourse: false
        };
    }

    componentDidMount() {
        fetchPermissions().then((perms) =>
            this.setState({canCreateCourse: perms.createCourse})
        );
    }

    toolbarComponent = (redoSearch: () => void) => {
        if (this.state.canCreateCourse)
            return <CourseCreate onCreate={redoSearch}/>;
        else
            return null;
    };

    renderRow = (children: Array<JSX.Element>): JSX.Element => {
        return <Row key={`row-${children[0].key}`}>
            {children.map((c, ix) => <Col md={3} key={`col-${ix}`}>{c}</Col>)}
        </Row>
    };

    render() {
        return (
            <SearchTableWithVerificationData
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.Course.Search}
                countAddress={Kotoed.Address.Api.Course.SearchCount}
                elementComponent={(key, c: CourseWithVer) => <CourseComponent {...c} key={key} />}
                group={{
                    by: 4,
                    using: this.renderRow
                }}
                toolbarComponent={this.toolbarComponent}
            />
        );
    }
}

render(
    <CoursesSearch/>,
    document.getElementById('course-search-app')
);
