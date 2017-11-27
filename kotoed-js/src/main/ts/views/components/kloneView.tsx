import * as React from "react";
import {Component} from "react";
import {Button, Clearfix, Col, Grid, Panel, Row} from "react-bootstrap";

import {fetchFile} from "../../code/remote/code";

import "less/kotoed-bootstrap/bootstrap.less";

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
    open: boolean
    leftCode: string
    rightCode: string
}

export class KloneView extends Component<KloneViewProps, KloneViewState> {
    constructor(props: KloneViewProps, context: undefined) {
        super(props, context);

        this.state = {
            open: false,
            leftCode: "Loading...",
            rightCode: "Loading..."
        };
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
                        <Row className="align-items-center">
                            <Col xs={6}>
                                <pre><code>{this.state.leftCode}</code></pre>
                            </Col>
                            <Col xs={6}>
                                <pre><code>{this.state.rightCode}</code></pre>
                            </Col>
                            <Clearfix/>
                        </Row>
                    </Panel>
                </Grid>
            </div>
        );
    }

}
