import * as React from "react";
import {Component} from "react";
import {Button, Clearfix, Col, Grid, Panel, Row} from "react-bootstrap";

import {fetchFile} from "../../code/remote/code";
import {editorModeParam, guessCmModeForFile} from "../../code/util/codemirror";

import "less/kotoed-bootstrap/bootstrap.less";

import "codemirror/lib/codemirror.css";
import "codemirror/addon/merge/merge.css";

import * as cm from "codemirror";
import "codemirror/mode/meta";
import "codemirror/mode/clike/clike.js" // FIXME: akhin Why is this needed???

import "diff-match-patch";
import "codemirror/addon/merge/merge.js";

export interface FileInfo {
    path: string
}

export interface KloneInfo {
    submissionId: number,
    file: FileInfo,
    fromLine: number,
    toLine: number
}

export interface KloneViewProps {
    leftKlone: KloneInfo
    rightKlone: KloneInfo
}

export interface KloneViewState {
    mergeViewer: cm.MergeView.MergeViewEditor | null
    open: boolean
    leftCode: string
    rightCode: string
}

export class KloneView extends Component<KloneViewProps, KloneViewState> {
    constructor(props: KloneViewProps, context: undefined) {
        super(props, context);

        this.state = {
            mergeViewer: null,
            open: false,
            leftCode: "Loading...",
            rightCode: "Loading..."
        };
    }

    mergeViewerElement: HTMLDivElement | null = null;

    componentDidUpdate(prevProps: KloneViewProps, prevState: KloneViewState) {
        if (null != this.state.mergeViewer) {
            if (prevState.leftCode != this.state.leftCode) {
                this.state.mergeViewer.editor().setValue(this.state.leftCode);
                setTimeout(() => this.state.mergeViewer!!.editor().refresh());
            }
            if (prevState.rightCode != this.state.rightCode) {
                this.state.mergeViewer.rightOriginal().setValue(this.state.rightCode);
                setTimeout(() => this.state.mergeViewer!!.rightOriginal().refresh());
            }
            if (!prevState.open && this.state.open) {
                setTimeout(() => this.state.mergeViewer!!.editor().refresh());
                setTimeout(() => this.state.mergeViewer!!.rightOriginal().refresh());
            }
        }
    }

    componentDidMount() {
        if (null != this.mergeViewerElement) {
            let mode = editorModeParam(guessCmModeForFile(this.props.leftKlone.file.path))

            let mergeViewer = cm.MergeView(this.mergeViewerElement, {
                value: this.state.leftCode,
                orig: this.state.leftCode,
                origRight: this.state.rightCode,
                mode: mode
            });

            mergeViewer.editor().setOption("mode", mode);
            mergeViewer.rightOriginal().setOption("mode", mode);

            this.setState({
                mergeViewer: mergeViewer
            });
        }
    }

    componentWillUnmount() {
        if (null != this.state.mergeViewer) {
            // this.state.mergeViewer.getWrapperElement()
        }
    }

    componentWillMount() {
        fetchFile(
            this.props.leftKlone.submissionId,
            this.props.leftKlone.file.path,
            this.props.leftKlone.fromLine,
            this.props.leftKlone.toLine
        ).then((leftCode) =>
            this.setState({leftCode: leftCode})
        );

        fetchFile(
            this.props.rightKlone.submissionId,
            this.props.rightKlone.file.path,
            this.props.rightKlone.fromLine,
            this.props.rightKlone.toLine
        ).then((rightCode) =>
            this.setState({rightCode: rightCode})
        );
    }

    toggleOpen = () => {
        this.setState(
            (prevState: KloneViewState) => {
                return {open: !prevState.open};
            }
        );
    };

    render() {

        let formatHeader = (klone: KloneInfo) => {
            return `Submission ${klone.submissionId} @ ${klone.file.path}:${klone.fromLine}:${klone.toLine}`
        };

        return (
            <div>
                <Grid fluid>
                    <Button block onClick={this.toggleOpen}>
                        <Row className="align-items-center">
                            <Col xs={6}>
                                <samp>{formatHeader(this.props.leftKlone)}</samp>
                            </Col>
                            <Col xs={6}>
                                <samp>{formatHeader(this.props.rightKlone)}</samp>
                            </Col>
                            <Clearfix/>
                        </Row>
                    </Button>
                    <Panel collapsible expanded={this.state.open}>
                        <div ref={(me) => this.mergeViewerElement = me}/>
                    </Panel>
                </Grid>
            </div>
        );
    }

}
