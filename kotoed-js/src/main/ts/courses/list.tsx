import * as React from "react";
import {Button, Thumbnail, Row, Col} from "react-bootstrap";
import {doNothing} from "../util/common";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {Course} from "./data";
import {imagePath} from "../images";
import {SearchTable} from "../views/components/search";

class CourseComponent extends React.PureComponent<Course> {
    constructor(props: Course) {
        super(props);

        this.state = {}
    }

    render() {
        return (
            <Thumbnail src={imagePath("kotoed3.png")} alt="242x200">
                <h3>{this.props.name}</h3>
                <p>
                    <Button href={Kotoed.UrlPattern.NotImplemented} bsSize="large" bsStyle="primary" block>Open</Button>
                </p>
            </Thumbnail>
        )
    }

}

class CoursesSearch extends React.PureComponent {

    renderRow = (children: Array<JSX.Element>): JSX.Element => {
        return <Row key={`row-${children[0].key}`}>
            {children.map((c, ix) => <Col md={3} key={`col-${ix}`}>{c}</Col>)}
        </Row>
    };

    render() {
        return (
            <SearchTable
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.Course.Search}
                countAddress={Kotoed.Address.Api.Course.SearchCount}
                elementComponent={(key, c: Course) => <CourseComponent {...c} key={key} />}
                group={{
                    by: 4,
                    using: this.renderRow
                }}
            />
        );
    }
}

render(
    <CoursesSearch/>,
    document.getElementById('course-search-app')
);
