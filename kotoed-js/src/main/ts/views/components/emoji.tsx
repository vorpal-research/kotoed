import * as React from "react";
import twemoji from "twemoji";
import {emojiIndex, Picker} from "emoji-mart";
import {BaseEmoji} from "emoji-mart/dist-es/utils/emoji-index/nimble-emoji-index";
import {Button, Glyphicon, OverlayTrigger, Tooltip} from "react-bootstrap/lib";
import enhanceWithClickOutside = require("react-click-outside");

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
    const re = /:([\w-+@]+):/g;
    let result: RegExpExecArray|null;
    return text.replace(re, (match, name) => {
        // Undoing changes done in Picker
        const replacement = (emojiIndex.emojis[name.replace(/\+/g, "_").replace(/@plus@/, "+")] as BaseEmoji);
        if (!replacement)
            return match;
        return replacement.native;
    })
}

export function twemojify(text: string, enlarge?: boolean): JSX.Element{
    return <span dangerouslySetInnerHTML={{__html: twemoji.parse(toNative(text), enlarge ? {className: "emoji bigger"} : {})}}/>;
}

export function twemojifyNode(node: {literal: string}): JSX.Element | null {
    return <span dangerouslySetInnerHTML={{__html: twemoji.parse(toNative(node.literal), {className: "emoji bigger"})}}/>;
}


interface ExpandableEmojiPickerProps {
    onPick: (emoji: string|undefined) => void
}

interface ExpandableEmojiPickerState {
    expanded: boolean
}

class EmojiPicker_ extends React.Component<ExpandableEmojiPickerProps, ExpandableEmojiPickerState> {
    constructor(props: ExpandableEmojiPickerProps) {
        super(props);
        this.state = {
            expanded: false
        }
    }

    handleClickOutside() {
        this.setState({expanded: false})
    }

    getStyle = () => {
        let style: React.CSSProperties = {
            position: 'absolute',
            top: '20px', left: '20px',
            zIndex: 999
        };
        if (!this.state.expanded) {
            style.display = "none"
        }
        return style
    };

    render() {
        return <div>
            <OverlayTrigger placement="top" overlay={<Tooltip id="bulleted-tooltip">Emoji picker</Tooltip>}>
            <Button bsSize="sm" onClick={() => this.setState({expanded: !this.state.expanded})}>
                <TwemojiText text="😀"/>
            </Button>
            </OverlayTrigger>
            <Picker
                style={this.getStyle()}
                title='Pick your emoji…'
                emoji='point_up'
                set="twitter"
                emojiSize={16}
                sheetSize={20}
                showPreview={false}
                onSelect={(emoji) => {
                    // Well, it looks like shit now.
                    // _ -> + replacement is because undersores are separate tokens in Markdown.
                    // + -> @plus@ is because some emojis actually use + in their names
                    this.props.onPick(emoji && emoji.colons && emoji.colons.replace(/\+/, "@plus@").replace(/_/g, "+"));
                    this.setState({expanded: false});
                }}
            />
        </div>
    }
}

export const EmojiPicker = enhanceWithClickOutside(EmojiPicker_);