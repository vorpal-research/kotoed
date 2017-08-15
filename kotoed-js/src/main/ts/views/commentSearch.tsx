import * as React from "react";
import {Component} from "react";
import {render} from "react-dom";
import {connect} from "react-redux";
import {Kotoed} from "../util/kotoed-api";
import {eventBus} from "../eventBus";
import CommentComponent from "../code/components/CommentComponent";
import {Pagination} from "react-bootstrap";
import {CODE_REVIEW_BASE_ADDR, makeCodePath} from "../util/url";
import {doNothing} from "../util/common";
import * as _ from "lodash";

import "less/commentSearch.less"
import "less/kotoed-bootstrap/bootstrap.less";
import {BaseCommentToRead, CommentToRead} from "../code/remote/comments";

function truncateString(str: string, len: number): string {
    if(str.length <= len) return str;
    else {
        let truc = (len - 3)/2;
        return str.substr(0, truc) + "..." + str.substr(str.length - truc, truc)
    }
}

export interface SearchBarProps {
    onChange: (state: SearchBarState) => void
}

export interface SearchBarState {
    openOnly: boolean,
    text: string
}

class SearchBar extends React.Component<SearchBarProps, SearchBarState> {
    constructor(props: SearchBarProps) {
        super(props);
        this.state = {
            text: "",
            openOnly: true
        };
    }

    private notify = _.debounce(() => this.props.onChange(this.state), 300);

    updateText = (newText: string) => {
        this.setState({text: newText}, this.notify);
    };

    onOpenOnlyChange = (event: any) => {
        this.setState({openOnly: event.target.value}, this.notify);
    };

    render() {
        return (
            <div className="search-bar">
                <div className="input-group">
                    <input className="search-query form-control"
                           placeholder="Search"
                           type="text"
                           value={this.state.text}
                           onChange={(e) => this.updateText(e.target.value)}
                    />
                </div>
            </div>
        );
    }
}

export interface SearchableCommentTableProps {

}

export interface SearchableCommentTableState extends SearchBarState {
    currentPage: number,
    pageCount: number,
    currentComments: Array<CommentToRead>
}

const PAGESIZE = 20;

class SearchableCommentTable extends React.Component<SearchableCommentTableProps, SearchableCommentTableState> {
    constructor(props: SearchableCommentTableProps) {
        super(props);

        this.state = {
            text: "",
            openOnly: false,
            currentPage: 0,
            pageCount: 0,
            currentComments: []
        };
    }

    private queryCount = () => {
        let message = {
            text: this.state.text
        };
        eventBus.send(Kotoed.Address.Api.Submission.Comment.SearchCount, message)
            .then((resp: any) => this.setState({ pageCount: Math.ceil(resp.count / PAGESIZE) }))
    };

    private queryComments = () => {
        let message = {
            text: this.state.text,
            currentPage: this.state.currentPage,
            pageSize: PAGESIZE
        };
        eventBus.send(Kotoed.Address.Api.Submission.Comment.Search, message)
            .then<any>(resp => this.setState({currentComments: resp as any[]}))
    };

    onPageChanged = (page: number) => {
        this.setState({currentPage: page}, this.queryComments)
    };

    onSearchStateChanged = (state: SearchBarState) => {
        this.setState({ currentPage: 0, ...state}, () => { this.queryCount(); this.queryComments() });
    };

    render() {
        return (
            <div>
                <SearchBar
                    onChange={this.onSearchStateChanged}
                />
                <Pagination
                    prev
                    next
                    first
                    last
                    ellipsis
                    boundaryLinks
                    items={this.state.pageCount}
                    maxButtons={5}
                    activePage={this.state.currentPage + 1}
                    onSelect={(e: any) => this.onPageChanged(e as number - 1)}
                />
                {this.state.currentComments.map((comment, index) =>
                    <div className="panel search-preview"
                         key={"comment" + index}
                    >
                        <div className="search-preview-overlay">
                            <div style={{ right:0, bottom:0, position:"absolute"}} >
                                <a target="_blank" href={
                                    `${CODE_REVIEW_BASE_ADDR}${makeCodePath(comment.submissionId, comment.sourcefile, comment.sourceline)}`
                                }><strong>At: {truncateString(comment.sourcefile, 25)} &raquo;</strong></a>
                            </div>
                        </div>
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
                    </div>
                )
                }
            </div>
        );
    }
}

render(
    <SearchableCommentTable/>,
    document.getElementById('container')
);