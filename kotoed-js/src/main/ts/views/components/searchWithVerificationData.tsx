import * as _ from "lodash";

import {MakeBaseQuery, SearchTable, SearchTableProps, SearchTableState} from "./search";
import {sleep} from "../../util/common";

interface WithVerificationDataReq {
    withVerificationData: boolean
}

type VerificationStatus = "NotReady" | "Invalid" | "Unknown" | "Processed"

export interface VerificationData {
    status: VerificationStatus
}

export interface WithVerificationDataResp {
    verificationData: VerificationData
}


const finalStatuses: Array<VerificationStatus> = ["Invalid", "Processed"];

export function isStatusFinal(status: VerificationStatus) {
    return finalStatuses.includes(status);
}

const RETRIES = 10;
const SLEEP = 1000;

export class SearchTableWithVerificationData<DataType, QueryType = {}> extends
        SearchTable<DataType & WithVerificationDataResp,
            QueryType | WithVerificationDataReq> {


    constructor(props: SearchTableProps<DataType & WithVerificationDataResp, QueryType | WithVerificationDataReq>) {
        super(props);
        this.makeBaseQuery = props.makeBaseQuery || (() => {
            return {
                withVerificationData: true
            }
        });

    }

    componentWillReceiveProps(props: SearchTableProps<DataType, QueryType>) {
        this.makeBaseQuery = props.makeBaseQuery || (() => {
            return {
                withVerificationData: true
            }
        });
    }

    private isGoodEnough(data: Array<DataType & WithVerificationDataResp>) {
        return data.every((value: DataType & WithVerificationDataResp) => isStatusFinal(value.verificationData.status))
    }

    private isQueryChanged(oldState: SearchTableState<DataType & WithVerificationDataResp>) {
        return oldState.text !== this.state.text || oldState.currentPage !== this.state.currentPage
    }

    protected queryData = async () => {
        console.log("QUERYING");
        let results;
        let i = 0;
        let oldState = this.state;
        while (true) {
            if (this.isQueryChanged(oldState))
                return;
            results = await this.doQueryData();
            this.setState({currentResults: results, touched: true}, () => oldState = this.state);
            if (i >= RETRIES || this.isGoodEnough(results))
                return;
            i++;
            await sleep(SLEEP);
        }
    }
}