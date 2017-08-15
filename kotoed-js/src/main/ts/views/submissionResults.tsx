import * as React from "react";
import {Component} from "react";
import {render} from "react-dom";
import {connect} from "react-redux";
import Griddle, {
    ColumnDefinition,
    components,
    GriddleStyleConfig,
    RowDefinition
} from "griddle-react";

import {eventBus} from "../eventBus";
import {Kotoed} from "../util/kotoed-api";

import "less/kotoed-bootstrap/bootstrap.less";

type CellProps = components.CellProps
type RowProps = components.RowProps

enum VerificationStatus {
    Processed,
    NotReady,
    Invalid,
    Unknown
}

interface VerificationData {
    status: VerificationStatus
    errors: number[]
}

interface IdRequest {
    id: number
}

interface GenericResponse<T> {
    records: T[]
    verificationData: VerificationData
}

interface GenericError {
    id: number
    data: any
}

export interface SubmissionResultTableProps {
    submissionId: number
}

export interface SubmissionResultTableState {
    records: any[]
    errors: any[]
}

function sendAsync<Request, Response>(address: string, request: Request): Promise<Response> {
    return eventBus.awaitOpen().then(_ =>
        eventBus.send<Request, Response>(address, request)
    );
}

export class SubmissionResultTable<ResultT> extends Component<SubmissionResultTableProps, SubmissionResultTableState> {

    constructor(props: SubmissionResultTableProps, context: undefined) {
        super(props, context);

        this.state = {records: [], errors: []};

        sendAsync<IdRequest, GenericResponse<ResultT>>
        (Kotoed.Address.Api.Submission.Result.Read, {"id": this.props.submissionId})
            .then(r => this.processSubmissionResults(r))
            .catch(ex => this.setState({
                errors: [{
                    id: ex.failureCode,
                    data: ex.message
                }]
            }));
    }

    processSubmissionResults = (json: GenericResponse<ResultT>) => {
        let records = Promise.resolve(json.records);
        let errors = 0 < json.verificationData.errors.length
            ? sendAsync<VerificationData, GenericError[]>
            (Kotoed.Address.Api.Submission.Error, json.verificationData)
            : Promise.resolve([] as GenericError[]);

        Promise.all([records, errors])
            .then(res => {
                let [r, e] = res;
                this.setState({records: r, errors: e});
            });
    };

    regularStyleConfig: GriddleStyleConfig = {
        classNames: {
            Table: 'table table-bordered table-striped table-hover',
            TableHeading: 'bg-info'
        }
    };

    errorStyleConfig: GriddleStyleConfig = {
        classNames: {
            Table: 'table table-bordered table-striped table-hover',
            TableHeading: 'bg-danger'
        }
    };

    rowDataSelector = (state: any, {griddleKey}: CellProps & RowProps) => {
        return state
            .get("data")
            .find((rowMap: any) => rowMap.get("griddleKey") === griddleKey)
            .toJSON();
    };

    enhancedWithRowData = connect((state: any, ownProps: CellProps & RowProps) => {
        return {
            rowData: this.rowDataSelector(state, ownProps)
        };
    });

    succeededTests = ({rowData}: { rowData: any }) => {
        let total = rowData.body.testsuite.tests;
        let errors = rowData.body.testsuite.errors;
        let skipped = rowData.body.testsuite.skipped;
        let failures = rowData.body.testsuite.failures;

        return (
            <div>{total - (errors + skipped + failures)}</div>
        );
    };

    render() {
        if (0 == this.state.errors.length) {
            return <Griddle
                key="records"
                data={this.state.records}
                components={{
                    Filter: () => <span/>,
                    SettingsToggle: () => <span/>
                }}
                styleConfig={this.regularStyleConfig}
            >
                <RowDefinition>
                    <ColumnDefinition id="id"
                                      title="ID"/>
                    <ColumnDefinition id="body.testsuite.name"
                                      title="Type"/>
                    <ColumnDefinition id="body.testsuite.tests"
                                      title="Total"/>
                    <ColumnDefinition id="none"
                                      title="Success"
                                      customComponent={this.enhancedWithRowData(this.succeededTests)}/>
                    <ColumnDefinition id="body.testsuite.errors"
                                      title="Errors"/>
                    <ColumnDefinition id="body.testsuite.skipped"
                                      title="Skipped"/>
                    <ColumnDefinition id="body.testsuite.failures"
                                      title="Failures"/>
                </RowDefinition>
            </Griddle>;
        } else {
            return <Griddle
                key="errors"
                data={this.state.errors}
                components={{
                    Filter: () => <span/>,
                    SettingsToggle: () => <span/>
                }}
                styleConfig={this.errorStyleConfig}
            >
                <RowDefinition>
                    <ColumnDefinition id="id" title="Error ID"/>
                    <ColumnDefinition id="data" title="Error data"/>
                </RowDefinition>
            </Griddle>;
        }
    }
}


render(
    <SubmissionResultTable submissionId={39}/>,
    document.getElementById("view-submission-results")
);
