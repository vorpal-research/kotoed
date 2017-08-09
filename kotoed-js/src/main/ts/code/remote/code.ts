import {sleep} from "../../util";
import {EventBusError} from "../../util/vertx";
import {eventBus} from "../../eventBus";
import {ResponseWithStatus, SubmissionIdRequest} from "./common";

const READY_ADDRESS = "kotoed.api.submission.code.download";
const LIST_ADDRESS = "kotoed.api.submission.code.list";
const FILE_ADDRESS = "kotoed.api.submission.code.read";


export type FileType = "file" | "directory"

export interface File {
    type: FileType;
    name: string,
    children: Array<File> | null
}


type RootDirRequest = SubmissionIdRequest

interface RootDirResponse extends ResponseWithStatus {
    root: File
}

interface FileRequest extends SubmissionIdRequest {
    path: string
}

interface FileResponse extends ResponseWithStatus {
    contents: string
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
        return eventBus.send(LIST_ADDRESS, {
            submissionId: submissionId
        })
    });
    return res.root;
}

export async function fetchFile(submissionId: number, path: string): Promise<string> {
    let res = await repeatTillReady<FileResponse>(() => {
        return eventBus.send(FILE_ADDRESS, {
            submissionId: submissionId,
            path
        })

    });
    return res.contents;
}

export async function waitTillReady(submissionId: number): Promise<void> {
    await repeatTillReady<IsReadyResponse>(() => {
        return eventBus.send(READY_ADDRESS, {
            submissionId: submissionId,
        })
    });
}
