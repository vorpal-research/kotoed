import * as React from "react";
import {Tag as TagData} from "../../../data/submission";
import {Label} from "react-bootstrap";

import "less/tags.less"

interface TagProps {
    tag: TagData,
    removable: boolean
    onRemove?: (tag: TagData) => void
}

export class Tag extends React.Component<TagProps> {
    handleRemove = () => {
        this.props.onRemove && this.props.onRemove(this.props.tag);
    };
    render() {
        return <Label bsStyle="default" className="tag">
            {this.props.tag.name}
            {this.props.removable && <a className="tags-remove" onClick={this.handleRemove}>&times;</a>}
        </Label>
    }
}