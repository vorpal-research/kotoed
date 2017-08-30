import * as _ from "lodash";
import {connect, MapStateToPropsParam} from "react-redux";
import SubmissionDetails, {SubmissionDetailsCallbacks, SubmissionDetailsProps} from "../components/SubmissionDetails";
import {Dispatch} from "redux";
import {WithId} from "../../data/common";
import {fetchHistory, initialize, navigateToNew} from "../actions";

type ContainerProps = WithId

const mapStateToProps = function(store: SubmissionDetailsProps,
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
            // TODO
        },
        onReopen: () => {
            // TODO
        },
        onMount: () => {
            dispatch(initialize(ownProps.id))
        }
    }
};

export default connect(
    mapStateToProps,
    mapDispatchToProps,
    (state, dispatch, own) => _.merge({}, state, dispatch, own)
)(SubmissionDetails);
