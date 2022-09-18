import {FileDiffChange} from "../remote/code";
import {FileComments} from "./comments";

export interface EditorState {
    fileName: string
    value: string
    displayedComments: FileComments,
    loading: boolean
    diff: Array<FileDiffChange>
}
