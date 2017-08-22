import * as React from "react";
import {List} from "immutable";

export const ArrayColumn = ({value}: { value: List<any> }) =>
    <span>{value.join(", ")}</span>;

interface UnknownFailureInfo {
    nestedException: string
}

function isUnknownFailureInfo(failure: FailureInfo): failure is UnknownFailureInfo {
    return (failure as any).class == "org.jetbrains.research.runner.data.UnknownFailureDatum"
}

type FailureInfo = (UnknownFailureInfo | UnknownFailureInfo)

interface TestData {
    status: string
    failure: FailureInfo
}

export const TestDataColumn = ({value}: { value: List<TestData> }) =>
    <ul className="list-group">{
        (value.toJS() as TestData[]).map(td => {
            switch (td.status) {
                case "SUCCESSFUL": {
                    return <li
                        className="list-group-item list-group-item-success">
                        Passed
                    </li>
                }
                case "FAILED": {
                    if (isUnknownFailureInfo(td.failure)) {
                        return <li
                            className="list-group-item list-group-item-danger">
                            Failed with:<br/>{td.failure.nestedException}
                        </li>
                    }
                }
            }
        })
    }</ul>;
