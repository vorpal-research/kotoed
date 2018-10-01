import * as React from "react";
import twemoji from "twemoji";

interface TwemojiTextProps {
    text: string
}

export default class TwemojiText extends React.Component<TwemojiTextProps> {
    constructor(props: TwemojiTextProps) {
        super(props)
    }

    render() {
        return twemojify(this.props.text)
    }
}

export function twemojify(text: string): JSX.Element{
    return <span dangerouslySetInnerHTML={{__html: twemoji.parse(text)}}/>;
}

export function twemojifyNode(node: {literal: string}): JSX.Element | null {
    return <span dangerouslySetInnerHTML={{__html: twemoji.parse(node.literal)}}/>;
}
