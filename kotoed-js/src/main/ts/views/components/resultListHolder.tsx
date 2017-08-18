import * as React from "react";
import {Component} from "react";
import {Tab, TabList, TabPanel, Tabs} from "react-tabs";
import {isArray} from "util";

import {components} from "griddle-react";

import {
    ErrorDesc,
    GenericResponse,
    sendAsync,
    VerificationData,
    VerificationStatus
} from "./common";
import {ErrorTable} from "./errorTable";
import {ResultHolder} from "./resultHolder";
import {ResultTable} from "./resultTable";

import {sleep} from "../../util/common";
import {Kotoed} from "../../util/kotoed-api";

export interface ResultListHolderProps {
    id: number
}

export interface ResultListHolderState<ResultT> {
    results: ResultT[]
    errors: ErrorDesc[]
    resultHolders: ResultHolder<ResultT>[],
    activeTabIndex: number
}

export abstract class ResultListHolder<ResultT> extends Component<ResultListHolderProps, ResultListHolderState<ResultT>> {

    constructor(props: ResultListHolderProps, context: undefined) {
        super(props, context);

        let resultHolders: any[] =
            isArray(this.props.children)
                ? this.props.children
                : [this.props.children];

        this.state = {
            results: [],
            errors: [],
            resultHolders: resultHolders.map(rh => new ResultHolder(rh.props, undefined)),
            activeTabIndex: 0
        };
    }

    componentDidMount() {
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
                    ? this.state.resultHolders.length
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

    render() {

        type TabData = [string, ResultT[], components.RowDefinition];

        let tabs: TabData[] = [];

        for (let resultHolder of this.state.resultHolders) {

            // Skipping data processing for non-active tabs
            if (this.state.activeTabIndex != tabs.length) {
                tabs.push([
                    resultHolder.props.name,
                    [],
                    resultHolder.state.rowDefinition
                ]);
                continue;
            }

            let resultList = [];
            for (let result of this.state.results) {
                if (resultHolder.props.selector(result)) {
                    resultList.push(result);
                }
            }
            tabs.push([
                resultHolder.props.name,
                Array.of<ResultT>().concat(...resultList.map(resultHolder.props.transformer)),
                resultHolder.state.rowDefinition
            ]);
        }

        return <Tabs
            selectedIndex={this.state.activeTabIndex}
            onSelect={newTabIndex => this.setState({activeTabIndex: newTabIndex})}>

            <TabList>
                {tabs.map((data) => {
                    let [name, list, rowDefinition] = data;
                    return <Tab key={name}>{name}</Tab>;
                })}
                <Tab key="Errors">Errors</Tab>
            </TabList>

            {tabs.map((data) => {
                let [name, list, rowDefinition] = data;
                return <TabPanel key={name}>
                    <ResultTable results={list} rowDefinition={rowDefinition}/>
                </TabPanel>;
            })}
            <TabPanel key="Errors">
                <ErrorTable errors={this.state.errors}/>
            </TabPanel>

        </Tabs>
    }
}
