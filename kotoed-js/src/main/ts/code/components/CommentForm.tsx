import * as React from "react";
import {CommentButton} from "./CommentButton";
import ReactMarkdown = require("react-markdown");
import CmrmCodeBlock from "./CmrmCodeBlock";
import {Button, Panel, Label, SplitButton, MenuItem, ButtonToolbar} from "react-bootstrap";
import {FormState} from "../state/forms";

import Mousetrap, {MousetrapInstance} from "../../util/mousetrap"
import {CommentTemplates} from "../remote/templates";
import {SimpleAutoSuggest} from "../../views/components/tags/SimpleAutosuggest";
import "less/autosuggest.less"
import {TextEditor} from "../../views/components/TextEditor";
import {setStateAsync} from "../../views/components/common";
import {twemojifyNode} from "../../views/components/emoji";

interface CommentFormProps {
    onSubmit: (text: string) => void
    onCancel: () => void
    notifyEditorAboutChange: () => void
    whoAmI: string
    commentTemplates: CommentTemplates
    formState: FormState
}

interface CommentFormState {
    editText: string
    editState: "edit" | "preview"
}

export default class CommentForm extends React.Component<CommentFormProps, CommentFormState> {
    private editor: TextEditor;

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
                    this.editor.focus();
                    return {
                        ...prev,
                        editState: "edit"
                    };
                case "edit":
                    this.editor.focus();
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
            renderers={{
                CodeBlock: CmrmCodeBlock,
                Text: twemojifyNode
            }}
            escapeHtml={true}
        />;
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
        let button = window.innerWidth < 992 ? <Button bsStyle="success"
                                                       onTouchStart={() => this.props.onSubmit(this.state.editText)}
                                                       disabled={this.props.formState.processing}>
            Send
        </Button> : <Button bsStyle="success"
                            onClick={() => this.props.onSubmit(this.state.editText)}
                            disabled={this.props.formState.processing}>
            Send
        </Button>;

        return <ButtonToolbar> {button} </ButtonToolbar>
    };

    private mousetrap: MousetrapInstance | undefined;

    componentWillUnmount() {
    }

    componentDidMount() {
        if (!this.props.formState.processing)
            this.editor.focus();
    }

    componentDidUpdate(prevProps: CommentFormProps, prevState: CommentFormState) {
        if (this.state.editState != prevState.editState)
            this.props.notifyEditorAboutChange();
        if (this.state.editState !== prevState.editState) {
            if (this.state.editState == "edit" && !this.props.formState.processing)
                this.editor.focus();
            else
                this.editor.blur();
        }
    }

    getTextAreaStyle = () => {
        switch (this.state.editState) {
            case "preview":
                return {
                    display: "none"
                };
            case "edit":
                return {};
        }
    };

    renderEditArea = (): JSX.Element => {
        return <div style={this.getTextAreaStyle()}>
            <TextEditor
                ref={(ref) => this.editor = ref!}
                text={this.state.editText}
                onChange={(text) => setStateAsync(this, {editText: text})}
                panelDisabled={this.state.editState !== "edit"}
                disabled={this.props.formState.processing}
                onEscape={this.props.onCancel}
                onCtrlEnter={() => this.props.onSubmit(this.state.editText)}
                commentTemplates={this.props.commentTemplates}
            />
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
