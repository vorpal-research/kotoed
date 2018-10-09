import * as React from "react";
import {Component} from "react";
import {Button, ListGroup, ListGroupItem, Panel} from "react-bootstrap";
import {List} from "immutable";
import * as _ from "lodash";
import {isArray, isObject} from "util";

import {formatKloneInfoAsHeader, KloneView} from "./kloneView";

export const ArrayColumn = ({value}: { value: List<any> }) =>
    <span>{value.join(", ")}</span>;

export const JsonColumn = ({value}: { value: any }) =>
    <pre><code>{JSON.stringify(value.toJS(), null, 2)}</code></pre>;

export const CodeColumn = ({value}: { value: any }) =>
    <pre><code>{value}</code></pre>;

export const KloneColumn = ({value}: { value: any }) => {
    return <KloneClassPanel value={value.toJS()}/>;
};

export interface KloneClassPanelProps {
    value: any
}

export interface KloneClassPanelState {
    open: boolean
}

export class KloneClassPanel extends Component<KloneClassPanelProps, KloneClassPanelState> {
    constructor(props: KloneClassPanelProps, context: undefined) {
        super(props, context);

        this.state = {
            open: false
        };
    }

    toggleOpen = () => {
        this.setState(
            (prevState: KloneClassPanelState) => {
                return {open: !prevState.open};
            }
        );
    };

    render() {
        let value = this.props.value;

        let baseKlone = value.kloneClass.find((klone: any) => value.baseSubmissionId === klone.submissionId);

        return <div>
            <Button block onClick={this.toggleOpen}>
                <div className="pull-left">
                    {`(${value.kloneClass.length - 1}) ${formatKloneInfoAsHeader(baseKlone)}`}
                </div>
            </Button>
            <Panel collapsible expanded={this.state.open}>
                <ListGroup>
                    {value.kloneClass.map((klone: any) => {
                        return baseKlone !== klone ? <ListGroupItem>
                            <KloneView leftKlone={baseKlone}
                                       rightKlone={klone}/>
                        </ListGroupItem> : null;
                    })}
                </ListGroup>
            </Panel>
        </div>;
    }
}


export interface UnknownFailureInfo {
    nestedException: string
}

export function isUnknownFailureInfo(failure: FailureInfo): failure is UnknownFailureInfo {
    return failure &&
        (failure as any)["@class"] === "org.jetbrains.research.runner.data.UnknownFailureDatum";
}

export interface TestFailureInfo {
    input: { [arg: string]: any }
    output: any
    expectedOutput: any
    nestedException: string
}

export function isTestFailureInfo(failure: FailureInfo): failure is TestFailureInfo {
    return failure &&
        (failure as any)["@class"] === "org.jetbrains.research.runner.data.TestFailureDatum";
}

export type FailureInfo = TestFailureInfo | UnknownFailureInfo


export interface TestData {
    status: string
    failure: FailureInfo
}

function prettyPrint(v: any) {
    return String(JSON.stringify(v, null, 2));
}

export function deepRenameKey(js: any, from: string, to: string): any {
    if (isArray(js)) {
        return js.map(e => deepRenameKey(e, from, to))
    } else if (isObject(js)) {
        return _.mapValues(
            _.mapKeys(
                js,
                (_, key: string) => from === key ? to : key
            ),
            value => deepRenameKey(value, from, to)
        );
    } else return js
}

export const TestDataColumn = ({value}: { value: List<TestData> }) =>
    <ul className="list-group">{
        (value.toJS() as TestData[]).map((td, idx) => {
            switch (td.status) {
                case "SUCCESSFUL": {
                    return <li key={`${value.hashCode()}-${idx}`}
                               className="list-group-item list-group-item-success">
                        Passed
                    </li>
                }
                case "NOT_IMPLEMENTED": {
                    return <li key={`${value.hashCode()}-${idx}`}
                               className="list-group-item list-group-item-secondary">
                        Not done
                    </li>
                }
                case "ABORTED":
                case "FAILED": {
                    let failure = td.failure;

                    if (isTestFailureInfo(failure)) {
                        return <li key={`${value.hashCode()}-${idx}`}
                                   className="list-group-item list-group-item-danger">
                            Failed with:
                            <ListGroup>
                                <ListGroupItem bsStyle="danger">
                                    Inputs:
                                    <ListGroup>
                                        {_.toPairs(failure.input)
                                            .filter(([k, _]) => !k.startsWith("@"))
                                            .map(([k, v]) => {
                                                return <ListGroupItem
                                                    bsStyle="danger">
                                                    <pre><code>{prettyPrint(k)} -> {prettyPrint(v)}</code></pre>
                                                </ListGroupItem>
                                            })}
                                    </ListGroup>
                                </ListGroupItem>
                                <ListGroupItem bsStyle="danger">
                                    Output:
                                    <pre><code>{prettyPrint(failure.output)}</code></pre>
                                </ListGroupItem>
                                <ListGroupItem bsStyle="danger">
                                    Expected output:
                                    <pre><code>{prettyPrint(failure.expectedOutput)}</code></pre>
                                </ListGroupItem>
                                <ListGroupItem bsStyle="danger">
                                    Nested exception:
                                    <pre><code>{prettyPrint(failure.nestedException)}</code></pre>
                                </ListGroupItem>
                            </ListGroup>
                        </li>
                    } else if (isUnknownFailureInfo(failure)) {
                        return <li key={`${value.hashCode()}-${idx}`}
                                   className="list-group-item list-group-item-danger">
                            Failed with:<br/>
                            <pre><code>{prettyPrint(failure.nestedException)}</code></pre>
                        </li>
                    }
                }
            }
        })
    }</ul>;


export namespace Bootstrap {

    export class Pagination extends Component<any> {
        render() {
            return (<nav aria-label="Submission result navigation">
                    <ul className="pagination">
                        <this.props.Previous/>
                        <this.props.Next/>
                    </ul>
                    <this.props.PageDropdown/>
                </nav>
            );
        }
    }

    export const PreviousButton = (props: any) => (
        <li key="pages-prev"
            className={props.hasPrevious ? "page-item" : "page-item disabled"}>
            <a className="page-link"
               href="#"
               aria-label="Previous"
               onClick={props.onClick}>
                <span aria-hidden="true">&laquo;</span>
                <span className="sr-only">Previous</span>
            </a>
        </li>
    );

    export const NextButton = (props: any) => (
        <li key="pages-next"
            className={props.hasNext ? "page-item" : "page-item disabled"}>
            <a className="page-link"
               href="#"
               aria-label="Next"
               onClick={props.onClick}>
                <span aria-hidden="true">&raquo;</span>
                <span className="sr-only">Next</span>
            </a>
        </li>
    );

    export const PageDropdown = (props: any) => (
        <ul className="pagination">
            {[...Array(props.maxPages).keys()].map(idx_ => {
                let idx = idx_ + 1;
                return <li key={`pages-${idx}`}
                           className={idx != props.currentPage ? "page-item" : "page-item active"}>
                    <a className="page-link"
                       href="#"
                       aria-label={`Page ${idx}`}
                       onClick={(_) => props.setPage(idx)}>
                        <span aria-hidden="true">{idx}</span>
                        <span className="sr-only">{`Page ${idx}`}</span>
                    </a>
                </li>
            })}
        </ul>
    );

} // namespace Bootstrap
