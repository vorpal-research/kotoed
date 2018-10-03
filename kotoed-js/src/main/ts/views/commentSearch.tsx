import * as React from "react";
import {makeCommentPath} from "../util/url";
import {SearchResult, SearchTable} from "./components/search";
import CommentComponent from "../code/components/CommentComponent";
import {doNothing} from "../util/common";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {CommentToRead} from "../data/comment";
import {truncateString} from "../util/string";


class CommentSearchResult extends React.PureComponent<{ comment: CommentToRead }> {
    constructor(props: { comment: CommentToRead }) {
        super(props);

        this.state = {}
    }

    renderOverlay = () => {
        let comment = this.props.comment;
        return (
            <a target="_blank" href={makeCommentPath(comment)}>
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
                    processing={false}
                    commentTemplates={[]}
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
