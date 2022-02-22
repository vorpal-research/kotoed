import {FileDiffChange} from "../remote/code";

export interface EditorState {
    fileName: string
    value: string
    loading: boolean
    diff: Array<FileDiffChange>
}
