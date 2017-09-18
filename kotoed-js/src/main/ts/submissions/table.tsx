import * as React from "react";
import {Table,
    Glyphicon,
    Tooltip,
    OverlayTrigger} from "react-bootstrap";

export function renderSubmissionTable(children: Array<JSX.Element>): JSX.Element {
    return (
        <Table striped bordered condensed hover responsive>
            <thead>
            <tr>
                <th className="col-md-1">Id</th>
                <th className="col-md-4">Created at</th>
                <th className="col-md-3">Revision</th>
                <th className="col-md-2">Results</th>
                <th className="col-md-2">Review</th>
            </tr>
            </thead>
            <tbody>
            {children}
            </tbody>
        </Table>)
}