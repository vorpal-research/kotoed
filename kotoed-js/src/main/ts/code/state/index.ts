import {FileTreeState} from "./filetree";
import {EditorState} from "./editor";
import {CommentsState} from "./comments";
import {CapabilitiesState} from "./capabilities";

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
    commentsState: CommentsState
    capabilitiesState: CapabilitiesState
}

