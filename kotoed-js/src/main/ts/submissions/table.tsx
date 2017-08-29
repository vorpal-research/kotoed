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
                <th>Id</th>
                <th>Created at</th>
                <th>Revision</th>
                <th>Results</th>
                <th>Review</th>
            </tr>
            </thead>
            <tbody>
            {children}
            </tbody>
        </Table>)
}