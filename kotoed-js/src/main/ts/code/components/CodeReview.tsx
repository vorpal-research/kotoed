import * as React from "react";
import {Button, Panel, Label} from "react-bootstrap";

import FileReview from "./FileReview";
import FileTree from "./FileTree";
import {Comment, FileComments, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import {NodePath} from "../state/blueprintTree";
import {FileNode} from "../state/filetree";
import {List} from "immutable";
import {LostFoundComments} from "./LostFoundComments";
import {CommentAggregate} from "../remote/comments";
import {UNKNOWN_FILE, UNKNOWN_LINE} from "../remote/constants";
import {ScrollTo} from "../state";
import SpinnerWithVeil from "../../views/components/SpinnerWithVeil";
import {BaseCommentToRead} from "../../data/comment";
import {DbRecordWrapper} from "../../data/verification";
import {SubmissionToRead} from "../../data/submission";
import VerificationDataAlert from "../../views/components/VerificationDataAlert";
import AggregatesLabel from "../../views/AggregatesLabel";
import {FileForms, ReviewForms} from "../state/forms";
import {ReviewAnnotations} from "../state/annotations";
import {CommentTemplates} from "../remote/templates";

export interface CodeReviewProps {
    submissionId: number
    submission: DbRecordWrapper<SubmissionToRead> | null

    annotations: ReviewAnnotations
    commentTemplates: CommentTemplates

    editor: {
        loading: boolean
        value: string
        file: string
        comments: FileComments
    }

    fileTree: {
        loading: boolean
        path: NodePath
        root: FileNode
    }

    lostFound: {
        loading: boolean
        comments: LostFoundCommentsState
        aggregate: CommentAggregate
    }

    capabilities: {
        canPostComment: boolean
        whoAmI: string
    }

    forms: {
        forms: ReviewForms
    }
}

interface CodeReviewPropsFromRouting {
    show: "lost+found" | "code"
    scrollTo: ScrollTo
}

interface ToggleState {
    isToggleOn: boolean
    value: string
    name: string
}

export interface CodeReviewCallbacks {
    editor: {
        onMarkerExpand: (file: string, lineNumber: number) => void
        onMarkerCollapse: (file: string, lineNumber: number) => void
    }

    comments: {
        onCommentSubmit: (file: string, line: number, text: string) => void
        onCommentUnresolve: (filePath: string, lineNumber: number, id: number) => void
        onCommentResolve: (filePath: string, lineNumber: number, id: number) => void
        onHiddenExpand: (file: string, lineNumber: number, comments: List<Comment>) => void
        onCommentEmphasize: (file: string, lineNumber: number, commentId: number) => void
        onCommentEdit: (file: string, line: number, id: number, newText: string) => void
        makeOriginalLink?: (comment: BaseCommentToRead) => string | undefined
    }

    fileTree: {
        onDirExpand: (path: number[]) => void;
        onDirCollapse: (path: number[]) => void;
        onFileSelect: (path: number[]) => void;
    }

    lostFound: {
        onSelect: () => void
    }
}

export type CodeReviewPropsAndCallbacks = CodeReviewProps & CodeReviewCallbacks

export default class CodeReview extends React.Component<CodeReviewPropsAndCallbacks & CodeReviewPropsFromRouting, ToggleState> {
    constructor() {
        super();
        this.state = {
            isToggleOn: true,
            value: "none",
            name: "Show menu"
        };
        this.handleClick = this.handleClick.bind(this);
    }

    handleClick() {
        this.setState({
            isToggleOn: !this.state.isToggleOn,
            value: this.state.isToggleOn ? "flex" : "none",
            name: this.state.isToggleOn ? "Hide menu" : "Show menu"
        })
    }

    makeOriginalLinkOrUndefined = (comment: BaseCommentToRead) => {
        if (this.props.comments.makeOriginalLink && comment.submissionId !== this.props.submissionId)
            return this.props.comments.makeOriginalLink(comment)
    };

    renderRightSide = () => {
        switch (this.props.show) {
            case "lost+found":
                return <LostFoundComments comments={this.props.lostFound.comments}
                                          onCommentUnresolve={(id) => this.props.comments.onCommentUnresolve(UNKNOWN_FILE, UNKNOWN_LINE, id)}
                                          onCommentResolve={(id) => this.props.comments.onCommentResolve(UNKNOWN_FILE, UNKNOWN_LINE, id)}
                                          onCommentEmphasize={(comments) => this.props.comments.onCommentEmphasize(UNKNOWN_FILE, UNKNOWN_LINE, comments)}
                                          onExpand={(comments) => this.props.comments.onHiddenExpand(UNKNOWN_FILE, UNKNOWN_LINE, comments)}
                                          onEdit={(id, newText) => this.props.comments.onCommentEdit(UNKNOWN_FILE, UNKNOWN_LINE, id, newText)}
                                          makeOriginalLink={this.props.comments.makeOriginalLink}
                                          loading={this.props.lostFound.loading}
                                          scrollTo={this.props.scrollTo}
                                          commentTemplates={this.props.commentTemplates}
                />;
            case "code":
                if (this.props.editor.file !== "")
                    return <FileReview canPostComment={this.props.capabilities.canPostComment}
                                       value={this.props.editor.value}
                                       height="100%"
                                       comments={this.props.editor.comments}
                                       filePath={this.props.editor.file}
                                       onSubmit={(line, text) => this.props.comments.onCommentSubmit(this.props.editor.file, line, text)}
                                       onCommentResolve={this.props.comments.onCommentResolve}
                                       onCommentUnresolve={this.props.comments.onCommentUnresolve}
                                       onMarkerExpand={this.props.editor.onMarkerExpand}
                                       onMarkerCollapse={this.props.editor.onMarkerCollapse}
                                       onHiddenExpand={this.props.comments.onHiddenExpand}
                                       onCommentEmphasize={this.props.comments.onCommentEmphasize}
                                       onCommentEdit={this.props.comments.onCommentEdit}
                                       whoAmI={this.props.capabilities.whoAmI}
                                       commentTemplates={this.props.commentTemplates}
                                       scrollTo={this.props.scrollTo}
                                       loading={this.props.editor.loading}
                                       makeOriginalCommentLink={this.makeOriginalLinkOrUndefined}
                                       forms={this.props.forms.forms.get(this.props.editor.file) || FileForms()}
                                       codeAnnotations={this.props.annotations.get(this.props.editor.file) || []}
                    />;
                else
                    return <div className="no-file-chosen">
                        <div>Please choose file</div>
                    </div>
        }
    };

    renderFileTreeVeil = () => {
        if (this.props.fileTree.loading)
            return <SpinnerWithVeil/>;
        else
            return null;
    };


    renderReview = () => {
        return <div>
            <div className="row">
                <button className="btn btn-default" onClick={this.handleClick}>{this.state.name}</button>
            </div>
            <div className="vspace-10">
            </div>
            <div className="row code-review">
                <div className="col-xs-6 col-sm-4 col-md-3 col-lg-2 col-xl-2" id="code-review-left"
                     style={{display: this.state.value}}>
                    {this.renderFileTreeVeil()}
                    <div className="code-review-tree-container">
                        <FileTree root={this.props.fileTree.root}
                                  onDirExpand={this.props.fileTree.onDirExpand}
                                  onDirCollapse={this.props.fileTree.onDirCollapse}
                                  onFileSelect={this.props.fileTree.onFileSelect}
                                  loading={this.props.fileTree.loading}
                                  lostFoundAggregate={this.props.lostFound.aggregate}
                        />
                        <div className="lost-found-button-container">
                            <Button bsStyle="warning" className="lost-found-button"
                                    onClick={this.props.lostFound.onSelect}>
                                Lost + Found {" "}
                                <AggregatesLabel {...this.props.lostFound.aggregate}/>
                            </Button>
                        </div>
                    </div>
                </div>
                <div>
                    <AggregatesLabel {...this.props.lostFound.aggregate} />
                </div>
                <div className="col-xs-8 col-sm-9 col-md-9 col-lg-10 col-xl-10" id="code-review-right">
                    {this.renderRightSide()}
                </div>
            </div>
        </div>
    };

    shouldRenderReview = () => this.props.submission && this.props.submission.verificationData.status === "Processed";


    render() {
        if (!this.props.submission) {
            return <div className="row code-review" style={{
                position: "relative"
            }}>
                <SpinnerWithVeil/>
            </div>
        }

        if (!this.shouldRenderReview()) {
            return <div style={{
                flex: "1 0 auto"
            }}>
                <VerificationDataAlert
                    makeString={(obj: DbRecordWrapper<SubmissionToRead>) => `Submission #${obj.record.id}`}
                    obj={this.props.submission}
                    gaveUp={false}/>
            </div>
        }

        return this.renderReview()
    }
}
