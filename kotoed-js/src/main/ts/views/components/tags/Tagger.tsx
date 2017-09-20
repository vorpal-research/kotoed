import * as React from "react";
import {Tag as TagData} from "../../../data/submission";
import {SimpleAutoSuggest} from "./SimpleAutosuggest";
import {Tag} from "./Tag";

interface TaggerClassNames {
    wrapper?: string
    inputWrapper?: string
    tagsWrapper?: string
}

interface TaggerProps {
    onTagAdd: (tag: TagData) => void
    onTagRemove: (tag: TagData) => void
    currentTags: Array<TagData>
    availableTags: Array<TagData>
    classNames?: TaggerClassNames

}

export class Tagger extends React.Component<TaggerProps> {
    private getClassName(key: keyof TaggerClassNames): string {
        if (this.props.classNames === undefined)
            return "";

        if (this.props.classNames[key] === undefined)
            return "";

        return this.props.classNames[key]!
    }

    private handleAdd = (tag: TagData) => {
        if (this.props.currentTags.some(t => t.id == tag.id))
            return;
        else
            this.props.onTagAdd(tag);
    };

    render() {
        return <div className={this.getClassName("wrapper")}>
            <div className={this.getClassName("tagsWrapper")}>
                {this.props.currentTags.map((tag, ix) =>
                    <Tag key={`tag-${ix}`} tag={tag} removable={true} onRemove={this.props.onTagRemove}/>)}
            </div>
            <div className={this.getClassName("inputWrapper")}>
                <SimpleAutoSuggest
                    values={this.props.availableTags}
                    onSelect={this.handleAdd}
                    valueToString={(v: TagData) => v.name}
                    renderSuggestion={(v: TagData) => <Tag tag={v} removable={false}/>}
                />
            </div>
        </div>
    }
}