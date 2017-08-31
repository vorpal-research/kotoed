import axios from "axios"
import {eventBus} from "../eventBus";
import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";
import {DbRecordWrapper, WithVerificationData} from "../data/verification";
import {SubmissionToRead} from "../data/submission";
import {WithId} from "../data/common";
import {CommentAggregate} from "../code/remote/comments";

export interface SubmissionPermissions {
    editOwnComments: boolean
    editAllComments: boolean,
    changeStateOwnComments: boolean,
    changeStateAllComments: boolean,
    postComment: boolean,
    changeState: boolean,
    resubmit: boolean
}


export async function fetchSubmission(submissionId: number): Promise<DbRecordWrapper<SubmissionToRead>> {
    return eventBus.send<WithId, DbRecordWrapper<SubmissionToRead>>(Kotoed.Address.Api.Submission.Read, {
        id: submissionId
    })
}

export async function fetchPermissions(submissionId: number): Promise<SubmissionPermissions> {
    let permissionsResp = await axios.get(
        Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.AuthHelpers.SubmissionPerms, {
            id: submissionId
        }));

    return keysToCamelCase(permissionsResp.data)
}

interface HistoryRequest {
    submissionId: number,
    limit: number
}

export async function fetchHistory(start: number, limit: number) {
    return eventBus.send<HistoryRequest, Array<SubmissionToRead>>(Kotoed.Address.Api.Submission.History, {
        submissionId: start,
        limit
    })
}

export async function fetchCommentsTotal(submissionId: number): Promise<CommentAggregate> {
    return eventBus.send<WithId, CommentAggregate>(Kotoed.Address.Api.Submission.CommentsTotal, {
        id: submissionId
    })
}