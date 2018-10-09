import * as React from "react";
import * as Table from "react-bootstrap/lib/Table";
import * as FileSaver from "file-saver";
import * as Button from "react-bootstrap/lib/Button";
import * as CopyToClipboard from "react-copy-to-clipboard";
import * as Glyphicon from "react-bootstrap/lib/Glyphicon";
import {sendAsync, setStateAsync} from "./common";
import SpinnerWithVeil from "./SpinnerWithVeil";
import {SingleDatePicker} from "react-dates";
import * as moment from "moment";
import {DatePicker} from "./datePicker";

interface TableData {
    data: string[][]
}

export class SimpleTable extends React.Component<TableData, TableData> {
    constructor(props: TableData) {
        super(props);
        this.state = props
    }

    render(): JSX.Element {
        return <Table striped bordered condensed hover responsive>
            <thead>
            <tr>
                { this.state.data[0].map(it => <th scope="col">{it}</th>) }
            </tr>
            </thead>
            <tbody>
            {
                this.state.data.slice(1).map(it =>
                    <tr>
                        <th scope="row">{it[0]}</th>
                        {
                            it.slice(1).map(it =>
                                <td>{it}</td>
                            )
                        }
                    </tr>
                )
            }
            </tbody>
        </Table>
    }
}

export class ExportableTable extends React.Component<TableData, TableData> {
    constructor(props: TableData) {
        super(props);
        this.state = props;
    }

    getStateAsCSV = () => this.state.data.map(it => it.join(", ")).join("\n");
    getStateAsTSD = () => this.state.data.map(it => it.join("\t")).join("\n");

    onExport = () => {
        const blob = new Blob(
            [this.getStateAsCSV()],
            { type: "text/csv;charset=utf-8" }
        );
        FileSaver.saveAs(blob, "data.csv");
    };

    render(): JSX.Element {
        return <div>
            <div className="clearfix">
                {this.props.children}
                <div className="pull-right">
                    <Button onClick={this.onExport}>.CSV</Button>
                    <CopyToClipboard text={this.getStateAsCSV()}>
                        <Button><Glyphicon glyph="copy"/></Button>
                    </CopyToClipboard>
                </div>
            </div>
            <SimpleTable {...this.state} />
        </div>
    }
}

interface ReportTableProps {
    address : string
    [key: string]: any
}

type ReportTableState = TableData & {
    loading: boolean
    timestamp: number
}

export class ReportTable extends React.Component<ReportTableProps, ReportTableState> {
    constructor(props: ReportTableProps) {
        super(props);
        this.state = {
            data: [],
            loading: true,
            timestamp: moment().valueOf()
        };

        this.loadData()
    }

    loadData = async () => {
        const resp = await sendAsync(this.props.address, { ...this.props, timestamp: this.state.timestamp }) as TableData;
        await setStateAsync(this, { ...resp, loading: false })
    };

    onDateChange = async (date: moment.Moment) => {
        console.debug(`Date change: ${date && date.toISOString()}`);
        const ts = (date || moment()).valueOf();
        await setStateAsync(this, { loading: true, timestamp: ts });
        await this.loadData()
    };

    render(): JSX.Element {
        return this.state.loading ? <SpinnerWithVeil/>
            : <ExportableTable {...this.state} >
                <div className="pull-left">
                    <DatePicker
                        date={moment(this.state.timestamp)}
                        onDateChange={this.onDateChange}
                    />
                </div>
            </ExportableTable>
    }
}