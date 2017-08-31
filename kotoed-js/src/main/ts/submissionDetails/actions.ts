import actionCreatorFactory from 'typescript-fsa';
import {Dispatch} from "react-redux";
import {DbRecordWrapper} from "../data/verification";
import {SubmissionToRead} from "../data/submission";
import {SubmissionPermissions} from "./remote";
import {SubmissionDetailsProps} from "./components/SubmissionDetails";
import {
    fetchSubmission as fetchSubmissionRemote,
    fetchPermissions as fetchPermissionsRemote,
    fetchHistory as fetchHistoryRemote,
    fetchCommentsTotal as fetchCommentsTotalRemote,

} from "./remote";
import {Kotoed} from "../util/kotoed-api";
import {isStatusFinal} from "../views/components/searchWithVerificationData";
import {sleep} from "../util/common";
import {isSubmissionAvalable} from "../submissions/util";
import {CommentAggregate} from "../code/remote/comments";

const actionCreator = actionCreatorFactory();

interface SignInPayload {
    username: string
    password: string
    oAuthProvider?: string
}

interface SignUpPayload {
    username: string
    password: string
    email: string|null
    oAuthProvider?: string
}


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

export function initialize(id: number) {
    return async (dispatch: Dispatch<SubmissionDetailsProps>) => {
        dispatch(submissionFetch.started(id));
        let sub = await fetchSubmissionRemote(id);
        dispatch(submissionFetch.done({
            params: id,
            result: sub
        }));

        await fetchPermissions(id)(dispatch); // TODO do we need it here?

        if (sub.record.parentSubmissionId)
            await fetchHistory(sub.record.parentSubmissionId, 5)(dispatch);

        while (!isStatusFinal(sub.verificationData.status)) {
            await sleep(15000);
            dispatch(submissionFetch.started(id));
            sub = await fetchSubmissionRemote(id);
            dispatch(submissionFetch.done({
                params: id,
                result: sub
            }));
        }

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