import {FileComments} from "./comments";
import {CmMode} from "../util/codemirror/index";

export interface EditorState {
    fileName: string
    value: string,
    displayedComments: FileComments
}
