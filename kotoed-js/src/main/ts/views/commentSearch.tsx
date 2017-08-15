import * as React from "react";
import {Component} from "react";
import {render} from "react-dom";
import {connect} from "react-redux";
import {Kotoed} from "../util/kotoed-api";
import {eventBus} from "../eventBus";
import CommentComponent from "../code/components/CommentComponent";
import {CODE_REVIEW_BASE_ADDR, makeCodePath} from "../util/url";

import "less/commentSearch.less"
import "less/kotoed-bootstrap/bootstrap.less";
import * as _ from "lodash";


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


export interface PaginationProps {
    onPageChange: (page: number) => void
    lastPage: number
}

export interface PaginationState {
    page: number
}

class Pagination extends React.Component<PaginationProps, PaginationState> {
    constructor(props: PaginationProps) {
        super(props);

        this.state = {page: 0};
    }

    prevClassName = () => this.state.page === 0 ? 'disabled' : '';
    nextClassName = () => this.state.page === this.props.lastPage ? 'disabled' : '';

    private notify = () => this.props.onPageChange(this.state.page);

    next = () => {
        this.setState({page: this.state.page + 1}, this.notify);
    };
    prev = () => {
        this.setState({page: this.state.page === 0 ? 0 : this.state.page - 1}, this.notify);
    };

    render() {
        return (
            <nav aria-label="...">
                <ul className="pager">
                    <li className={this.prevClassName()}><a href="#" onClick={this.prev}>Previous</a></li>
                    <li className={this.nextClassName()}><a href="#" onClick={this.next}>Next</a></li>
                </ul>
            </nav>
        )
    }
}

export interface SearchableCommentTableProps {
}

export interface SearchableCommentTableState extends SearchBarState {
    currentPage: number,
    currentComments: any[]
}

class SearchableCommentTable extends React.Component<SearchableCommentTableProps, SearchableCommentTableState> {
    constructor(props: SearchableCommentTableProps) {
        super(props);

        this.state = {
            text: "",
            openOnly: false,
            currentPage: 0,
            currentComments: []
        };
    }

    private queryEB = () => {
        let message = {
            text: this.state.text,
            currentPage: this.state.currentPage,
            pageSize: 20
        };
        eventBus.send(Kotoed.Address.Api.Submission.Comment.Search, message)
            .then<any>(resp => this.setState({currentComments: resp as any[]}))
    };

    onPageChanged = (page: number) => {
        this.setState({currentPage: page}, this.queryEB)
    };

    onSearchStateChanged = (state: SearchBarState) => {
        this.setState(state, this.queryEB);
    };

    render() {
        return (
            <div>
                <SearchBar
                    onChange={this.onSearchStateChanged}
                />
                <Pagination
                    lastPage={-1}
                    onPageChange={this.onPageChanged}
                />
                {this.state.currentComments.map((comment, index) =>
                    <div className="panel search-preview"
                         key={"comment" + index}
                         onClick={_ => {
                             //let link = `${CODE_REVIEW_BASE_ADDR}${makeCodePath(comment.submissionId, comment.sourcefile, comment.sourceline)}`
                             //window.open(link, "_blank")
                         }}
                    >
                        <div className="search-preview-overlay">
                            <div style={{ right:0, bottom:0, position:"absolute"}} >
                                <a target="_blank" href={
                                    `${CODE_REVIEW_BASE_ADDR}${makeCodePath(comment.submissionId, comment.sourcefile, comment.sourceline)}`
                                }><strong>At: {comment.sourcefile} &raquo;</strong></a>
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
                            onUnresolve={(_ => {
                            })}
                            onResolve={(_ => {
                            })}
                            notifyEditorAboutChange={(() => {
                            })}
                            onEdit={(_ => {
                            })}/>
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