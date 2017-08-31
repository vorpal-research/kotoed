import actionCreatorFactory from 'typescript-fsa';
import {Dispatch} from "react-redux";
import {DbRecordWrapper} from "../data/verification";
import {SubmissionState, SubmissionToRead} from "../data/submission";
import {SubmissionPermissions, SubmissionUpdateRequest} from "./remote";
import {SubmissionDetailsProps} from "./components/SubmissionDetails";
import {
    fetchSubmission as fetchSubmissionRemote,
    fetchPermissions as fetchPermissionsRemote,
    fetchHistory as fetchHistoryRemote,
    fetchCommentsTotal as fetchCommentsTotalRemote,
    updateSubmission as updateSubmissionRemote,
} from "./remote";
import {Kotoed} from "../util/kotoed-api";
import {isStatusFinal} from "../views/components/searchWithVerificationData";
import {sleep} from "../util/common";
import {isSubmissionAvalable} from "../submissions/util";
import {CommentAggregate} from "../code/remote/comments";
import {pollDespairing} from "../util/poll";

const actionCreator = actionCreatorFactory();


export const submissionFetch = actionCreator.async<number, DbRecordWrapper<SubmissionToRead>, {}>('SUB_FETCH');
export const permissionsFetch = actionCreator.async<number, SubmissionPermissions, {}>('PERM_FETCH');
export const historyFetch = actionCreator.async<{start: number, limit: number}, Array<SubmissionToRead>, {}>('HIST_FETCH');
export const commentsTotalFetch = actionCreator.async<number, CommentAggregate, {}>('COMMENTS_TOTAL_FETCH');


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

function pollSubmissionIfNeeded(id: number, initial: DbRecordWrapper<SubmissionToRead>) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        let sub = initial;

        if (isStatusFinal(sub.verificationData.status))
            return;

        dispatch(permissionsFetch.done({
            params: id,
            result: {
                resubmit: false,
                changeState: false,
                editAllComments: false,
                changeStateOwnComments: false,
                postComment: false,
                changeStateAllComments: false,
                editOwnComments: false
            }
        }));
        dispatch(commentsTotalFetch.done({
            params: id,
            result: {
                open: 0,
                closed: 0
            }
        }));


        function dispatchDone(res: DbRecordWrapper<SubmissionToRead>) {
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

        // We wont fetch anything for invalid sub
        if (isSubmissionAvalable({...sub.record, verificationData: sub.verificationData})) {
            dispatch(commentsTotalFetch.started(id));
            let comments = await fetchCommentsTotalRemote(id);
            dispatch(commentsTotalFetch.done({
                params: id,
                result: comments
            }));
        }

        await fetchPermissions(id)(dispatch); // Permissions can be changed after status has changed
    }
}

export function initialize(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(submissionFetch.started(id));
        let sub = await fetchSubmissionRemote(id);
        dispatch(submissionFetch.done({
            params: id,
            result: sub
        }));

        await fetchPermissions(id)(dispatch);

        if (sub.record.parentSubmissionId)
            await fetchHistory(sub.record.parentSubmissionId, 5)(dispatch);

        pollSubmissionIfNeeded(id, sub)(dispatch)

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

        pollSubmissionIfNeeded(payload.id, sub)(dispatch)

    }
}