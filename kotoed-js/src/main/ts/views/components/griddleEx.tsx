import * as React from "react";
import {Component} from "react";
import {List} from "immutable";

export const ArrayColumn = ({value}: { value: List<any> }) =>
    <span>{value.join(", ")}</span>;

export const JsonColumn = ({value}: { value: any }) =>
    <pre><code>{JSON.stringify(value.toJS(), null, 2)}</code></pre>;

export interface UnknownFailureInfo {
    nestedException: string
}

export function isUnknownFailureInfo(failure: FailureInfo): failure is UnknownFailureInfo {
    return failure && (failure as any).class == "org.jetbrains.research.runner.data.UnknownFailureDatum"
}

export type FailureInfo = (UnknownFailureInfo | UnknownFailureInfo)

export interface TestData {
    status: string
    failure: FailureInfo
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
                case "FAILED": {
                    if (isUnknownFailureInfo(td.failure)) {
                        return <li key={`${value.hashCode()}-${idx}`}
                                   className="list-group-item list-group-item-danger">
                            Failed with:<br/>{td.failure.nestedException}
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
                       onClick={(e) => props.setPage(idx)}>
                        <span aria-hidden="true">{idx}</span>
                        <span className="sr-only">{`Page ${idx}`}</span>
                    </a>
                </li>
            })}
        </ul>
    );

}
