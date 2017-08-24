import * as React from "react";
import {Component} from "react";
import {Badge} from "react-bootstrap";
import {Tab, TabList, TabPanel, Tabs} from "react-tabs";

import {components} from "griddle-react";
import * as _ from "lodash";

import {
    ErrorDesc,
    GenericResponse,
    sendAsync,
    VerificationData,
    VerificationStatus
} from "./common";
import {ErrorTable} from "./errorTable";
import {ResultFilter, ResultHolder} from "./resultHolder";
import {ResultTable} from "./resultTable";

import {sleep} from "../../util/common";
import {Kotoed} from "../../util/kotoed-api";

export interface ResultListHolderProps<ResultT> {
    id: number
    resultHolders: ResultHolder<ResultT>[]
}

export type ResultListHolderState<ResultT> = {
    results: ResultT[]
    errors: ErrorDesc[]
    activeTabIndex: number
} & {
    [key: string]: boolean
}

export abstract class ResultListHolder<ResultT> extends Component<ResultListHolderProps<ResultT>, ResultListHolderState<ResultT>> {

    constructor(props: ResultListHolderProps<ResultT>, context: undefined) {
        super(props, context);

        let flatMappedHolderFilters = _.flatMap(
            this.props.resultHolders,
            rh => _.map(rh.props.filters, f => [`${rh.props.name}.${f.name}`, f.isOnByDefault])
        );

        this.state = _.extend(
            {
                results: [],
                errors: [],
                activeTabIndex: 0
            },
            _.reduce(
                flatMappedHolderFilters,
                (acc, [fname, fvalue]) => _.set(acc, fname, fvalue),
                {}
            ) as { [key: string]: boolean }
        );
    }

    componentWillMount() {
        this.loadResults()
            .then(this.processResults)
    }

    abstract loadResults: () => Promise<any>;

    protected processResults = (response: GenericResponse<ResultT>): Promise<any> => {
        let records = Promise.resolve(response.records);
        let errors = 0 < response.verificationData.errors.length
            ? sendAsync<VerificationData, ErrorDesc[]>
            (Kotoed.Address.Api.Submission.Error, response.verificationData)
            : Promise.resolve([] as ErrorDesc[]);

        return Promise.all([records, errors])
            .then(res => {
                let [r, e] = res;
                let activeTabIndex = e.length > 0
                    ? this.props.resultHolders.length
                    : this.state.activeTabIndex;
                this.setState({
                    results: r,
                    errors: e,
                    activeTabIndex: activeTabIndex
                });

                let shouldRetry =
                    VerificationStatus[VerificationStatus.NotReady] == response.verificationData.status
                    || VerificationStatus[VerificationStatus.Unknown] == response.verificationData.status
                    || 0 == r.length + e.length;

                if (shouldRetry) {
                    return sleep(15000)
                        .then(this.loadResults)
                        .then(this.processResults);
                } else {
                    return Promise.resolve();
                }
            });
    };

    isResultFilterActive = (holderName: string, filterName: string) => {
        let holderFilterName = `${holderName}.${filterName}`;
        return _.get(this.state, holderFilterName);
    };

    toggleResultFilter = (holderName: string, filterName: string) => {
        let holderFilterName = `${holderName}.${filterName}`;
        this.setState(_.set(this.state, holderFilterName, !_.get(this.state, holderFilterName)))
    };

    render() {

        type TabData = [ResultHolder<ResultT>, string, ResultT[], ResultFilter<ResultT>[], components.RowDefinition];

        let tabs: TabData[] = [];

        for (let resultHolder of this.props.resultHolders) {

            // Skipping data processing for non-active tabs
            // if (this.state.activeTabIndex != tabs.length) {
            //     tabs.push([
            //         resultHolder,
            //         resultHolder.props.name,
            //         [],
            //         [],
            //         resultHolder.props.rowDefinition
            //     ]);
            //     continue;
            // }

            let resultList = [];
            for (let result of this.state.results) {
                if (resultHolder.props.selector(result)) {
                    resultList.push(result);
                }
            }

            resultList = Array.of<ResultT>().concat(...resultList.map(resultHolder.props.transformer));

            for (let filter of resultHolder.props.filters) {
                if (this.isResultFilterActive(resultHolder.props.name, filter.name)) {
                    resultList = resultList.filter(_.negate(filter.predicate))
                }
            }

            tabs.push([
                resultHolder,
                resultHolder.props.name,
                resultList,
                resultHolder.props.filters,
                resultHolder.props.rowDefinition
            ]);
        }

        return <Tabs
            selectedIndex={this.state.activeTabIndex}
            onSelect={newTabIndex => this.setState({activeTabIndex: newTabIndex})}>

            <TabList>
                {tabs.map((data) => {
                    let [rh, name, list, filters, rowDefinition] = data;
                    return <Tab key={name}>
                        {name}
                        <Badge>{list.length}</Badge>
                    </Tab>;
                })}
                <Tab key="Errors">
                    Errors
                    <Badge>{this.state.errors.length}</Badge>
                </Tab>
            </TabList>

            {tabs.map((data) => {
                let [rh, name, list, filters, rowDefinition] = data;
                return <TabPanel key={name}>
                    <div className="btn-toolbar clearfix">
                        <div className="btn-group float-right">
                            {filters.map(f => {
                                let isActive = this.isResultFilterActive(name, f.name);
                                return <button key={f.name}
                                               className={isActive ? "btn btn-default active" : "btn btn-default"}
                                               type="button"
                                               onClick={(e) => this.toggleResultFilter(name, f.name)}>
                                    {f.name}
                                </button>;
                            })}
                        </div>
                    </div>
                    <ResultTable results={list} rowDefinition={rowDefinition}/>
                </TabPanel>;
            })}
            <TabPanel key="Errors">
                <ErrorTable errors={this.state.errors}/>
            </TabPanel>

        </Tabs>
    }
}
