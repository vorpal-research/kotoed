import * as React from "react";
import {
    Button,
    Panel,
    Label,
    Modal,
    Form,
    FormGroup,
    ControlLabel,
    FormControl,
    Radio,
    SplitButton, MenuItem
} from "react-bootstrap";

import FileReview from "./FileReview";
import FileTree from "./FileTree";
import {Comment, FileComments, LostFoundComments as LostFoundCommentsState} from "../state/comments";
import {NodePath} from "../state/blueprintTree";
import {FileNode} from "../state/filetree";
import {List, Map} from "immutable";
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
import {DiffBase, DiffBaseType, FileDiffChange, FileDiffResult, RevisionInfo} from "../remote/code";

import "@fortawesome/fontawesome-free/less/fontawesome.less"
import "@fortawesome/fontawesome-free/less/solid.less"
import "@fortawesome/fontawesome-free/less/regular.less"
import {ChangeEvent} from "react";
import {DiffState} from "../state/diff";
import {Profile} from "../../data/denizen";

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
        canViewTags: boolean
        whoAmI: string
    }

    forms: {
        forms: ReviewForms
    }

    diff: DiffState
}

interface CodeReviewPropsFromRouting {
    show: "lost+found" | "code"
    scrollTo: ScrollTo
}

export interface CodeReviewCallbacks {
    editor : {
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

    diff: {
        onChangeDiffBase: (submissionId: number, diffBase: DiffBase, persist: boolean) => void
    }
}

export type CodeReviewPropsAndCallbacks = CodeReviewProps & CodeReviewCallbacks

interface CodeReviewState {
    showDiffModal: boolean
    baseChoice: {
        type: DiffBaseType
        submissionId: string|null
    }
}

export default class CodeReview extends
    React.Component<CodeReviewPropsAndCallbacks & CodeReviewPropsFromRouting, CodeReviewState> {

    constructor(props: CodeReviewPropsAndCallbacks & CodeReviewPropsFromRouting, context: any) {
        super(props, context);
        this.state = {
            showDiffModal: false,
            baseChoice: this.baseToFormState(props.diff.base)
        }
    }


    private baseToFormState(base: DiffBase) {
        return {
            type: base.type,
            submissionId: base.submissionId && base.submissionId.toString() || null
        }
    }

    makeOriginalLinkOrUndefined = (comment: BaseCommentToRead) => {
        if (this.props.comments.makeOriginalLink && comment.submissionId !== this.props.submissionId)
            return this.props.comments.makeOriginalLink(comment)
    };

    getDiffForEditor = () => {
        const fileDiff = this.props.diff.diff.get(this.props.editor.file);
        if (!fileDiff) {
            return []
        }

        return fileDiff.changes;
    }

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
                                       diff={this.getDiffForEditor()}
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
                    return <div className="no-file-chosen"><div>Please choose file</div></div>
        }
    };

    renderFileTreeVeil = () => {
        if (this.props.fileTree.loading)
            return <SpinnerWithVeil/>;
        else
            return null;
    };


    renderReview = () => {
        return <div className="code-review-app-rows">
            <div className="row code-review">
                <div className="col-xs-4 col-sm-3 col-md-3 col-lg-2 col-xl-2" id="code-review-left">
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
                            <Button bsStyle="warning" className="review-bottom-button" onClick={this.props.lostFound.onSelect}>
                                Lost + Found {" "}
                                <AggregatesLabel {...this.props.lostFound.aggregate}/>
                            </Button>
                        </div>
                        <div className="diff-mode-button-container">
                            <Button
                                bsStyle="primary"
                                className="review-bottom-button"
                                disabled={this.props.diff.loading}
                                onClick={() => this.setState({
                                    showDiffModal: true,
                                    baseChoice: this.baseToFormState(this.props.diff.base)
                                })}
                            >
                                Settings
                            </Button>
                        </div>
                    </div>
                </div>
                <div className="col-xs-8 col-sm-9 col-md-9 col-lg-10 col-xl-10" id="code-review-right">
                    {this.renderRightSide()}
                </div>
                {this.renderModal()}
            </div>
            {this.renderStatusBar()}
        </div>
    };

    shouldRenderReview = () => this.props.submission && this.props.submission.verificationData.status === "Processed";

    renderStatusBar = () =>
        <div className="code-review-status-bar">
            {this.renderDiffStatus()}
            <div className="clearfix"></div>
        </div>

    shrinkRev = (rev: string) => rev.substring(0, 10)

    renderSubLink = (id?: number) => {
        if (!id) {
            return undefined;
        }

        return <span>{" "}(<a href={`/submission/${id}/review/code/${this.props.editor.file}`}>Sub #{id}</a>)</span>
    }

    renderSubText = (id?: number) => {
        if (!id) {
            return undefined;
        }
        return ` (Sub #${id})`
    }

    renderDiffStatus = () => {
        if (!this.props.diff.from || !this.props.diff.to)
            return undefined;

        if (this.props.diff.from.revision === this.props.diff.to.revision)
            return undefined;

        return <div className="pull-right text-muted">
            {"Showing diff "}
            {this.shrinkRev(this.props.diff.from.revision)}
            {this.renderSubLink(this.props.diff.from.submissionId)}
            {" .. "}
            {this.shrinkRev(this.props.diff.to.revision)}
            {this.renderSubText(this.props.diff.to.submissionId)}
        </div>
    }

    renderModal = () =>
        <Modal show={this.state.showDiffModal} onHide={() => {
            this.setState({
                showDiffModal: false,
                baseChoice: this.baseToFormState(this.props.diff.base)
            })
        }}>
            <Modal.Header closeButton>
                <Modal.Title>Settings</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <FormGroup controlId="diff-base" >
                    <ControlLabel>Diff base</ControlLabel>
                    <Radio
                        name="diff-base"
                        value="COURSE_BASE"
                        checked={this.state.baseChoice.type === "COURSE_BASE"}
                        onChange={() => this.setState({
                            ...this.state,
                            baseChoice: {
                                type: "COURSE_BASE",
                                submissionId: null
                            }
                        })}>
                        Course base revision
                    </Radio>
                    {" "}
                    <Radio
                        name="diff-base"
                        value="PREVIOUS_CLOSED"
                        checked={this.state.baseChoice.type === "PREVIOUS_CLOSED"}
                        onChange={() => this.setState({
                            ...this.state,
                            baseChoice: {
                                type: "PREVIOUS_CLOSED",
                                submissionId: null
                            }
                        })}>
                        Latest closed submission
                    </Radio>
                    {this.props.capabilities.canViewTags &&<Radio
                        name="diff-base"
                        value="PREVIOUS_CHECKED"
                        checked={this.state.baseChoice.type === "PREVIOUS_CHECKED"}
                        onChange={() => this.setState({
                            ...this.state,
                            baseChoice: {
                                type: "PREVIOUS_CHECKED",
                                submissionId: null
                            }
                        })}>
                        Latest checked submission
                    </Radio>}
                    <Radio
                        name="diff-base"
                        value="SUBMISSION_ID"
                        checked={this.state.baseChoice.type === "SUBMISSION_ID"}
                        onChange={() => this.setState({
                            ...this.state,
                            baseChoice: {
                                type: "SUBMISSION_ID",
                                submissionId: this.state.baseChoice.submissionId
                            }
                        })}>
                        Specific submission

                    </Radio>
                </FormGroup>
                {this.state.baseChoice.type == "SUBMISSION_ID" && <FormGroup controlId="submission-id">
                    <ControlLabel >Submission Id</ControlLabel>
                    <FormControl
                        type="text"
                        value={this.state.baseChoice.submissionId || ""}
                        onChange={(e: ChangeEvent<any>) => {
                            this.setState({
                                ...this.state,
                                baseChoice: {
                                    type: "SUBMISSION_ID",
                                    submissionId: e.target.value as string || ""
                                }
                            })
                        }}
                    />
                    <FormControl.Feedback/>
                </FormGroup>}
            </Modal.Body>
            <Modal.Footer>
                <SplitButton
                    id="apply-diff-preference-dropdown"
                    title="Apply"
                    bsStyle="success"
                    disabled={this.state.baseChoice.type == "SUBMISSION_ID" &&
                        isNaN(parseInt(this.state.baseChoice.submissionId || ""))}
                    onClick={() => this.applyDiffPreference(false)}>
                    <MenuItem eventKey="apply-and-save"
                              disabled={this.state.baseChoice.type == "SUBMISSION_ID"}
                              onClick={() => this.applyDiffPreference(true)}>Apply and save</MenuItem>
                </SplitButton>
            </Modal.Footer>
        </Modal>

    applyDiffPreference = (persist: boolean) => {
        this.props.diff.onChangeDiffBase(
            this.props.submission!!.record.id, {
                type: this.state.baseChoice.type,
                submissionId: this.state.baseChoice.type == "SUBMISSION_ID" ?
                    parseInt(this.state.baseChoice.submissionId || "0") :
                    undefined
            }, persist)
        this.setState({
            showDiffModal: false
        })
    }

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
