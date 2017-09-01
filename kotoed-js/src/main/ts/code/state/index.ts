import {FileTreeState} from "./filetree";
import {EditorState} from "./editor";
import {CommentsState} from "./comments";
import {CapabilitiesState} from "./capabilities";
import {DbRecordWrapper} from "../../data/verification";
import {SubmissionToRead} from "../../data/submission";
import {SubmissionState} from "./submission";

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    commentsState: CommentsState
    capabilitiesState: CapabilitiesState
    submissionState: SubmissionState
}

export interface ScrollTo {
    line?: number
    commentId?: number
}