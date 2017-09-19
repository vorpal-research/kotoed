import axios from "axios"
import {eventBus} from "../eventBus";
import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";
import {DbRecordWrapper} from "../data/verification";
import {BloatSubmission, SubmissionState, SubmissionToRead, TagRTI as Tag} from "../data/submission";
import {WithId} from "../data/common";
import {CommentAggregate} from "../code/remote/comments";
import {CourseToRead} from "../data/course";
import {sendAsync} from "../views/components/common";

export interface SubmissionPermissions {
    editOwnComments: boolean
    editAllComments: boolean,
    changeStateOwnComments: boolean,
    changeStateAllComments: boolean,
    postComment: boolean,
    changeState: boolean,
    resubmit: boolean,
    clean: boolean,
    tags: boolean
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
    return eventBus.send<WithId, DbRecordWrapper<BloatSubmission>>(Kotoed.Address.Api.Submission.Read, {
        id: submissionId
    })
}

export async function updateSubmission(submissionId: number,
                                       state: SubmissionState): Promise<DbRecordWrapper<BloatSubmission>> {
    return eventBus.send<SubmissionUpdateRequest, DbRecordWrapper<BloatSubmission>>(Kotoed.Address.Api.Submission.Update, {
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

interface TagRemote {
    id: number
    name: string
}

export async function fetchTagList(submissionId: number): Promise<Tag[]> {
    return eventBus.send<WithId, TagRemote[]>(Kotoed.Address.Api.Submission.Tags.Read, {
        id: submissionId
    }).then(tags => {
        return tags.map(tag => {
            return {id: tag.id, text: tag.name}
        })
    });
}

export async function fetchAvailableTags(): Promise<Tag[]> {
    return eventBus.send<{}, TagRemote[]>(Kotoed.Address.Api.Tag.List, {})
        .then(tags => {
            return tags.map(tag => {
                return {id: tag.id, text: tag.name}
            })
        });
}

export async function addSubmissionTag(tagId: number, submissionId: number): Promise<any> {
    return eventBus.send<SubmissionTagRequest, TagRemote[]>(Kotoed.Address.Api.Submission.Tags.Create, {
        tagId, submissionId
    });
}

export async function deleteSubmissionTag(tagId: number, submissionId: number): Promise<any> {
    return eventBus.send<SubmissionTagRequest, TagRemote[]>(Kotoed.Address.Api.Submission.Tags.Delete, {
        tagId, submissionId
    });
}

export async function cleanSubmission(submissionId: number): Promise<void> {
    return await sendAsync<WithId, void>(
        Kotoed.Address.Api.Submission.Verification.Clean,
        {
            id: submissionId
        }
    )
}