import * as $ from "jquery"
import "bootstrap-less"
import * as React from "react";
import * as ReactMarkdown from "react-markdown";
import * as moment from "moment";

import {Comment} from "../state";
import {highlight, languages} from "prismjs";

type CommentProps = Comment & {
    onUnresolve: (id: number) => void
    onResolve: (id: number) => void
}

function CodeBlock(props: {literal: string, language: string}) {
    let html = highlight(props.literal, languages[props.language]);
    let cls = 'language-' + props.language;

    return (<pre className={cls}>
      <code
          dangerouslySetInnerHTML={{__html: html}}
          className={cls}
      />
    </pre>
    )
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