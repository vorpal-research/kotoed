
import {DbRecordWrapper} from "../../data/verification";
import {SubmissionToRead} from "../../data/submission";

export interface SubmissionState {
    submission: DbRecordWrapper<SubmissionToRead> | null
}