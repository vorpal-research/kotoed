import * as React from "react";
import {Component} from "react";

import Griddle, {
    ColumnDefinition,
    GriddleStyleConfig,
    RowDefinition
} from "griddle-react";

import {ErrorDesc} from "./common";
import {JsonColumn} from "./griddleEx";

import "less/kotoed-bootstrap/bootstrap.less";

export interface ErrorTableProps {
    errors: ErrorDesc[]
}

export class ErrorTable extends Component<ErrorTableProps, any> {

    constructor(props: ErrorTableProps, context: undefined) {
        super(props, context);
    }

    styleConfig: GriddleStyleConfig = {
        classNames: {
            Table: 'table table-bordered table-striped table-hover',
            TableHeading: 'bg-danger'
        }
    };

    render() {
        return <Griddle
            key="errors"
            data={this.props.errors}
            components={{
                Filter: () => <span/>,
                SettingsToggle: () => <span/>,
                Pagination: () => <span/>,
                NoResults: () =>
                    <div className="alert alert-warning" role="alert">
                        No data available
                    </div>
            }}
            styleConfig={this.styleConfig}
        >
            <RowDefinition>
                <ColumnDefinition id="id" title="Error ID"/>
                <ColumnDefinition id="data.error" title="Error data"/>
                <ColumnDefinition id="data"
                                  title="Details"
                                  customComponent={JsonColumn}/>
            </RowDefinition>
        </Griddle>;
    }
}
