import * as React from "react";
import {Component} from "react";
import {Button, ListGroup, ListGroupItem, Panel} from "react-bootstrap";
import {List} from "immutable";
import * as _ from "lodash";
import {isArray, isObject} from "util";

import {formatKloneInfoAsHeader, KloneView} from "./kloneView";

import "less/submissionResults.less";

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
    nestedException?: string
    errorMessage?: string
}

export function isUnknownFailureInfo(failure: FailureInfo): failure is UnknownFailureInfo {
    return _.isObject(failure) && !isTestFailureInfo(failure)
        && (failure.nestedException !== undefined || failure.errorMessage !== undefined)
}

export interface TestFailureInfo {
    input: { [arg: string]: any }
    output: any
    expectedOutput: any
    nestedException: string
}

export function isTestFailureInfo(failure: FailureInfo): failure is TestFailureInfo {
    const failureObj = failure as any
    return _.isObject(failure)
        && failureObj.input !== undefined
        && failureObj.output !== undefined
        && failureObj.expectedOutput !== undefined
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

export interface TestDataColumnProps {
    value: any
}

export interface TestDataColumnState {
    open: boolean
}

export class TestDataColumn extends Component<TestDataColumnProps, TestDataColumnState> {
    constructor(props: KloneClassPanelProps, context: undefined) {
        super(props, context);

        this.state = {
            open: true
        };
    }

    toggleOpen = () => {
        this.setState(
            (prevState: TestDataColumnState) => {
                return {open: !prevState.open};
            }
        );
    };

    render() {
        let value = this.props.value;

        return <div className="test-data-column">
            <Panel collapsible expanded={this.state.open}>
                <ListGroup onDoubleClick={this.toggleOpen}>{
                    (value.toJS() as TestData[]).map((td, idx) => {
                        switch (td.status) {
                            case "SUCCESSFUL": {
                                return <ListGroupItem bsStyle="success">
                                    Passed
                                </ListGroupItem>
                            }
                            case "NOT_IMPLEMENTED": {
                                return <ListGroupItem bsStyle="info">
                                    Not done
                                </ListGroupItem>
                            }
                            case "ABORTED":
                            case "FAILED": {
                                let failure = td.failure;

                                if (isTestFailureInfo(failure)) {
                                    return <ListGroupItem bsStyle="danger">
                                        Failed with:
                                        <ListGroup>
                                            <ListGroupItem bsStyle="danger">
                                                Inputs:
                                                <ListGroup>
                                                    {_.toPairs(failure.input)
                                                        .filter(([k, v]) => !k.startsWith("@"))
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
                                    </ListGroupItem>
                                } else if (isUnknownFailureInfo(failure)) {
                                    return <ListGroupItem bsStyle="danger">
                                        Failed with:<br/>
                                        {
                                            failure.nestedException ?
                                                [<pre><code>{prettyPrint(failure.nestedException)}</code></pre>]
                                                : []
                                        }
                                        {
                                            failure.errorMessage ?
                                                [<pre><code>{failure.errorMessage}</code></pre>]
                                                : []
                                        }
                                    </ListGroupItem>
                                }
                            }
                        }
                    })
                }</ListGroup>
            </Panel>
        </div>;
    }
}


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
