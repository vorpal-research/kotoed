import * as React from "react";

import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {fetchPermissions} from "./remote";
import {
    SearchTableWithVerificationData
} from "../views/components/searchWithVerificationData";
import snafuDialog from "../util/snafuDialog";
import "less/projects.less"
import {renderSubmissionTable} from "./table";
import {SubmissionComponent, SubmissionWithVer} from "./SubmissionComponent";
import {SubmissionCreate} from "./create";

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
        if (this.state.canCreateSubmission)
            return <SubmissionCreate onCreate={redoSearch} projectId={id_}/>;
        else
            return null;
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
                wrapResults={renderSubmissionTable}
                elementComponent={(key, c: SubmissionWithVer) => <SubmissionComponent {...c} key={key} pendingIsAvailable={false}/>}
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
