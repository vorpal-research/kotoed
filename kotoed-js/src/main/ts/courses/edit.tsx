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

type WrapperState = {
    loading: boolean
    failed: boolean
    course?: CourseToRead
}

class CourseEditor extends React.Component<CourseToRead, CourseToRead> {
    constructor(props: CourseToRead) {
        super(props);
        this.state = props
    }
    bindInput = <K extends keyof CourseToRead>(key: K) => (e: ChangeEvent<HTMLInputElement>) => {
        this.setState({ [key]: e.target.value } as Pick<CourseToRead, K>)
    };

    mkInputFor = (label: string, field: keyof CourseToRead, type = "input", onChange = this.bindInput(field)) =>
        <div className="form-group">
            <label className="control-label col-sm-2"
                   htmlFor={`input-${field}`}>{label}</label>
            <div className="col-sm-10">
                <input
                    className="form-control"
                    id={`input-${field}`}
                    type={type}
                    value={this.state[field] || ""}
                    placeholder="not specified"
                    onChange={onChange}
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
        this.state = {loading: true, failed: false};
        this.tryLoad()
    }
    tryLoad = async () => {
        let remote: DbRecordWrapper<CourseToRead>;
        while (true) {
            remote = await fetchCourse(this.props.id);
            if(remote.verificationData.status == "Processed") {
                this.setState({loading: false, course: remote.record});
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
        return <CourseEditor {...this.state.course!} />
    }
}

render(
    <CourseEditorWrapper id={courseId}/>,
    document.getElementById("course-edit-app")
);
