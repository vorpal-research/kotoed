import {SubmissionDetailsProps} from "./components/SubmissionDetails";
import {Action} from "redux";
import {isType} from "typescript-fsa";
import {historyFetch, permissionsFetch, submissionFetch} from "./actions";

const initialState: SubmissionDetailsProps = {
    history: [],
    permissions: {
        resubmit: false,
        changeState: false
    },
    loading: true,
    submission: {
        record: {
            id: 0,
            state: "invalid",
            datetime: 0,
            projectId: 0,
            revision: ""
        },
        verificationData: {
            status: "Invalid"
        }
    },
    comments: {
        open: 0,
        closed: 0
    }
};

export function reducer(state: SubmissionDetailsProps = initialState, action: Action): SubmissionDetailsProps {
    if (isType(action, submissionFetch.done)) {
        return {...state, submission: action.payload.result, loading: false}
    } else if (isType(action, permissionsFetch.done)) {
        return {...state, permissions: action.payload.result}
    } else if (isType(action, historyFetch.done)) {
        let newHistory = [...state.history, ...action.payload.result];
        return {...state, history: newHistory}
    }
    return state;
}
