/// <reference types="codemirror/codemirror-runmode"/>
// ^^ this CANNOT be import since it should not be emitted to resulting JS
import * as cm from "codemirror"
import "codemirror/addon/runmode/runmode"
import * as $ from "jquery"
import "bootstrap-less"
import * as React from "react";
import * as ReactMarkdown from "react-markdown";
import * as moment from "moment";

import {Comment} from "../state";
import {editorModeParam, guessCmModeForExt, requireCmMode} from "../util/codemirror";

type CommentProps = Comment & {
    onUnresolve: (id: number) => void
    onResolve: (id: number) => void
}

interface CodeBlockProps {
    literal: string
    language: string
}

class CodeBlock extends React.Component<CodeBlockProps> {
    output: HTMLPreElement;

    componentDidMount() {
        let mode = guessCmModeForExt(this.props.language);
        requireCmMode(mode);
        cm.runMode(this.props.literal, editorModeParam(mode), this.output);
    }

    render() {
        return <pre ref={(ref: HTMLPreElement) => this.output = ref}/>
    }

}

export default class CommentComponent extends React.Component<CommentProps, {}> {
    getPanelClass = () => {
        if (this.props.state == "open")
            return "panel-primary";
        else
            return "panel-default"
    };

    renderPanelLabel = () => {
        if (this.props.state == "open")
            return null;
        else
            return <span className="label label-default">Resolved</span>;
    };

    handleStateChange = () => {
        if (this.props.state == "closed")
            this.props.onUnresolve(this.props.id);
        else
            this.props.onResolve(this.props.id);
    };

    getStateButtonIcon = () => {
        if (this.props.state == "closed")
            return "glyphicon-remove-circle";
        else
            return "glyphicon-ok-circle";
    };

    getStateButtonText = () => {
        if (this.props.state == "closed")
            return "Unresolve";
        else
            return "Resolve";
    };

    renderOpenCloseButton = () => {
        if (!this.props.canStateBeChanged)
            return null;

        return <div ref={(me: HTMLDivElement) => $(me).tooltip()}
                    className="comment-state-change-button"
                    onClick={this.handleStateChange}
                    data-toggle="tooltip"
                    data-placement="left"
                    title={this.getStateButtonText()}>
            <span className={`glyphicon ${this.getStateButtonIcon()}`} />
        </div>;
    };

    render() {
        return (
            <div className={`panel ${this.getPanelClass()} comment`}>
                <div className="panel-heading comment-heading clearfix">
                    <div className="pull-left">
                        <b>{this.props.denizenId}</b>
                        {` @ ${moment(this.props.datetime).format('LLLL')}`}
                        {" "}
                        {this.renderPanelLabel()}
                    </div>
                    <div className="pull-right">
                        {this.renderOpenCloseButton()}
                    </div>
                </div>
                <div className="panel-body">
                    <ReactMarkdown
                        source={this.props.text}
                        className="comment-markdown"
                        renderers={{CodeBlock: CodeBlock}}/>
                </div>
            </div>
        );
    }
}