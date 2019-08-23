import * as React from "react";
import {Kotoed} from "../util/kotoed-api";
import {eventBus, SoftError} from "../eventBus";
import {ChangeEvent, FormEvent, KeyboardEvent} from "react";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import {render} from "react-dom";
import Row = require("react-bootstrap/lib/Row");
import Panel = require("react-bootstrap/lib/Panel");
import {CourseToRead} from "../data/course";
import {fetchCourse} from "./remote";
import {WithId} from "../data/common";
import {DbRecordWrapper} from "../data/verification";
import UrlPattern = Kotoed.UrlPattern;

const params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Course.Edit, window.location.pathname) || new Map();
const courseId = parseInt(params.get("id")) || -1;

type WrapperState = { loading: true, failed: false }
                  | { loading: false, failed: true }
                  | { loading: false, failed: false, course: CourseToRead }

class CourseEditor extends React.Component<CourseToRead, CourseToRead> {
    constructor(props: CourseToRead) {
        super(props);
        this.state = props
    }
    bindInput = <K extends keyof CourseToRead>(key: K) => (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({ [key]: e.target.value } as Pick<CourseToRead, K>)
    };
    bindFileInput = <K extends keyof CourseToRead>(key: K) => (e: ChangeEvent<HTMLInputElement>) => {
        const reader = new FileReader();
        reader.addEventListener("load", () => {
            this.setState({ [key]: window.btoa(reader.result) } as Pick<CourseToRead, K>)
        }, false);
        if(!e.target.files) {
            this.setState({ [key]: undefined } as Pick<CourseToRead, K>);
            return
        }
        reader.readAsBinaryString(e.target.files[0])
    };

    mkInputFor = (label: string, field: keyof CourseToRead, type = "input",
                  accept: string | undefined = undefined, onChange = this.bindInput(field)) =>
        <div className="form-group">
            <label className="control-label col-sm-2"
                   htmlFor={`input-${field}`}>{label}</label>
            <div className="col-sm-10">
                <input
                    className={ type === "file"? `form-control-file` : `form-control` }
                    id={`input-${field}`}
                    type={type}
                    value={this.state[field] || ""}
                    placeholder="not specified"
                    onChange={onChange}
                    accept={accept}
                />
            </div>
        </div>;

    onSave = async () => {
        try {
            await eventBus.send(Kotoed.Address.Api.Course.Update, this.state);
        } catch (error) {
            throw error;
        }
    };

    mkBuildTemplateEditBtn = () => {
        const disabled = this.state.buildTemplateId != undefined ? "" : "disabled";
        const href = this.state.buildTemplateId != undefined ?
            UrlPattern.reverse(UrlPattern.BuildTemplate.Edit, {id: this.state.buildTemplateId}) : "#";
        return <a className={`btn btn-default ${disabled}`} href={href}>Edit build template</a>;
    };

    render() {
        return <Row>
            <Panel>
                <div className="panel-heading">
                    <Row>
                        <div className="col-sm-12">
                            <h3>{this.state.name}</h3>
                        </div>
                    </Row>
                </div>
                <div className="panel-body">
                    <form className="form-horizontal">
                        {this.mkInputFor("Base repo URL", "baseRepoUrl")}
                        {this.mkInputFor("Base revision", "baseRevision")}
                        {this.mkInputFor("Build template ID", "buildTemplateId", "number")}
                        {this.mkInputFor("Icon file (*.png)", "icon", "file",
                        "image/png", this.bindFileInput("icon"))}
                        <div className="form-group">
                            <div className="col-sm-offset-2 col-sm-1">
                                <a className="btn btn-primary btn-block" onClick={this.onSave}>Save</a>
                            </div>
                            <div className="col-sm-9">
                                {this.mkBuildTemplateEditBtn()}
                            </div>
                        </div>
                    </form>
                </div>
            </Panel>
        </Row>
    }
}

class CourseEditorWrapper extends React.Component<WithId, WrapperState> {
    constructor(props: WithId) {
        super(props);
        this.state = { loading: true, failed: false };
        this.tryLoad()
    }
    tryLoad = async () => {
        let remote: DbRecordWrapper<CourseToRead>;
        while (true) {
            remote = await fetchCourse(this.props.id);
            if(remote.verificationData.status == "Processed") {
                const newState = { loading: false, failed: false, course: remote.record };
                this.setState(newState);
                return;
            }
            if(remote.verificationData.status == "Invalid") {
                this.setState({ loading: false, failed: true });
                return;
            }
        }
    };

    render() {
        if(this.state.loading) return <SpinnerWithVeil/>;
        if(this.state.failed) return <h1>Cannot load course</h1>;
        return <CourseEditor {...this.state.course } />
    }
}

render(
    <CourseEditorWrapper id={courseId}/>,
    document.getElementById("course-edit-app")
);
