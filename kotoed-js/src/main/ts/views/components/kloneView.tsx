import * as React from "react";
import {Component} from "react";
import {Clearfix, Col, Grid, Panel, Row} from "react-bootstrap";
import * as _ from "lodash";

import {fetchFile} from "../../code/remote/code";
import {
    editorModeParam,
    guessCmModeForFile,
    requireCmMode
} from "../../code/util/codemirror";

import "less/kotoed-bootstrap/bootstrap.less";

import "codemirror/lib/codemirror.css";
import "codemirror/addon/merge/merge.css";

import * as cm from "codemirror";
import "codemirror/mode/meta";

import "diff-match-patch";
import "codemirror/addon/merge/merge.js";
import "codemirror/addon/fold/foldcode.js";
import "codemirror/addon/fold/foldgutter.js";
import "codemirror/addon/fold/brace-fold.js";
import "codemirror/addon/fold/indent-fold.js";
import "codemirror/addon/fold/comment-fold.js";

export interface FileInfo {
    path: string
}

export interface KloneInfo {
    submissionId: number,
    denizen: string,
    project: string,
    file: FileInfo,
    fromLine: number,
    toLine: number
}

export function formatKloneInfoAsHeader(klone: KloneInfo) {
    return `${klone.denizen}:${klone.project}:${klone.submissionId} @ ${klone.file.path}:${klone.fromLine}:${klone.toLine}`
}

export interface KloneViewProps {
    leftKlone: KloneInfo
    rightKlone: KloneInfo
}

export interface KloneViewState {
    open: boolean
    leftCode: string
    rightCode: string
}

function foldAllCode(editor: cm.Editor) {
    editor.operation(() => {
        for (let l = editor.getDoc().firstLine(); l <= editor.getDoc().lastLine(); ++l) {
            (editor as any).foldCode(cm.Pos(l, 0), null, "fold");
        }
    });
}

export class KloneView extends Component<KloneViewProps, KloneViewState> {
    constructor(props: KloneViewProps, context: undefined) {
        super(props, context);

        this.state = {
            open: false,
            leftCode: KloneView.NO_CODE,
            rightCode: KloneView.NO_CODE
        };
    }

    static NO_CODE = "No code available...";
    static LOADING_CODE = "Loading...";

    mergeViewerElement: HTMLDivElement | null = null;
    mergeViewer: cm.MergeView.MergeViewEditor | null = null;

    mergeViewerHeight = () => {
        if (null != this.mergeViewer) {
            return Math.max(
                this.mergeViewer.editor().getScrollInfo().height,
                this.mergeViewer.rightOriginal().getScrollInfo().height,
            );
        } else {
            return null
        }
    };

    mergeViewerResize = () => {
        if (null != this.mergeViewer) {
            foldAllCode(this.mergeViewer.editor());
            foldAllCode(this.mergeViewer.rightOriginal());

            let height = this.mergeViewerHeight();
            this.mergeViewer.editor().setSize(null, height);
            this.mergeViewer.rightOriginal().setSize(null, height);

            // this.mergeViewer.getWrapperElement().style.height = `${height}px`
        }
    };

    componentDidUpdate(prevProps: KloneViewProps, prevState: KloneViewState) {
        if (null != this.mergeViewer) {
            if (prevState.leftCode != this.state.leftCode) {
                this.mergeViewer.editor().setValue(this.state.leftCode);
                setTimeout(() => this.mergeViewer!!.editor().refresh());
            }
            if (prevState.rightCode != this.state.rightCode) {
                this.mergeViewer.rightOriginal().setValue(this.state.rightCode);
                setTimeout(() => this.mergeViewer!!.rightOriginal().refresh());
            }
            if (!prevState.open && this.state.open) {
                setTimeout(() => this.mergeViewer!!.editor().refresh());
                setTimeout(() => this.mergeViewer!!.rightOriginal().refresh());
            }
            this.mergeViewerResize()
        }
    }

    componentDidMount() {
        if (null != this.mergeViewerElement) {
            let mode = guessCmModeForFile(this.props.leftKlone.file.path);

            requireCmMode(mode);

            let mergeViewer = cm.MergeView(this.mergeViewerElement, {
                value: this.state.leftCode,
                orig: this.state.leftCode,
                origRight: this.state.rightCode,
                mode: editorModeParam(mode),
                readOnly: true
            });

            mergeViewer.editor().setOption("mode", editorModeParam(mode));
            mergeViewer.rightOriginal().setOption("mode", editorModeParam(mode));

            this.mergeViewer = mergeViewer;
        }
    }

    componentWillUnmount() {
        if (null != this.mergeViewer) {
            // this.mergeViewer.getWrapperElement()
        }
    }

    fetchCode = _.once(() => {
        if (KloneView.NO_CODE === this.state.leftCode) {
            this.setState({leftCode: KloneView.LOADING_CODE});

            fetchFile(
                this.props.leftKlone.submissionId,
                this.props.leftKlone.file.path,
                this.props.leftKlone.fromLine,
                this.props.leftKlone.toLine
            ).then((leftCode) =>
                this.setState({leftCode: leftCode})
            );
        }

        if (KloneView.NO_CODE === this.state.rightCode) {
            this.setState({rightCode: KloneView.LOADING_CODE});

            fetchFile(
                this.props.rightKlone.submissionId,
                this.props.rightKlone.file.path,
                this.props.rightKlone.fromLine,
                this.props.rightKlone.toLine
            ).then((rightCode) =>
                this.setState({rightCode: rightCode})
            );
        }
    });

    toggleOpen = () => {
        this.fetchCode();

        this.setState(
            (prevState: KloneViewState) => {
                return {open: !prevState.open};
            }
        );
    };

    render() {
        return (
            <div>
                <Grid fluid>
                    <Row className="align-items-center"
                         onClick={this.toggleOpen}>
                        <Col xs={6} style={{wordWrap: "break-word"}}>
                            {formatKloneInfoAsHeader(this.props.leftKlone)}
                        </Col>
                        <Col xs={6}>
                            {formatKloneInfoAsHeader(this.props.rightKlone)}
                        </Col>
                        <Clearfix/>
                    </Row>
                    <Row>
                        <Panel collapsible expanded={this.state.open}>
                            <div ref={(me) => this.mergeViewerElement = me}/>
                        </Panel>
                    </Row>
                </Grid>
            </div>
        );
    }

}
