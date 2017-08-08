import {FileComments} from "./comments";

export interface EditorState {
    fileName: string
    value: string,
    displayedComments: FileComments
}
