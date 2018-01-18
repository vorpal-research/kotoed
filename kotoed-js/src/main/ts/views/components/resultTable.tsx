import * as React from "react";
import {Component} from "react";

import Griddle, {components, GriddleStyleConfig, plugins} from "griddle-react";

import "sass/kotoed-bootstrap/bootstrap.sass";
import {Bootstrap} from "./griddleEx";
import Pagination = Bootstrap.Pagination;
import PreviousButton = Bootstrap.PreviousButton;
import NextButton = Bootstrap.NextButton;
import PageDropdown = Bootstrap.PageDropdown;

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
            TableHeading: 'bg-info',

            Filter: "form-control"
        }
    };

    render() {
        return <Griddle
            key="results"
            data={this.props.results}
            pageProperties={{pageSize: 20}}
            plugins={[plugins.LocalPlugin]}
            components={{
                // Filter: () => <span/>,
                Pagination: Pagination,
                PreviousButton: PreviousButton,
                NextButton: NextButton,
                PageDropdown: PageDropdown,
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
