import * as React from "react";
import * as tinycolor from "tinycolor2"
import {Tag as TagData} from "../../../data/submission";
import {Label} from "react-bootstrap";

import "sass/tags.sass"

interface TagProps {
    tag: TagData,
    removable: boolean
    onRemove?: (tag: TagData) => void
    onClick?: () => void
    disabled?: boolean
}

export class Tag extends React.Component<TagProps> {
    private handleRemove = () => {
        !this.isDisabled() && this.props.onRemove && this.props.onRemove(this.props.tag);
    };

    private getColor = (): string | undefined => {
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

    private getXColor = (color: string) => {
        let tc = tinycolor(color);
        if (tc.isDark()) {
            return tc.lighten(20);
        } else {
            return tc.darken(20);
        }
    };

    private isDisabled = () => {
        if (this.props.disabled === undefined)
            return false;

        return this.props.disabled
    };

    render() {
        let color = this.getColor();
        let xColor = this.getXColor(color || "white");
        let cursorType = this.props.onClick ? { cursor: "pointer" } : {};
        return <Label style={{...this.props.tag.style, color, ...cursorType}} bsStyle="default" className="tag"
                      onClick={this.props.onClick}>
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