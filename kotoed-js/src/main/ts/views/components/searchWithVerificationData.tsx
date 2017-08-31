import * as _ from "lodash";

import {MakeBaseQuery, SearchTable, SearchTableProps, SearchTableState} from "./search";
import {sleep} from "../../util/common";
import {VerificationStatus, WithVerificationData} from "../../data/verification";
import {pollDespairing} from "../../util/poll";

interface WithVerificationDataReq {
    withVerificationData: boolean
}

const finalStatuses: Array<VerificationStatus> = ["Invalid", "Processed"];

export function isStatusFinal(status: VerificationStatus) {
    return finalStatuses.includes(status);
}

// TODO maybe add optional polling right into SearchTable
// TODO because extening react components makes mess
export class SearchTableWithVerificationData<DataType, QueryType = {}> extends
        SearchTable<DataType & WithVerificationData,
            QueryType | WithVerificationDataReq> {

    constructor(props: SearchTableProps<DataType & WithVerificationData, QueryType | WithVerificationDataReq>) {
        super(props);
        this.makeBaseQuery = () => {
            const orig = props.makeBaseQuery || (() => {return {}});
            return Object.assign({}, orig(), {
                withVerificationData: true
            })
        };


    }

    componentWillReceiveProps(props: SearchTableProps<DataType, QueryType | WithVerificationDataReq>) {
        super.componentWillReceiveProps(props);
        this.makeBaseQuery = () => {
            const orig = props.makeBaseQuery || (() => {return {}});
            return Object.assign({}, orig(), {
                withVerificationData: true
            })
        };
    }

    private isGoodEnough = (data: Array<DataType & WithVerificationData>) => {
        return data.every((value: DataType & WithVerificationData) => isStatusFinal(value.verificationData.status))
    };

    private isQueryChanged = (oldState: SearchTableState<DataType & WithVerificationData>) => {
        return oldState.text !== this.state.text || oldState.currentPage !== this.state.currentPage
    };

    protected queryData = async () => {
        let oldState = this.state;

        const handleResult = (results: Array<DataType & WithVerificationData>) => {
            this.setState({currentResults: results, touched: true}, () => oldState = this.state);
        };

        await pollDespairing({
            action: this.doQueryData,
            isGoodEnough: this.isGoodEnough,
            onFinal: handleResult,
            onIntermediate: handleResult,
            strategyParams: {shouldGiveUp: () => this.isQueryChanged(oldState)}
        });
    }
}