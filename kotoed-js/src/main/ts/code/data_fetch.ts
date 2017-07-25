/**
 * Created by gagarski on 7/20/17.
 */

import  axios , {AxiosResponse} from "axios"
import {sleep} from "../util";
import {File} from "./model";

// TODO switch from debug API
const READY_URL = "/debug/eventbus/kotoed.api.submission.code.download";
const LIST_URL = "/debug/eventbus/kotoed.api.submission.code.list";
const FILE_URL = "/debug/eventbus/kotoed.api.submission.code.read";
const COMMENTS_URL = "/debug/eventbus/kotoed.api.submission.comments";


interface ResponseWithStatus {
    status: "done" | "pending" | "failed"
}

interface RootDirResponse extends ResponseWithStatus {
    root: File
}

interface FileResponse extends ResponseWithStatus {
    contents: string
}

const AWAIT_READY_DELAY = 500;

async function repeatTillReady<T extends ResponseWithStatus>(doRequest: () => Promise<AxiosResponse>): Promise<T> {
    while(true) {
        let response = await doRequest();
        let data = response.data as T;
        if (data.status === "failed")
            throw "Fetch failed =("; // TODO replace with proper handling
        if (data.status === "done")
            return data;
        await sleep(AWAIT_READY_DELAY);
    }
}

export async function fetchRootDir(submissionId: number): Promise<Array<File>> {
    let res = await repeatTillReady<RootDirResponse>(() => {
        return axios.post(LIST_URL, {
            submission_id: submissionId
        })
    });
    return res.root.children || [];
}

export async function fetchFile(submissionId: number, path: string): Promise<string> {
    let res = await repeatTillReady<FileResponse>(() => {
        return axios.post(FILE_URL, {
            submission_id: submissionId,
            path
        })
    });
    return res.contents;
}

export async function waitTillReady(submissionId: number): Promise<void> {
    await repeatTillReady<ResponseWithStatus>(() => {
        return axios.post(READY_URL, {
            submission_id: submissionId
        })
    });
}
