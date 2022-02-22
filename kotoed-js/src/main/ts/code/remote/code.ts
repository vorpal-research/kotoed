import {sleep} from "../../util/common";
import {EventBusError} from "../../util/vertx";
import {eventBus} from "../../eventBus";
import {ResponseWithStatus, SubmissionIdRequest} from "./common";
import {Kotoed} from "../../util/kotoed-api";
import {sendAsync} from "../../views/components/common";

export type FileType = "file" | "directory"

export interface File {
    type: FileType;
    name: string,
    changed: boolean,
    children?: Array<File>
}

export type DiffLineChangeType = 'NEUTRAL' | 'FROM' | 'TO'

export interface FileDiffChangeRange {
    start: number
    count: number
}

export interface FileDiffChangeLine {
    contents: string
    type: DiffLineChangeType
}

export interface FileDiffChange {
    from: FileDiffChangeRange
    to: FileDiffChangeRange
    lines: Array<FileDiffChangeLine>
}

export interface FileDiffResult {
    fromFile: string
    toFile: string
    changes: Array<FileDiffChange>
}

type RootDirRequest = SubmissionIdRequest

interface RootDirResponse extends ResponseWithStatus {
    root?: File
}

interface FileRequest extends SubmissionIdRequest {
    path: string
}

interface FileResponse extends ResponseWithStatus {
    contents: string
}

interface FileDiffResponse extends ResponseWithStatus {
    diff: Array<FileDiffResult>
}

type IsReadyRequest = SubmissionIdRequest

type IsReadyResponse = ResponseWithStatus

const AWAIT_READY_DELAY = 500;

async function repeatTillReady<T extends ResponseWithStatus>(doRequest: () => Promise<T>): Promise<T> {
    while(true) {
        let response = await doRequest();
        if (response.status === "failed")
            throw new EventBusError("Fetch failed"); // TODO replace with proper handling
        if (response.status === "done")
            return response;
        await sleep(AWAIT_READY_DELAY);
    }
}

export async function fetchRootDir(submissionId: number): Promise<File> {
    let res = await repeatTillReady<RootDirResponse>(() => {
        return sendAsync(Kotoed.Address.Api.Submission.Code.List, {
            submissionId: submissionId
        })
    });
    return res.root!;
}

export async function fetchFile(submissionId: number,
                                path: string,
                                fromLine: number | undefined = undefined,
                                toLine: number | undefined = undefined): Promise<string> {

    let res = await repeatTillReady<FileResponse>(() => {
        return sendAsync(Kotoed.Address.Api.Submission.Code.Read, {
            submissionId: submissionId,
            path: path,
            fromLine: fromLine,
            toLine: toLine
        });
    });
    return res.contents;
}

export async function fetchDiff(submissionId: number): Promise<Array<FileDiffResult>> {

    let res = await repeatTillReady<FileDiffResponse>(() => {
        return sendAsync(Kotoed.Address.Api.Submission.Code.Diff, {
            submissionId: submissionId
        });
    });
    return res.diff;
}

export async function waitTillReady(submissionId: number): Promise<void> {
    await repeatTillReady<IsReadyResponse>(() => {
        return sendAsync(Kotoed.Address.Api.Submission.Code.Download, {
            submissionId: submissionId,
        })
    });
}
