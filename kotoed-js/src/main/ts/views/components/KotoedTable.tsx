import {KotoedLink} from "./Link";
import {Kotoed} from "../../util/kotoed-api";
import {Table} from "react-bootstrap";
import * as React from "react";
import {Submission} from "../../data/submission";
import UrlPattern = Kotoed.UrlPattern;
import moment = require("moment");

type KotoedCellContents = JSX.Element | string | null

type KotoedColumnDescription<State> = {
    element: (state: State) => KotoedCellContents
    cellClassName?: string
}

export type KotoedTableDescription<State> = [KotoedCellContents, KotoedColumnDescription<State>][]

export type KotoedTableProps<State> = {
    data: State[]
    description: KotoedTableDescription<State>
}

export class KotoedTable<State> extends React.PureComponent<KotoedTableProps<State>> {
    constructor(props: KotoedTableProps<State>) {
        super(props);
        this.state = {}
    }

    render(): JSX.Element {
        return <Table striped bordered condensed hover responsive>
            <thead>
            <tr>
                {
                    this.props.description.map(kv => {
                        const k = kv[0];
                        const v = kv[1];
                        return <th className={v.cellClassName}>{k}</th>
                    })
                }
            </tr>
            </thead>
            <tbody>
            {
                this.props.data.map((state, i) => <tr>
                    {
                        this.props.description.map(kv => {
                            const v = kv[1];
                            return <td className={v.cellClassName}>{v.element(state)}</td>
                        })
                    }
                </tr>)
            }
            </tbody>
        </Table>
    }
}
