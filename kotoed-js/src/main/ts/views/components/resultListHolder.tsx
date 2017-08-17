import * as React from "react";
import {Component} from "react";
import {Tab, TabList, TabPanel, Tabs} from "react-tabs";
import {isArray} from "util";

import {components} from "griddle-react";

import {
    ErrorDesc,
    GenericResponse,
    sendAsync,
    VerificationData
} from "./common";
import {ErrorTable} from "./errorTable";
import {ResultHolder} from "./resultHolder";
import {ResultTable} from "./resultTable";

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

    protected processResults = (r: GenericResponse<ResultT>): void => {
        let records = Promise.resolve(r.records);
        let errors = 0 < r.verificationData.errors.length
            ? sendAsync<VerificationData, ErrorDesc[]>
            (Kotoed.Address.Api.Submission.Error, r.verificationData)
            : Promise.resolve([] as ErrorDesc[]);

        Promise.all([records, errors])
            .then(res => {
                let [r, e] = res;
                let activeTabIndex = e.length > 0
                    ? this.state.resultHolders.length
                    : 0;
                this.setState({
                    results: r,
                    errors: e,
                    activeTabIndex: activeTabIndex
                });
            });
    };

    render() {

        type TabData = [string, ResultT[], components.RowDefinition];

        let tabs: TabData[] = [];

        for (let resultHolder of this.state.resultHolders) {
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
