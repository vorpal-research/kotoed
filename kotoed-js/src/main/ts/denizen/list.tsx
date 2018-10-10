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

import "less/projects.less"
import {SearchCallback, SearchTable} from "../views/components/search";
import {BloatDenizen, Denizen, Profile} from "../data/denizen";
 import {makeGroup} from "../util/denizen";

class DenizenComponent extends React.PureComponent<BloatDenizen & {cb: SearchCallback}> {
    constructor(props: BloatDenizen & {cb: SearchCallback}) {
        super(props);
    }

    handleSearchableClick = (word: string) => {
        const searchState = this.props.cb();
        // XXX: we can do better
        if(!searchState.oldKey.includes(word)) {
            searchState.toggleSearch(searchState.oldKey + " " + word)
        }
    };

    makeGroupLink = () => {
        const mg = makeGroup(this.props);
        return mg && <a style={{cursor: "pointer"}}
                        onClick={() => this.handleSearchableClick(mg)}>{mg}</a>
    };

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
            <td>{this.makeGroupLink()}</td>
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
                elementComponent={(key, c: BloatDenizen, cb: SearchCallback) =>
                    <DenizenComponent {...c} key={key} cb={cb}/>}
            />
        );
    }
}

render(
    <ProjectsSearch/>,
    document.getElementById('denizen-search-app')
);
