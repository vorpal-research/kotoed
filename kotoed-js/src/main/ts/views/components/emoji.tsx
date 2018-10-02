import * as React from "react";
import twemoji from "twemoji";
import {emojiIndex} from "emoji-mart";
import {BaseEmoji} from "emoji-mart/dist-es/utils/emoji-index/nimble-emoji-index";

interface TwemojiTextProps {
    text: string,
    enlarge?: boolean
}

export class TwemojiText extends React.Component<TwemojiTextProps> {
    constructor(props: TwemojiTextProps) {
        super(props)
    }

    render() {
        return twemojify(this.props.text, this.props.enlarge)
    }
}

function toNative(text: string): string {
    const re = /:(\w+):/;
    let result: RegExpExecArray|null;
    return text.replace(re, (match, name) => {
        return (emojiIndex.emojis[name] as BaseEmoji).native || match;
    })
}

export function twemojify(text: string, enlarge?: boolean): JSX.Element{
    return <span dangerouslySetInnerHTML={{__html: twemoji.parse(toNative(text), enlarge ? {className: "emoji bigger"} : {})}}/>;
}

export function twemojifyNode(node: {literal: string}): JSX.Element | null {
    return <span dangerouslySetInnerHTML={{__html: twemoji.parse(toNative(node.literal), {className: "emoji bigger"})}}/>;
}
