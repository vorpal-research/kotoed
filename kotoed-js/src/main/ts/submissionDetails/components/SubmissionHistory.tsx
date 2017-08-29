import * as React from "react";
import {Glyphicon} from "react-bootstrap";

import {SubmissionToRead} from "../../data/submission";
import {renderSubmissionTable} from "../../submissions/table";
import {SubmissionComponent} from "../../submissions/SubmissionComponent";

export interface SubmissionHistoryProps {
    items: Array<SubmissionToRead>
    onMore: (latestId: number) => void
}

export default class SubmissionHistory extends React.PureComponent<SubmissionHistoryProps> {
    private hasMore = (): boolean => this.props.items[this.props.items.length - 1].parentSubmissionId !== undefined;
    private nextId = (): number => {
        let ret = this.props.items[this.props.items.length - 1].parentSubmissionId;
        if (ret === undefined)
            throw new Error("Nothing to request");
        return ret;
    };

    private renderChildren = (): Array<JSX.Element> => {
        let ret = this.props.items.map(sub => <SubmissionComponent {...sub} key={sub.id} verificationData={{status: "Processed"}}/>);
        if (this.hasMore()) {
            ret.push(
                <tr key="more-submissions">
                    <td colSpan={5} onClick={() => this.props.onMore(this.nextId())} className="text-center">
                        <span className="text-primary"><Glyphicon glyph="menu-down"/>&nbsp;More&nbsp;<Glyphicon glyph="menu-down"/></span>
                    </td>
                </tr>);
        }
        return ret;
    };


    render() {
        return renderSubmissionTable(this.renderChildren());
    }
}