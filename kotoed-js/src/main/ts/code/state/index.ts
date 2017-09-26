import {FileTreeState} from "./filetree";
import {EditorState} from "./editor";
import {CommentsState} from "./comments";
import {CapabilitiesState} from "./capabilities";
import {DbRecordWrapper} from "../../data/verification";
import {SubmissionToRead} from "../../data/submission";
import {SubmissionState} from "./submission";
import {ReviewForms} from "./forms";

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    commentsState: CommentsState
    capabilitiesState: CapabilitiesState
    submissionState: SubmissionState
    formState: ReviewForms
}

export interface ScrollTo {
    line?: number
    commentId?: number
}