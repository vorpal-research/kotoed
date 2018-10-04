import * as React from "react";
import {ButtonGroup, Button, Glyphicon, ButtonToolbar, OverlayTrigger, Tooltip} from "react-bootstrap";
import {EmojiPicker} from "./emoji";
import {MousetrapInstance} from "../../util/mousetrap";
import {CommentTemplate, CommentTemplates} from "../../code/remote/templates";
import * as _ from "lodash"
import {SimpleAutoSuggest} from "./tags/SimpleAutosuggest";
import * as Row from "react-bootstrap/lib/Row";
import Col = require("react-bootstrap/lib/Col");

import "@fortawesome/fontawesome-free/less/fontawesome.less"
import "@fortawesome/fontawesome-free/less/solid.less"
import "@fortawesome/fontawesome-free/less/brands.less"


interface TextEditorProps {
    text: string,
    onChange: (text: string) => void
    panelDisabled: boolean
    disabled: boolean
    onCtrlEnter: () => void
    onEscape: () => void
    commentTemplates: CommentTemplates
}

interface Selection {
    selectionStart: number
    selectionEnd: number
}

const URL_PLACEHOLDER = "url";

function charAtOrNewline(str: string, index: number) {
    if (index < 0 || index >= str.length)
        return "\n";

    return str[index];
}

function isSpaceAt(str: string, index: number) {
    return /\s/.test(charAtOrNewline(str, index))
}

export class TextEditor extends React.Component<TextEditorProps> {
    private textArea: HTMLTextAreaElement;
    private mousetrap: MousetrapInstance | undefined;
    private sortedTemplates: CommentTemplates;

    constructor(props: TextEditorProps) {
        super(props)

    }

    componentWillMount() {
        this.sortedTemplates = _.sortBy(this.props.commentTemplates, t => t.name.length).reverse()
    }

    componentWillReceiveProps(props: TextEditorProps) {
        if (this.props.commentTemplates != props.commentTemplates)
            this.sortedTemplates = _.sortBy(this.props.commentTemplates, t => t.name.length).reverse()
    }

    focus = () => {
        this.textArea.focus();
    };

    blur = () => {
        this.textArea.blur();
    };

    expandSelectionToWord = () => {
        if (this.props.disabled) return;

        let {selectionStart, selectionEnd} = this.textArea;

        if (selectionEnd - selectionStart != 0) return;

        if (isSpaceAt(this.props.text, selectionEnd)) return;

        while (!isSpaceAt(this.props.text, selectionStart)) {
            selectionStart--;
        }

        while (!isSpaceAt(this.props.text, selectionEnd)) {
            selectionEnd++;
        }

        this.textArea.setSelectionRange(
            selectionStart + 1,
            selectionEnd);
    };

    replaceWith = async (text: string) => {
        if (this.props.disabled) return;

        const {selectionStart, selectionEnd} = this.textArea;

        await this.props.onChange(
            this.props.text.substring(0, selectionStart) +
            text +
            this.props.text.substring(selectionEnd),
        );

        this.textArea.focus();
        if (selectionEnd - selectionStart !== 0)
            this.textArea.setSelectionRange(
                selectionStart,
                selectionStart + text.length);
        else
            this.textArea.setSelectionRange(
                selectionStart + text.length,
                selectionStart + text.length);
    };

    surroundWith = async (before: string, after: string, moveSelection_?: (selection: Selection) => Selection) => {
        if (this.props.disabled) return;

        this.expandSelectionToWord();

        const {selectionStart, selectionEnd} = this.textArea;

        const moveSelection = moveSelection_ || function(selection: Selection) {
            return {
                selectionStart: selection.selectionStart + before.length,
                selectionEnd: selection.selectionEnd + before.length
            }
        };

        await this.props.onChange(
            this.props.text.substring(0, selectionStart) +
            before +
            this.props.text.substring(selectionStart, selectionEnd) +
            after +
            this.props.text.substring(selectionEnd),
        );

        const newSelection = moveSelection({selectionStart, selectionEnd});

        this.textArea.focus();
        this.textArea.setSelectionRange(
            newSelection.selectionStart,
            newSelection.selectionEnd);
    };

    makeList = async (prefix: string) => {
        if (this.props.disabled) return;

        this.expandSelectionToWord();

        const {selectionStart, selectionEnd} = this.textArea;

        let nNewLinesBefore = 2;

        const prev1 = charAtOrNewline(this.props.text, this.textArea.selectionStart - 1) === "\n";
        const prev2 = charAtOrNewline(this.props.text, this.textArea.selectionStart - 2) === "\n";

        if (prev1 && prev2) {
            nNewLinesBefore = 0;
        } else if (prev1) {
            nNewLinesBefore = 1;
        }

        const newLinesBefore = "\n".repeat(nNewLinesBefore);

        let nNewLinesAfter = 2;

        const next1 = charAtOrNewline(this.props.text, this.textArea.selectionEnd + 1) === "\n";
        const next2 = charAtOrNewline(this.props.text, this.textArea.selectionEnd + 2) === "\n";

        if (next1 && next2) {
            nNewLinesAfter = 0;
        } else if (next2) {
            nNewLinesAfter = 1;
        }

        const newLinesAfter = "\n".repeat(nNewLinesBefore);

        const lines = this.props.text.substring(selectionStart, selectionEnd).split("\n");
        const newSelectionText =
            lines.map((str) => `${prefix}${str}`).join("\n");

        await this.props.onChange(
            this.props.text.substring(0, selectionStart) +
            newLinesBefore +
            newSelectionText +
            newLinesAfter +
            this.props.text.substring(selectionEnd),
        );

        this.textArea.focus();

        if (selectionEnd - selectionStart != 0)
            this.textArea.setSelectionRange(
                selectionStart + newLinesBefore.length,
                selectionEnd + newLinesBefore.length + lines.length * prefix.length);
        else
            this.textArea.setSelectionRange(
                selectionStart + newLinesBefore.length + prefix.length,
                selectionEnd + newLinesBefore.length + prefix.length);
    };

    makeHeader = () => this.surroundWith("### ", "");
    makeBold = () => this.surroundWith("**", "**");
    makeItalic = () => this.surroundWith("*", "*");
    makeLink = () => this.surroundWith("[", `](${URL_PLACEHOLDER})`, (selection) => {
        if (selection.selectionEnd - selection.selectionStart === 0)
            return {
                selectionStart: selection.selectionStart + 1,
                selectionEnd: selection.selectionEnd + 1
            };

        let selectionStart = selection.selectionStart + 1 +
            (selection.selectionEnd - selection.selectionStart) + 2;
        return {
            selectionStart,
            selectionEnd: selectionStart + URL_PLACEHOLDER.length
        }
    });

    makeCode = () => {
        const selected = this.props.text.substring(this.textArea.selectionStart, this.textArea.selectionEnd);
        const lineNum = selected.split("\n").length;

        if (lineNum === 1) {
            return this.surroundWith("`", "`")
        } else {
            return this.surroundWith("```\n", "\n```")
        }
    };

    makeQuote = () => this.makeList("> ");
    makeBulleted = () => this.makeList("- ");
    makeNumbered = () => this.makeList("1. ");


    suggestCommentFull = async () => {
        if (this.props.disabled) return;

        let {selectionStart, selectionEnd} = this.textArea;

        const selected = this.props.text.substring(this.textArea.selectionStart, this.textArea.selectionEnd);
        const trimmedRight = _.trimEnd(selected);
        const rightSpaces = selected.length - trimmedRight.length;
        const trimmed = _.trimStart(trimmedRight);
        const leftSpaces = trimmedRight.length - trimmed.length;

        selectionStart += leftSpaces;
        selectionEnd -= rightSpaces;

        this.textArea.setSelectionRange(selectionStart, selectionEnd);

        const selectedNoSp = this.props.text.substring(selectionStart, selectionEnd);

        const template = this.sortedTemplates.find(template => template.name.trim() == selectedNoSp.trim());

        template && this.replaceWith(template.text)
    };

    suggestCommentEndsWith = async () => {
        if (this.props.disabled) return;

        let {selectionStart, selectionEnd} = this.textArea;
        let processed = this.props.text.substring(0, selectionEnd);
        let trimmed = _.trimEnd(processed);
        const rightSpaces = processed.length - trimmed.length;

        const template = this.sortedTemplates.find(template => trimmed.endsWith(template.name.trim()));

        if (!template) return;

        const tmpSelectionStart = selectionStart - rightSpaces - template.name.trim().length;

        this.textArea.setSelectionRange(tmpSelectionStart, selectionEnd);

        await this.replaceWith(template.text);

        this.textArea.setSelectionRange(
            selectionStart + template.text.length - (template.name.length + rightSpaces),
            selectionEnd + template.text.length - (template.name.length + rightSpaces));
    };

    suggestComment() {
        if (this.props.disabled) return;

        let {selectionStart, selectionEnd} = this.textArea;

        if (selectionEnd - selectionStart > 0) {
            this.suggestCommentFull()
        } else {
            this.suggestCommentEndsWith()
        }
    }

    componentWillUnmount() {
        this.mousetrap && this.mousetrap.unbind("mod+enter");
        this.mousetrap && this.mousetrap.unbind("ctrl+space");
        this.mousetrap && this.mousetrap.unbind("escape");
    }

    componentDidMount() {
        this.mousetrap = new Mousetrap(this.textArea);
        this.mousetrap.bind("mod+enter", () =>
            this.props.disabled || this.props.onCtrlEnter());
        this.mousetrap.bind("ctrl+space", () =>
            this.props.disabled || this.suggestComment());
        this.mousetrap.bind("escape", () => this.props.disabled || this.props.onEscape());
    }

    renderPanel() {
        return <Row>
                <Col md={7}>
                    <ButtonToolbar>
                        <ButtonGroup>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="header-tooltip">Header</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeHeader}>
                                    <i className="fas fa-heading"/>
                                </Button>
                            </OverlayTrigger>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="bold-tooltip">Bold</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeBold}>
                                    <i className="fas fa-bold"/>
                                </Button>
                            </OverlayTrigger>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="italic-tooltip">Italic</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeItalic}>
                                    <i className="fas fa-italic"/>
                                </Button>
                            </OverlayTrigger>
                        </ButtonGroup>
                        <ButtonGroup>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="link-tooltip">Link</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeLink}>
                                    <i className="fas fa-link"/>
                                </Button>
                            </OverlayTrigger>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="code-tooltip">Code</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeCode}>
                                    <i className="fas fa-code"/>
                                </Button>
                            </OverlayTrigger>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="quote-tooltip">Quote</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeQuote}>
                                    <i className="fas fa-quote-right"/>
                                </Button>
                            </OverlayTrigger>
                        </ButtonGroup>
                        <ButtonGroup>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="bulleted-tooltip">Bulleted list</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeBulleted}>
                                    <i className="fas fa-list-ul"/>
                                </Button>
                            </OverlayTrigger>
                            <OverlayTrigger placement="top" overlay={<Tooltip id="numbered-tooltip">Numbered list</Tooltip>}>
                                <Button bsSize="sm" onClick={this.makeNumbered}>
                                    <i className="fas fa-list-ol"/>
                                </Button>
                            </OverlayTrigger>
                        </ButtonGroup>
                        <ButtonGroup>
                            <EmojiPicker onPick={(emoji) => emoji && this.replaceWith(emoji)}/>
                        </ButtonGroup>

                    </ButtonToolbar>
                </Col>
                <Col md={5}>
                    {this.props.commentTemplates.length > 0 && <SimpleAutoSuggest
                        values={this.props.commentTemplates}
                        onSelect={(template: CommentTemplate) => this.replaceWith(template.text)}
                        valueToString={(template: CommentTemplate) => template.name}
                        renderSuggestion={(template: CommentTemplate) => <span>{template.name}</span>}
                        placeholder="Comment templates"
                    />}
                </Col>
            </Row>
    }

    render() {
        return <div>
            {!this.props.panelDisabled && this.renderPanel()}
            <div className="vspace-10" key="spacer"/>
            <textarea
                key={`comment-form-editor`}
                disabled={this.props.disabled}
                className="form-control"
                ref={ref => this.textArea = ref!}
                rows={5}
                id="comment"
                value={this.props.text}
                style={{
                    resize: "none"
                }}
                onChange={(event) => this.props.onChange(event.target.value)}/>
        </div>
    }

}