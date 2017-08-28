import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import * as Spinner from "react-spinkit"
import * as moment from "moment";

import {fetchPermissions} from "./remote";
import {
    SearchTableWithVerificationData
} from "../views/components/searchWithVerificationData";
import {WithVerificationData} from "../data/verification";
import snafuDialog from "../util/snafuDialog";
import {JumboProject, SubmissionToRead} from "../data/submission";

import "less/projects.less"
import {makeSubmissionResultsUrl, makeSubmissionReviewUrl} from "../util/url";

type SubmissionWithVer = SubmissionToRead & WithVerificationData

class SubmissionComponent extends React.PureComponent<SubmissionWithVer> {
    constructor(props: SubmissionWithVer) {
        super(props);
    }

    linkToSubDetails = (text: string): JSX.Element => {
        if (this.props.verificationData.status === "Processed")
            return <a href={Kotoed.UrlPattern.NotImplemented}>{text}</a>;
        else
            return <span className={"grayed-out"}>{text}</span>
    };

    private readonly invalidTooltip = <Tooltip id="tooltip">This submission is invalid</Tooltip>;
    private readonly closedTooltip = <Tooltip id="tooltip">This submission is closed</Tooltip>;

    private readonly spinner =  <Spinner name="three-bounce" color="gray" fadeIn="none" className="display-inline"/>;
    private readonly exclamation = <OverlayTrigger placement="right" overlay={this.invalidTooltip}>
        <span className="text-danger">
            <Glyphicon glyph="exclamation-sign"/>
        </span>
    </OverlayTrigger>;
    private readonly lock = <OverlayTrigger placement="right" overlay={this.closedTooltip}>
        <span className="text-danger">
            <Glyphicon glyph="lock"/>
        </span>
    </OverlayTrigger>;


    private renderIcon = (): JSX.Element | null => {
        let {status} = this.props.verificationData;
        let {state} = this.props;
        switch (status) {
            case "NotReady":
            case "Unknown":
                return this.spinner;
            case "Invalid":
                return this.exclamation;
            case "Processed":
                switch (state) {
                    case "pending":
                        return this.spinner;
                    case "invalid":
                        return this.exclamation;
                    case "open":
                        return null;
                    case "obsolete":
                        return null; // Should not happen here
                    case "closed":
                        return this.lock;
                    default:
                        return null;
                }
        }
    };
    render() {
        return <tr>
            <td>{this.linkToSubDetails(this.props.id.toString())}{" "}{this.renderIcon()}</td>
            <td>{this.linkToSubDetails(moment(this.props.datetime).format('LLLL'))}</td>
            <td>{this.props.revision}</td>
            <td><a href={makeSubmissionResultsUrl(this.props.id)}>Results</a></td>
            <td><a href={makeSubmissionReviewUrl(this.props.id)}>Review</a></td>
        </tr>
    }

}

class SubmissionList extends React.Component<{}, {canCreateSubmission: boolean}> {


    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateSubmission: false
        };
    }

    componentDidMount() {
        fetchPermissions(id_).then((perms) =>
            this.setState({canCreateSubmission: perms.createSubmission})
        );
    }

    toolbarComponent = (redoSearch: () => void) => {
        return null
    };


    renderTable = (children: Array<JSX.Element>): JSX.Element => {
        return (
            <Table striped bordered condensed hover responsive>
                <thead>
                <tr>
                    <th>Id</th>
                    <th>Created at</th>
                    <th>Revision</th>
                    <th>Results</th>
                    <th>Review</th>
                </tr>
                </thead>
                <tbody>
                {children}
                </tbody>
            </Table>)
    };

    render() {
        return (
            <SearchTableWithVerificationData
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.Submission.List}
                countAddress={Kotoed.Address.Api.Submission.ListCount}
                withSearch={false}
                makeBaseQuery={() => {
                    return {
                        find: {
                            projectId: id_
                        }
                    }
                }}
                wrapResults={this.renderTable}
                elementComponent={(key, c: SubmissionWithVer) => <SubmissionComponent {...c} key={key} />}
                toolbarComponent={this.toolbarComponent}
            />
        );
    }
}

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Project.Index, window.location.pathname);

if (params == null) {
    snafuDialog();
    throw new Error("Cannot resolve course id")
}

let id = params.get("id");

if (id === undefined ) {
    snafuDialog();
    throw new Error("Cannot resolve course id")
}

let id_ = parseInt(id);

if (isNaN(id_)) {
    snafuDialog();
    throw new Error("Cannot resolve course id")
}

render(
    <SubmissionList/>,
    document.getElementById('submission-list-app')
);
