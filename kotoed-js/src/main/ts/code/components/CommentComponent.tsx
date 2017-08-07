/// <reference types="codemirror/codemirror-runmode"/>
// ^^ this CANNOT be import since it should not be emitted to resulting JS
import * as cm from "codemirror"
import "codemirror/addon/runmode/runmode"
import "bootstrap-less"
import * as React from "react";
import * as ReactMarkdown from "react-markdown";
import * as moment from "moment";

import {Comment, CommentState} from "../state/comments";
import {editorModeParam, guessCmModeForExt, requireCmMode} from "../util/codemirror";
import {CommentButton} from "./CommentButton";

interface CodeBlockProps {
    literal: string
    language: string
}

class CmrmCodeBlock extends React.Component<CodeBlockProps> {
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


type CommentProps = Comment & {
    onUnresolve: (id: number) => void
    onResolve: (id: number) => void
    notifyEditorAboutChange: () => void
    onEdit: (id: number, newText: string) => void
}

interface CommentComponentState {
    editState: "display" | "edit" | "preview"
    editText: string
}

export default class CommentComponent extends React.Component<CommentProps, CommentComponentState> {
    constructor(props: CommentProps) {
        super(props);
        this.state = {
            editState: "display",
            editText: ""
        }
    }

    getPanelClass = () => {
        if (this.props.state == "open")
            return "panel-primary";
        else
            return "panel-default"
    };

    renderPanelLabels = () => {
        let labels: Array<JSX.Element> = [];

        if (this.props.state === "closed")
            labels.push(<span key="resolved" className="label label-default">Resolved</span>);

        if (this.state.editState === "preview")
            labels.push(<span key="preview" className="label label-warning">Preview</span>);

        return labels
    };

    handleStateChange = () => {
        if (this.props.state == "closed")
            this.props.onUnresolve(this.props.id);
        else
            this.props.onResolve(this.props.id);
    };

    handleEditButtonClick = () => {
        this.setState((prev, props) => {
            switch (prev.editState) {
                case "display":
                    return {
                        editState: "edit",
                        editText: props.text
                    };
                case "preview":
                    return {
                        ...prev,
                        editState: "edit"
                    };
                case "edit":
                    return {
                        ...prev,
                        editState: "preview"
                    };
            }
        });
    };

    getStateButtonIcon = () => {
        if (this.props.state == "closed")
            return "remove-circle";
        else
            return "ok-circle";
    };

    getStateButtonText = () => {
        if (this.props.state == "closed")
            return "Unresolve";
        else
            return "Resolve";
    };

    renderStateButton = () => {
        if (!this.props.canStateBeChanged) {
            return null;
        }

        if (this.state.editState !== "display")
            return null;

        return <CommentButton
            title={this.getStateButtonText()}
            icon={this.getStateButtonIcon()}
            onClick={this.handleStateChange}/>;
    };

    getEditButtonIcon = () => {
        switch (this.state.editState) {
            case "display":
            case "preview":
                return "pencil";
            case "edit":
                return "eye-open"
        }
    };

    getEditButtonText = () => {
        switch (this.state.editState) {
            case "display":
            case "preview":
                return "Edit";
            case "edit":
                return "Preview"
        }
    };

    renderEditButton = () => {
        if (!this.props.canBeEdited)
            return null;

        return <CommentButton
            title={this.getEditButtonText()}
            icon={this.getEditButtonIcon()}
            onClick={this.handleEditButtonClick}/>;
    };

    renderPanelHeading = () => {
        return <div className="panel-heading comment-heading clearfix">
            <div className="pull-left">
                <b>{this.props.denizenId}</b>
                {` @ ${moment(this.props.datetime).format('LLLL')}`}
                {" "}
                {this.renderPanelLabels()}
            </div>
            <div className="pull-right">
                {this.renderEditButton()}
                {this.renderStateButton()}
            </div>
        </div>
    };

    renderDisplayPanelBodyContent = () => {
        return <ReactMarkdown
            source={this.props.text}
            className="comment-markdown"
            renderers={{CodeBlock: CmrmCodeBlock}}/>
    };

    renderEditPanelBodyContent = () => {
        return null;
    };

    renderPreviewPanelBodyContent = () => {
        return <ReactMarkdown
                source={this.state.editText}
                className="comment-markdown"
                renderers={{CodeBlock: CmrmCodeBlock}}/>;
    };

    renderPanelBodyContent = () => {
        switch (this.state.editState) {
            case "display":
                return this.renderDisplayPanelBodyContent();
            case "edit":
                return this.renderEditPanelBodyContent();
            case "preview":
                return this.renderPreviewPanelBodyContent();

        }
    };

    renderEditPreviewPanelFooter = () => {
        return <div className="panel-footer">
            <p>
                <button type="button" className="btn btn-success"
                        onClick={() => this.props.onEdit(this.props.id, this.state.editText)}>
                    Send
                </button>
                {" "}
                <button type="button" className="btn btn-danger"
                        onClick={() => {
                            this.setState({
                                editState: "display",
                                editText: ""
                            })
                        }}>
                    Cancel
                </button>
            </p>
        </div>;
    };

    renderPanelFooter = () => {
        switch (this.state.editState) {
            case "display":
                return null;
            case "edit":
            case "preview":
                return this.renderEditPreviewPanelFooter();

        }
    };

    componentWillReceiveProps(nextProps: CommentProps) {
        this.setState({
            editState: "display",
            editText: ""
        })
    };

    componentDidUpdate(prevProps: CommentProps, prevState: CommentComponentState) {
        if (this.state.editState != prevState.editState)
            this.props.notifyEditorAboutChange();
    }

    getTextAreaStyle = () => {
        switch(this.state.editState) {
            case "display":
            case "preview":
                return {
                    display: "none"
                };
            case "edit":
                return {};
        }
    };

    renderEditArea = () => {
        return <div style={this.getTextAreaStyle()}>
            {/* Trying to cheat on React here to preserve Ctrl-Z history on text area when switching edit<->preview */}
            <textarea
                className="form-control"
                rows={5}
                id="comment"
                value={this.state.editText}
                style={{
                    resize: "none"
                }}
                onChange={(event) => this.setState({editText: event.target.value})}/>
        </div>
    }

    render() {
        return (
            <div className={`panel ${this.getPanelClass()} comment`}>
                {this.renderPanelHeading()}
                <div className="panel-body">
                    {this.renderPanelBodyContent()}
                    {this.renderEditArea()}
                </div>
                {this.renderPanelFooter()}

            </div>
        );
    }
}