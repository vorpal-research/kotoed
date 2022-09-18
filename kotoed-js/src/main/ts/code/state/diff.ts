import {DiffBase, FileDiffResult} from "../remote/code";
import {Map} from "immutable";

export interface DiffState {
    loading: boolean
    base: DiffBase
    diff: Map<string, FileDiffResult>
}
