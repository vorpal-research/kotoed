import {FileTreeState} from "./filetree";
import {EditorState} from "./editor";
import {CommentsState} from "./comments";
import {CapabilitiesState} from "./capabilities";
import {DbRecordWrapper} from "../../data/verification";
import {SubmissionToRead} from "../../data/submission";
import {SubmissionState} from "./submission";
import {ReviewForms} from "./forms";
import {CodeAnnotationsState} from "./annotations";
import {CommentTemplateState} from "./templates";
import {DiffBase, FileDiffResult} from "../remote/code";
import {Map} from "immutable"
import {DiffState} from "./diff";

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    codeAnnotationsState: CodeAnnotationsState
    commentTemplateState: CommentTemplateState
    commentsState: CommentsState
    capabilitiesState: CapabilitiesState
    submissionState: SubmissionState
    formState: ReviewForms
    diffState: DiffState
}

export interface ScrollTo {
    line?: number
    commentId?: number
}
