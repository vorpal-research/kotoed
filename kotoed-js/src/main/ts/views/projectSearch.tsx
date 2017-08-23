import * as React from "react";
import {SearchResult, SearchTable} from "./components/search";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {makeCodeReviewCodePath} from "../util/url";

interface Project {
    id: number
    name: string
    repoUrl: string
    repoType: "git" | "mercurial"
    courseName: string
    denizenId: string
    lastSubmissionId: number
}

class ProjectSearchResult extends React.PureComponent<{ project: Project }> {

    constructor(props: { project: Project }) {
        super(props);
    }

    render() {
        let project = this.props.project;
        return (
            <SearchResult>
                <div className="panel panel-default">
                    <div className="panel-heading">
                        <strong>
                            <a target="_blank" href={makeCodeReviewCodePath(project.lastSubmissionId, "")}>
                                {project.denizenId}/{project.name}
                            </a>
                            {' '} in course {project.courseName}
                        </strong>
                    </div>
                    <div className="panel-body">
                        <p>
                            Repository: <a href={project.repoUrl}>{project.repoUrl}</a>
                        </p>
                    </div>
                </div>
            </SearchResult>
        )
    }

};

class SearchableProjectTable extends React.PureComponent {
    render() {
        return (
            <SearchTable
                searchAddress={Kotoed.Address.Api.Project.Search}
                countAddress={Kotoed.Address.Api.Project.SearchCount}
                elementComponent={(key, p: Project) => <ProjectSearchResult key={key} project={p} />}
            />
        );
    }
}

render(
    <SearchableProjectTable/>,
    document.getElementById('container')
);
