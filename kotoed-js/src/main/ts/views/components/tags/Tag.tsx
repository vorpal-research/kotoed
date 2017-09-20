import * as React from "react";
import * as tinycolor from "tinycolor2"
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

    getColor = (): string | undefined => {
        let {tag} = this.props;

        if (tag.style.color)
            return tag.style.color;

        if (!tag.style.backgroundColor)
            return undefined;

        let bg = tag.style.backgroundColor;

        if (tinycolor(bg).isDark()) {
            return "#fff"
        } else {
            return "#000"
        }
    };

    getXColor = (color: string) => {
        let tc = tinycolor(color);
        if (tc.isDark()) {
            return tc.lighten(20);
        } else {
            return tc.darken(20);
        }
    };

    render() {
        let color = this.getColor();
        let xColor = this.getXColor(color || "white");
        return <Label style={{...this.props.tag.style, color}} bsStyle="default" className="tag">
            {this.props.tag.name}
            {this.props.removable &&
                <span
                    style={{color: xColor}}
                    className="tags-remove"
                    onClick={this.handleRemove}>&times;
                </span>}
        </Label>
    }
}