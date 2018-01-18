import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger, Row} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import * as Spinner from "react-spinkit"

import {DbRecordWrapper, isStatusFinal, WithVerificationData} from "../data/verification";
import snafuDialog from "../util/snafuDialog";
import {JumboProject, SubmissionToRead} from "../data/submission";

import {SearchTable} from "../views/components/search";
import {BloatDenizen, Denizen, Profile} from "../data/denizen";

class DenizenComponent extends React.PureComponent<BloatDenizen> {
    constructor(props: BloatDenizen) {
        super(props);
    }

    linkify = (text: string | undefined): JSX.Element => {
        if (!text)
            return <span>&mdash;</span>;
        else
            return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Profile.Index, {id: this.props.id})}>{text}</a>
    };

    render() {
        let profile: Profile = (this.props.profiles || [])[0] || {};
        return <tr>
            <td>{this.linkify(this.props.id.toString())}</td>
            <td>{this.linkify(this.props.denizenId)}</td>
            <td>{this.linkify(this.props.email)}</td>
            <td>{this.linkify(profile.firstName)}</td>
            <td>{this.linkify(profile.lastName)}</td>
            <td>{this.linkify(profile.groupId)}</td>
        </tr>
    }

}



class ProjectsSearch extends React.Component<{}> {
    constructor(props: {}) {
        super(props);
        this.state = {
            canCreateProject: false
        };
    }


    renderTable = (children: Array<JSX.Element>): JSX.Element => {
        return (
            <Table striped bordered condensed hover responsive>
                <thead>
                    <tr>
                        <th className="col-md-1">Id</th>
                        <th className="col-md-3">Username</th>
                        <th className="col-md-2">E-mail</th>
                        <th className="col-md-2">First name</th>
                        <th className="col-md-2">Last name</th>
                        <th className="col-md-2">Group</th>
                    </tr>
                </thead>
                <tbody>
                    {children}
                </tbody>
            </Table>)
    };

    render() {
        return (
            <SearchTable
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.Denizen.Search}
                countAddress={Kotoed.Address.Api.Denizen.SearchCount}
                wrapResults={this.renderTable}
                elementComponent={(key, c: BloatDenizen) => <DenizenComponent {...c} key={key} />}
            />
        );
    }
}

render(
    <ProjectsSearch/>,
    document.getElementById('denizen-search-app')
);
