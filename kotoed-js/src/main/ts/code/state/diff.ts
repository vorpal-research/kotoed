import {DiffBase, FileDiffResult, RevisionInfo} from "../remote/code";
import {Map} from "immutable";

export interface DiffState {
    loading: boolean
    base: DiffBase
    diff: Map<string, FileDiffResult>
    from?: RevisionInfo
    to?: RevisionInfo
}
