import * as React from "react";
import {SearchResult, SearchTable} from "./components/search";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {makeProjectIndexUrl, makeSubmissionIndexUrl} from "../util/url";

interface SubmissionInfo {
    id: number
    submissionId: number
    submission: Submission
}

interface Submission {
    id: number
    project: Project
}

interface Project {
    id: number
    name: string
    denizen: Denizen
}

interface Denizen {
    id: number
    denizenId: string
}

class SubmissionTagSearchResult extends React.PureComponent<{ submissionInfo: SubmissionInfo }> {

    constructor(props: { submissionInfo: SubmissionInfo }) {
        super(props);
    }

    render() {
        let subInfo = this.props.submissionInfo;
        return (
            <SearchResult>
                <div className="panel panel-default">
                    <div className="panel-heading">
                        <strong>
                            <a target="_blank"
                               href={makeSubmissionIndexUrl(subInfo.submissionId)}>
                                Submission #{subInfo.submissionId}&ensp;
                                by {subInfo.submission.project.denizen.denizenId}
                            </a>
                        </strong>
                    </div>
                    <div className="panel-body">
                        <a target="_blank"
                           href={makeSubmissionIndexUrl(subInfo.submissionId)}>
                            Submission #{subInfo.submissionId}
                        </a>&ensp;
                        in&ensp;
                        <a target="_blank"
                           href={makeProjectIndexUrl(subInfo.submission.project.id)}>
                            {subInfo.submission.project.name}
                        </a>&ensp;
                        by {subInfo.submission.project.denizen.denizenId}
                    </div>
                </div>
            </SearchResult>
        )
    }
}

class SearchableByTagsSubmissionTable extends React.PureComponent {
    render() {
        return (
            <SearchTable
                searchAddress={Kotoed.Address.Api.Submission.Tags.Search}
                countAddress={Kotoed.Address.Api.Submission.Tags.SearchCount}
                elementComponent={(key, submissionInfo: SubmissionInfo) =>
                    <SubmissionTagSearchResult
                        key={key} submissionInfo={submissionInfo}/>
                }
            />
        );
    }
}

render(
    <SearchableByTagsSubmissionTable/>,
    document.getElementById('container')
);
