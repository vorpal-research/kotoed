import * as React from "react";
import {CommentButton} from "./CommentButton";
import ReactMarkdown = require("react-markdown");
import CmrmCodeBlock from "./CmrmCodeBlock";

interface CommentFormProps {
    onSubmit: (text: string) => void
    notifyEditorAboutChange: () => void
    whoAmI: string
}

interface CommentFormState {
    editText: string
    editState: "edit"|"preview"
}

export default class CommentForm extends React.Component<CommentFormProps, CommentFormState> {
    constructor(props: CommentFormProps) {
        super(props);
        this.state = {
            editText: "",
            editState: "edit"
        }
    }

    getPanelClass = () => {
        return "panel-success"
    };

    renderPanelLabels = () => {
        return this.state.editState === "preview" ?
            <span key="preview" className="label label-warning">Preview</span> :
            null
    };


    handleEditButtonClick = () => {
        this.setState((prev) => {
            switch (prev.editState) {
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


    getEditButtonIcon = () => {
        switch (this.state.editState) {
            case "preview":
                return "pencil";
            case "edit":
                return "eye-open"
        }
    };

    getEditButtonText = () => {
        switch (this.state.editState) {
            case "preview":
                return "Edit";
            case "edit":
                return "Preview"
        }
    };

    renderEditButton = () => {
        return <CommentButton
            title={this.getEditButtonText()}
            icon={this.getEditButtonIcon()}
            onClick={this.handleEditButtonClick}/>;
    };

    renderPanelHeading = () => {
        return <div className="panel-heading comment-heading clearfix">
            <div className="pull-left">
                <b>{this.props.whoAmI}</b>{" will write:"}
                {" "}
                {this.renderPanelLabels()}
            </div>
            <div className="pull-right">
                {this.renderEditButton()}
            </div>
        </div>
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
            case "edit":
                return this.renderEditPanelBodyContent();
            case "preview":
                return this.renderPreviewPanelBodyContent();

        }
    };

    renderPanelFooter = () => {
        return <div className="panel-footer">
            <p>
                <button type="button" className="btn btn-success"
                        onClick={() => this.props.onSubmit(this.state.editText)}>
                    Send
                </button>
            </p>
        </div>;
    };


    componentDidUpdate(prevProps: CommentFormProps, prevState: CommentFormState) {
        if (this.state.editState != prevState.editState)
            this.props.notifyEditorAboutChange();
    }

    getTextAreaStyle = () => {
        switch(this.state.editState) {
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
    };

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