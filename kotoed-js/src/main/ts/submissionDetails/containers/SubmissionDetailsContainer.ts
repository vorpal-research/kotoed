import * as _ from "lodash";
import {connect} from "react-redux";
import SubmissionDetails, {
    SubmissionDetailsCallbacks,
    SubmissionDetailsProps
} from "../components/SubmissionDetails";
import {Dispatch} from "redux";
import {WithId} from "../../data/common";
import {
    addSubmissionTag,
    deleteSubmissionTag,
    fetchHistory,
    initialize,
    navigateToNew,
    updateSubmission,
} from "../actions";

type ContainerProps = WithId

const mapStateToProps = function (store: SubmissionDetailsProps,
                                  ownProps: ContainerProps): SubmissionDetailsProps {
    return store
};

const mapDispatchToProps = function (dispatch: Dispatch<SubmissionDetailsProps>,
                                     ownProps: ContainerProps): SubmissionDetailsCallbacks {
    return {
        history: {
            onMore: (latest: number) => {
                dispatch(fetchHistory(latest, 5))
            }
        },
        onResubmit: (newId: number) => {
            dispatch(navigateToNew(newId))
        },
        onClose: () => {
            dispatch(updateSubmission({id: ownProps.id, state: "closed"}))
        },
        onReopen: () => {
            dispatch(updateSubmission({id: ownProps.id, state: "open"}))
        },
        onMount: () => {
            dispatch(initialize(ownProps.id))
        },
        onTagAdd: (tagId: number) => {
            dispatch(addSubmissionTag(tagId, ownProps.id))
        },
        onTagDelete: (tagId: number) => {
            dispatch(deleteSubmissionTag(tagId, ownProps.id))
        }
    }
};

export default connect(
    mapStateToProps,
    mapDispatchToProps,
    (state, dispatch, own) => _.merge({}, state, dispatch, own)
)(SubmissionDetails);
