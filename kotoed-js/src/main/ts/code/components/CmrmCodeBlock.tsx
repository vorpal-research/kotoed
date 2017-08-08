/// <reference types="codemirror/codemirror-runmode"/>
// ^^ this CANNOT be import since it should not be emitted to resulting JS

import * as cm from "codemirror"
import * as React from "react";
import "codemirror/addon/runmode/runmode"

import {editorModeParam, guessCmModeForLang, guessCmModeForLangOrExt, requireCmMode} from "../util/codemirror";

interface CodeBlockProps {
    literal: string
    language: string
}

export default class CmrmCodeBlock extends React.Component<CodeBlockProps> {
    output: HTMLPreElement;

    componentDidMount() {
        let mode = guessCmModeForLangOrExt(this.props.language);
        requireCmMode(mode);
        cm.runMode(this.props.literal, editorModeParam(mode), this.output);
    }

    render() {
        return <pre ref={(ref: HTMLPreElement) => this.output = ref}/>
    }

}