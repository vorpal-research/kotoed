import * as React from "react";
import {CommentButton} from "./CommentButton";
import ReactMarkdown = require("react-markdown");
import CmrmCodeBlock from "./CmrmCodeBlock";
import {Button, Panel, Label} from "react-bootstrap";
import {FormState} from "../state/forms";

import Mousetrap, {MousetrapInstance} from "../../util/mousetrap"

interface CommentFormProps {
    onSubmit: (text: string) => void
    notifyEditorAboutChange: () => void
    whoAmI: string
    formState: FormState
}

interface CommentFormState {
    editText: string
    editState: "edit"|"preview"
}

export default class CommentForm extends React.Component<CommentFormProps, CommentFormState> {
    private textArea: HTMLTextAreaElement;

    constructor(props: CommentFormProps) {
        super(props);
        this.state = {
            editText: "",
            editState: "edit"
        }
    }

    getPanelClass = () => {
        return "success"
    };

    renderPanelLabels = () => {
        return this.state.editState === "preview" ?
            <Label key="preview" bsStyle="warning">Preview</Label> :
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
        return <div className="comment-heading clearfix">
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
        return <p>
            <Button bsStyle="success"
                    onClick={() => this.props.onSubmit(this.state.editText)}
                    disabled={this.props.formState.processing}>
                Send
            </Button>
        </p>;
    };

    private mousetrap: MousetrapInstance | undefined;
    componentWillUnmount() {
        this.mousetrap && this.mousetrap.unbind("mod+enter")
    }

    componentDidMount() {
        this.mousetrap = new Mousetrap(this.textArea);
        this.mousetrap.bind("mod+enter", () =>
            this.props.formState.processing || this.props.onSubmit(this.state.editText));
        if (!this.props.formState.processing)
            this.textArea.focus();
    }

    componentDidUpdate(prevProps: CommentFormProps, prevState: CommentFormState) {
        if (this.state.editState != prevState.editState)
            this.props.notifyEditorAboutChange();

        if (this.state.editState == "edit" && !this.props.formState.processing)
            this.textArea.focus();
        else
            this.textArea.blur();
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
                disabled={this.props.formState.processing}
                className="form-control"
                ref={ref => this.textArea = ref!}
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
            <Panel header={this.renderPanelHeading()} bsStyle={this.getPanelClass()} footer={this.renderPanelFooter()}>
                {this.renderPanelBodyContent()}
                {this.renderEditArea()}
            </Panel>
        );
    }
}
