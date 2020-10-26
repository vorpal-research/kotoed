import axios from "axios"
import * as _ from "lodash"
import {eventBus} from "../eventBus";
import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";
import {DbRecordWrapper} from "../data/verification";
import {BloatSubmission, SubmissionState, SubmissionToRead, Tag} from "../data/submission";
import {WithId} from "../data/common";
import {CommentAggregate} from "../code/remote/comments";
import {CourseToRead} from "../data/course";
import {sendAsync} from "../views/components/common";
import natsort from "natsort";

export interface SubmissionPermissions {
    editOwnComments: boolean
    editAllComments: boolean,
    changeStateOwnComments: boolean,
    changeStateAllComments: boolean,
    postComment: boolean,
    changeState: boolean,
    resubmit: boolean,
    clean: boolean,
    tags: boolean,
    klones: boolean
}

export interface SubmissionUpdateRequest {
    id: number,
    state: SubmissionState
}

export interface SubmissionTagRequest {
    tagId: number,
    submissionId: number
}

export async function fetchSubmission(submissionId: number): Promise<DbRecordWrapper<BloatSubmission>> {
    return sendAsync(Kotoed.Address.Api.Submission.Read, {
        id: submissionId
    })
}

export async function updateSubmission(submissionId: number,
                                       state: SubmissionState): Promise<DbRecordWrapper<BloatSubmission>> {
    return sendAsync(Kotoed.Address.Api.Submission.Update, {
        id: submissionId,
        state: state
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
    return sendAsync(Kotoed.Address.Api.Submission.History, {
        submissionId: start,
        limit
    })
}

export async function fetchCommentsTotal(submissionId: number): Promise<CommentAggregate> {
    return sendAsync(Kotoed.Address.Api.Submission.CommentsTotal, {
        id: submissionId
    })
}


export async function fetchTagList(submissionId: number): Promise<Tag[]> {
    return sendAsync(Kotoed.Address.Api.Submission.Tags.Read, {
        id: submissionId
    }).then(tags => {
        return tags
    });
}

export async function fetchAvailableTags(): Promise<Tag[]> {
    const sorter = natsort();

    return sendAsync(Kotoed.Address.Api.Tag.List, undefined)
        .then(tags => {
            return tags.sort((a: Tag, b: Tag) =>
                // Sort negative numbers as if they were positive
                sorter(_.trimStart(a.name, '-'), _.trimStart(b.name, '-')));
        });
}

export async function addSubmissionTag(tagId: number, submissionId: number): Promise<any> {
    return sendAsync(Kotoed.Address.Api.Submission.Tags.Create, {
        tagId, submissionId
    });
}

export async function deleteSubmissionTag(tagId: number, submissionId: number): Promise<any> {
    return sendAsync(Kotoed.Address.Api.Submission.Tags.Delete, {
        tagId, submissionId
    });
}

export async function cleanSubmission(submissionId: number): Promise<void> {
    await sendAsync(
        Kotoed.Address.Api.Submission.Verification.Clean,
        {
            id: submissionId
        }
    )
}