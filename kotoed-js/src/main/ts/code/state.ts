import {List, Map} from "immutable";
import {FileTreeProps} from "./components/FileTree";
/**
 * Created by gagarski on 7/18/17.
 */

export interface EditorState {
    fileName: string
    value: string
}

export interface FileTreeState extends FileTreeProps {
    selectedPath: Array<number>
}

export interface CodeReviewState {
    fileTreeState: FileTreeState
    editorState: EditorState
}
