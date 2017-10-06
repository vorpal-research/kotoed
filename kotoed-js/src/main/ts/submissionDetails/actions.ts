import actionCreatorFactory from 'typescript-fsa';
import {Dispatch} from "react-redux";
import {DbRecordWrapper, isStatusFinal} from "../data/verification";
import {BloatSubmission, Submission, SubmissionToRead, Tag} from "../data/submission";
import {
    addSubmissionTag as addSubmissionTagRemote,
    deleteSubmissionTag as deleteSubmissionTagRemote,
    fetchAvailableTags as fetchAvailableTagsRemote,
    fetchCommentsTotal as fetchCommentsTotalRemote,
    fetchHistory as fetchHistoryRemote,
    fetchPermissions as fetchPermissionsRemote,
    fetchSubmission as fetchSubmissionRemote,
    fetchTagList as fetchTagListRemote,
    SubmissionPermissions,
    SubmissionUpdateRequest,
    updateSubmission as updateSubmissionRemote,
    cleanSubmission as cleanSubmissionRemote
} from "./remote";
import {Kotoed} from "../util/kotoed-api";
import {SubmissionDetailsProps} from "./components/SubmissionDetails";
import {isSubmissionAvalable} from "../submissions/util";
import {CommentAggregate} from "../code/remote/comments";
import {pollDespairing} from "../util/poll";
import {WithId} from "../data/common";
import {RequestWithId} from "../code/remote/common";

const actionCreator = actionCreatorFactory();


export const submissionFetch = actionCreator.async<number, DbRecordWrapper<BloatSubmission>, {}>('SUB_FETCH');
export const permissionsFetch = actionCreator.async<number, SubmissionPermissions, {}>('PERM_FETCH');
export const historyFetch = actionCreator.async<{ start: number, limit: number }, Array<SubmissionToRead>, {}>('HIST_FETCH');
export const commentsTotalFetch = actionCreator.async<number, CommentAggregate, {}>('COMMENTS_TOTAL_FETCH');
export const tagListFetch = actionCreator.async<number, Tag[], {}>('TAG_LIST_FETCH');
export const availableTagsFetch = actionCreator.async<null, Tag[], {}>('AVAILABLE_TAGS');
export const submissionTagAdd = actionCreator.async<{ tagId: number, submissionId: number }, number, {}>('TAG_ADD');
export const submissionTagDelete = actionCreator.async<{ tagId: number, submissionId: number }, number, {}>('TAG_DELETE');

export function fetchSubmission(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(submissionFetch.started(id));
        let sub = await fetchSubmissionRemote(id);
        dispatch(submissionFetch.done({
            params: id,
            result: sub
        }));
    }
}

export function fetchPermissions(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(permissionsFetch.started(id));
        let perms = await fetchPermissionsRemote(id);
        dispatch(permissionsFetch.done({
            params: id,
            result: perms
        }));
    }
}

export function fetchHistory(start: number, limit: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(historyFetch.started({start, limit}));
        let hist = await fetchHistoryRemote(start, limit);
        dispatch(historyFetch.done({
            params: {start, limit},
            result: hist
        }));
    }
}

export function navigateToNew(submissionId: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        window.location.href = Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Submission.Index, {id: submissionId})
    };
}

function fetchCommentsTotal(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {

        dispatch(commentsTotalFetch.started(id));
        let comments = await fetchCommentsTotalRemote(id);
        dispatch(commentsTotalFetch.done({
            params: id,
            result: comments
        }));
    }
}

function pollSubmissionIfNeeded(id: number, initial: DbRecordWrapper<SubmissionToRead>) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        let sub = initial;

        if (!isStatusFinal(sub.verificationData.status)) {
            dispatch(commentsTotalFetch.done({
                params: id,
                result: {
                    open: 0,
                    closed: 0
                }
            }));


            function dispatchDone(res: DbRecordWrapper<BloatSubmission>) {
                dispatch(submissionFetch.done({
                    params: id,
                    result: res
                }));
                sub = res;
            }

            await pollDespairing({
                action: () => fetchSubmissionRemote(id),
                isGoodEnough: (sub) => isStatusFinal(sub.verificationData.status),
                onIntermediate: dispatchDone,
                onFinal: dispatchDone,
                onGiveUp: dispatchDone
            });
        }
        await fetchPermissions(id)(dispatch);

    }
}

export function initialize(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>, getState: () => SubmissionDetailsProps) => {
        dispatch(submissionFetch.started(id));
        let sub = await fetchSubmissionRemote(id);
        dispatch(submissionFetch.done({
            params: id,
            result: sub
        }));

        await fetchPermissions(id)(dispatch);

        if (getState().permissions.tags) {
            await fetchTagList(id)(dispatch);
            await fetchAvailableTags()(dispatch);
        }

        if (sub.record.parentSubmissionId)
            await fetchHistory(sub.record.parentSubmissionId, 5)(dispatch);

        await pollSubmissionIfNeeded(id, sub)(dispatch);

        if (isSubmissionAvalable({...getState().submission.record as SubmissionToRead,
                verificationData: getState().submission.verificationData})) {
            await fetchCommentsTotal(id)(dispatch);
        }

    }
}

export function updateSubmission(payload: SubmissionUpdateRequest) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(submissionFetch.started(payload.id));
        let sub = await updateSubmissionRemote(payload.id, payload.state);
        dispatch(submissionFetch.done({
            params: payload.id,
            result: sub
        }));

        await fetchPermissions(payload.id)(dispatch);

        await pollSubmissionIfNeeded(payload.id, sub)(dispatch);

    }
}

export function deleteSubmission() {
    return async (dispatch: Dispatch<SubmissionDetailsProps>, getState: () => SubmissionDetailsProps) => {
        let sub = getState().submission.record;
        await updateSubmissionRemote(sub.id, "deleted");
        window.location.href = Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Project.Index, {
            id: sub.projectId
        });
    }
}

export function fetchTagList(submissionId: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(tagListFetch.started(submissionId));
        let tagList = await fetchTagListRemote(submissionId);
        dispatch(tagListFetch.done({
            params: submissionId,
            result: tagList
        }));
    }
}

export function fetchAvailableTags() {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(availableTagsFetch.started(null));
        let availableTags = await fetchAvailableTagsRemote();
        dispatch(availableTagsFetch.done({
            params: null,
            result: availableTags
        }));
    }
}

export function addSubmissionTag(tagId: number, submissionId: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(submissionTagAdd.started({tagId, submissionId}));
        let res = await addSubmissionTagRemote(tagId, submissionId);
        dispatch(submissionTagAdd.done({
            params: {tagId, submissionId},
            result: tagId
        }));
    }
}

export function deleteSubmissionTag(tagId: number, submissionId: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(submissionTagDelete.started({tagId, submissionId}));
        let res = await deleteSubmissionTagRemote(tagId, submissionId);
        dispatch(submissionTagDelete.done({
            params: {tagId, submissionId},
            result: tagId
        }));
    }
}

export function cleanSubmission(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        await cleanSubmissionRemote(id);
        dispatch(submissionFetch.started(id));

        let sub = await fetchSubmissionRemote(id);
        dispatch(submissionFetch.done({
            params: id,
            result: sub
        }));

        await fetchPermissions(id)(dispatch);

        await pollSubmissionIfNeeded(id, sub)(dispatch);
    }
}
