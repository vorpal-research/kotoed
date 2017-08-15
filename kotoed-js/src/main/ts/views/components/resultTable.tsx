import * as React from "react";
import {Component} from "react";

import Griddle, {components, GriddleStyleConfig} from "griddle-react";

import "less/kotoed-bootstrap/bootstrap.less";

export interface ResultTableProps<ResultT> {
    results: ResultT[]
    rowDefinition: components.RowDefinition
}

export class ResultTable<ResultT> extends Component<ResultTableProps<ResultT>, any> {

    constructor(props: ResultTableProps<ResultT>, context: undefined) {
        super(props, context);
    }

    styleConfig: GriddleStyleConfig = {
        classNames: {
            Table: 'table table-bordered table-striped table-hover',
            TableHeading: 'bg-info'
        }
    };

    render() {
        return <Griddle
            key="results"
            data={this.props.results}
            components={{
                Filter: () => <span/>,
                SettingsToggle: () => <span/>,
                NoResults: () =>
                    <div className="alert alert-warning" role="alert">
                        No data available
                    </div>
            }}
            styleConfig={this.styleConfig}
        >
            {this.props.rowDefinition}
        </Griddle>;
    }
}
