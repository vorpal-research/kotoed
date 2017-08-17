import * as React from "react";
import {makeCommentPath} from "../util/url";
import {CommentToRead} from "../code/remote/comments";
import {SearchResult, SearchTable} from "./components/search";
import CommentComponent from "../code/components/CommentComponent";
import {doNothing} from "../util/common";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";

function truncateString(str: string, len: number): string {
    if (str.length <= len) return str;
    else {
        let truc = (len - 3) / 2;
        return str.substr(0, truc) + "..." + str.substr(str.length - truc, truc)
    }
}

class CommentSearchResult extends React.PureComponent<{ comment: CommentToRead }> {
    constructor(props: { comment: CommentToRead }) {
        super(props);

        this.state = {}
    }

    renderOverlay = () => {
        let comment = this.props.comment;
        return (
            <a target="_blank" href={makeCommentPath(comment.submissionId, comment.sourcefile, comment.sourceline)}>
                <strong>At: {truncateString(comment.sourcefile, 25)} &raquo;</strong>
            </a>
        );
    };

    render() {
        let comment = this.props.comment;
        return (
            <SearchResult overlayContents={this.renderOverlay()}>
                <CommentComponent
                    denizenId={comment.denizenId}
                    id={comment.id}
                    authorId={comment.authorId}
                    datetime={comment.datetime}
                    state={comment.state}
                    submissionId={comment.submissionId}
                    text={comment.text}
                    sourcefile={comment.sourcefile}
                    sourceline={comment.sourceline}
                    canStateBeChanged={false}
                    canBeEdited={false}
                    collapsed={false}
                    onUnresolve={doNothing}
                    onResolve={doNothing}
                    notifyEditorAboutChange={doNothing}
                    onEdit={doNothing}
                />
            </SearchResult>
        )
    }

}

class SearchableCommentTable extends React.PureComponent {
    render() {
        return (
            <SearchTable
                searchAddress={Kotoed.Address.Api.Submission.Comment.Search}
                countAddress={Kotoed.Address.Api.Submission.Comment.SearchCount}
                elementComponent={(key, c: CommentToRead) => <CommentSearchResult key={key} comment={c} />}
            />
        );
    }
}

render(
    <SearchableCommentTable/>,
    document.getElementById('comment-search-app')
);
