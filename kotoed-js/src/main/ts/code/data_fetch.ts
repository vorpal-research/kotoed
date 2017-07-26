/**
 * Created by gagarski on 7/20/17.
 */

import  axios , {AxiosResponse} from "axios"
import {sleep} from "../util";
import {File} from "./model";
import {AsyncEventBus, EventBusError} from "../util/vertx";
import {fromLocationHost} from "../util/url";

const READY_ADDRESS = "kotoed.api.submission.code.download";
const LIST_ADDRESS = "kotoed.api.submission.code.list";
const FILE_ADDRESS = "kotoed.api.submission.code.read";
const COMMENTS_ADDRESS = "kotoed.api.submission.comments";

type ResponseStatus = "done" | "pending" | "failed"

interface ResponseWithStatus {
    status: ResponseStatus
}

interface SubmissionIdRequest {
    submission_id: number
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

const eventBus = new AsyncEventBus(fromLocationHost("eventbus"));

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

export async function fetchRootDir(submissionId: number): Promise<Array<File>> {
    let res = await repeatTillReady<RootDirResponse>(() => {
        return eventBus.send<RootDirRequest, RootDirResponse>(LIST_ADDRESS, {
            submission_id: submissionId
        })
    });
    return res.root.children || [];
}

export async function fetchFile(submissionId: number, path: string): Promise<string> {
    let res = await repeatTillReady<FileResponse>(() => {
        return eventBus.send<FileRequest, FileResponse>(FILE_ADDRESS, {
            submission_id: submissionId,
            path
        })

    });
    return res.contents;
}

export async function waitTillReady(submissionId: number): Promise<void> {
    await repeatTillReady<ResponseWithStatus>(() => {
        return eventBus.send<IsReadyRequest, IsReadyResponse>(READY_ADDRESS, {
            submission_id: submissionId,
        })
    });
}
