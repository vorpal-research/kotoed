import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Thumbnail, Row, Col, Modal, Label} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {embeddedImage, imagePath} from "../images";
import {ChoosyByVerDataSearchTable, SearchTable} from "../views/components/search";
import {eventBus, isSnafu, SoftError} from "../eventBus";
import {fetchPermissions} from "./remote";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import Planks from "../views/components/Planks";
import {isStatusFinal, WithVerificationData} from "../data/verification";
import {Course, CourseToRead} from "../data/course";
import {CourseCreate} from "./create";

import "less/courses.less"

type CourseWithVer = CourseToRead & WithVerificationData

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
        if (this.props.verificationData.status === "Invalid" || this.props.state === "closed")
            return <Planks/>;
        else
            return null;
    };

    render() {
        let href = Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Course.Index, {
            id: this.props.id
        });
        let icon = embeddedImage(this.props.icon) || imagePath("kotoed3.png");
        return (
            <div className={"thumbnail-wrapper"}>
                {this.renderSpinner()}
                {this.renderPlanks()}
                <Thumbnail src={icon} alt="242x200">
                    <div className={"stretch"}/>
                    <h3 className="course-name">{this.props.name || <span className="text-danger">Unnamed</span>}</h3>
                    <p className="open-wrapper">
                        <Button
                            disabled={this.props.verificationData.status !== "Processed"}
                            href={href} bsSize="large" bsStyle="primary"
                            block>Open</Button>
                    </p>
                </Thumbnail>
            </div>
        )
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
        return <Row key={`row-${children[0].key}`} className="flex-row">
            {children.map((c, ix) => <Col xs={12} sm={6} md={4} lg={3} key={`col-${ix}`}>{c}</Col>)}
        </Row>
    };

    render() {
        return (
            <ChoosyByVerDataSearchTable
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.Course.Search}
                countAddress={Kotoed.Address.Api.Course.SearchCount}
                elementComponent={(key, c: CourseWithVer) => <CourseComponent {...c} key={key} />}
                group={{
                    by: Infinity,
                    using: this.renderRow
                }}
                toolbarComponent={this.toolbarComponent}
                makeBaseQuery={() => {
                    return {
                        withVerificationData: true,
                    }
                }}
            />
        );
    }
}

render(
    <CoursesSearch/>,
    document.getElementById('course-search-app')
);
