import * as React from "react";

interface TextEditorProps {
    textArea: HTMLTextAreaElement,
    text: string,
    onNewText: (text: string, afterTextUpdated: () => void) => void
}




export class TextEditor extends React.Component<TextEditorProps> {
    constructor(props: TextEditorProps) {
        super(props)
    }

    onBold = () => {
        this.props.onNewText(
            this.props.text.substring(0, this.props.textArea.selectionStart) +
            "**" +
            this.props.text.substring(this.props.textArea.selectionStart, this.props.textArea.selectionEnd) +
            "**" +
            this.props.text.substring(this.props.textArea.selectionEnd),
            () => {
                this.props.textArea.focus();
                this.props.textArea.setSelectionRange(this.props.textArea.selectionStart + 2, this.props.textArea.selectionEnd + 2);
            }
        );


    };

    render() {
        return <div onClick={this.onBold}>BOLD</div>
    }

}