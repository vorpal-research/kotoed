import * as React from "react";
import {Alert, Button, Form, FormGroup, ControlLabel, FormControl, Modal} from "react-bootstrap";
import {Kotoed} from "../util/kotoed-api";
import {eventBus, SoftError} from "../eventBus";
import {ComponentWithLocalErrors} from "../views/components/ComponentWithLocalErrors";
import {ErrorMessages} from "../login/util";
import {ChangeEvent, FormEvent, KeyboardEvent} from "react";
import Glyphicon = require("react-bootstrap/lib/Glyphicon");
import ComponentWithLoading from "../views/components/ComponentWithLoading";
import SpinnerWithVeil from "../views/components/SpinnerWithVeil";
import {render} from "react-dom";
import * as Immutable from "immutable"
import Row = require("react-bootstrap/lib/Row");
import {log} from "util";
import Panel = require("react-bootstrap/lib/Panel");
import {Redirect} from "react-router";
import UrlPattern = Kotoed.UrlPattern;
import {WithId} from "../data/common";

let uniqueId: number = 100000;
function nextUniqueId() {
    return ++uniqueId;
}

type CommandType = 'SHELL'

interface CommandTemplate {
    type: CommandType
    commandLine: string
    uniqueKey?: number
}
type EnVar = {
    name: string,
    value: string,
    uniqueKey?: number
}
interface BuildTemplate {
    id: number
    environment : Immutable.List<EnVar>
    commandLine: Immutable.List<CommandTemplate>
}

function toRemote(bt: BuildTemplate): object {
    return {
        id: bt.id,
        environment : bt.environment.toArray(),
        commandLine : bt.commandLine.map( command => ({
                type: command!.type,
                commandLine: qSplit(command!.commandLine)
                    .map(it => it.trim())
                    .filter(it => it !== "")
        })).toArray()
    }
}

function fromRemote(bt: any): BuildTemplate {
    return {
        id: bt.id,
        environment : Immutable.List(bt.environment as EnVar[]),
        commandLine : bt.commandLine.map( (command: any) => ({
            type: command.type,
            commandLine: (command.commandLine as string[]).join(" ")
        }))
    }
}

function qSplit(s: string): string[] {
    let ss = s.trim().split(' ');
    let res = [];
    for(let i = 0; i < ss.length; ++i) {
        let cur = ss[i];
        if(cur.startsWith("\"")) {
            cur = "";
            do { cur = cur + ss[i]; ++i } while(i < ss.length && !ss[i].endsWith("\""))
        }
        res.push(cur)
    }
    return res
}

function assignKeys(template: BuildTemplate): BuildTemplate {
    return {
        id: template.id,
        environment :
            Immutable.List(template
                .environment
                .map(v => Object.assign(v!, { uniqueKey: nextUniqueId()} ))),
        commandLine :
            Immutable.List(template
                .commandLine
                .map(v => Object.assign(v!, { uniqueKey: nextUniqueId()} )))
    }
}

type LocalErrors = {
    emptySomething: false
}

interface EditProps<State> {
    initialState: State
    onEdit: (value: Readonly<State>, oldValue: Readonly<State>) => void
}

class EnvironmentVariableEdit extends React.Component<
        EditProps<EnVar> & { onRemove: () => void}, EnVar> {
    constructor(props: EditProps<EnVar> & { onRemove: () => void}) {
        super(props);
        this.state = props.initialState
    }

    onVarChange = (value: string) => {
        const oldState = this.state;
        this.setState({ name: value }, () => this.props.onEdit(this.state, oldState))
    };

    onValueChange = (value: string) => {
        const oldState = this.state;
        this.setState({ value: value }, () => this.props.onEdit(this.state, oldState))
    };

    onRemove = () => this.props.onRemove();

    doRender = () => {
        return <FormGroup>
            <div className="col-sm-3">
                <input className="form-control" value={this.state.name} onChange={ e => this.onVarChange(e.target.value) }/>
            </div>
            <div className="col-sm-8">
                <input className="form-control" value={this.state.value} onChange={ e => this.onValueChange(e.target.value) }/>
            </div>
            <div className="col-sm-1">
                <a className="btn btn-danger" onClick={this.onRemove}><Glyphicon glyph="minus"/></a>
            </div>
        </FormGroup>
    };

    render() { return this.doRender() }
}

type Wrapped<T> = { unwrapped : T }
function wrap<T>(value: T): Wrapped<T> { return { unwrapped: value } };

type Environment = Wrapped<Immutable.List<EnVar>>

class EnvironmentVariableMultiEdit extends React.Component<EditProps<Environment>, Environment> {
    constructor(props: EditProps<Environment>) {
        super(props);
        this.state = props.initialState
    }

    onElementEdit = (ix: number, value: EnVar) => {
        const oldState = this.state;
        const newState = wrap(this.state.unwrapped.set(ix, value));
        this.setState(newState, () => this.props.onEdit(this.state, oldState))
    };

    onElementRemove = (ix: number) => {
        const oldState = this.state;
        const newState = wrap(this.state.unwrapped.remove(ix));
        this.setState(newState, () => this.props.onEdit(this.state, oldState))
    };

    onElementAdd = () => {
        const oldState = this.state;
        const newState = wrap(this.state.unwrapped.push({name: "VARIABLE", value: "VALUE", uniqueKey: nextUniqueId()}));
        this.setState(newState, () => this.props.onEdit(this.state, oldState))
    };

    doRender = () => {
        return <form className="form-horizontal"> {
            this.state.unwrapped.map((kv, ix) =>
                <EnvironmentVariableEdit
                    initialState={kv!}
                    onEdit={(value) => this.onElementEdit(ix!, value)}
                    onRemove={ () => this.onElementRemove(ix!) }
                    key={"envars-" + kv!.uniqueKey}
                />
            )
        }
            <FormGroup>
                <div className="col-sm-12">
                    <a className="btn btn-success" onClick={this.onElementAdd}>
                        <strong>New variable</strong> <Glyphicon glyph="plus"/>
                    </a>
                </div>
            </FormGroup>
        </form>
    };

    render() { return this.doRender() }
}

type CommandLineMultiEditState = Wrapped<Immutable.List<CommandTemplate>>

class CommandLineMultiEdit extends React.Component<
        EditProps<CommandLineMultiEditState>, CommandLineMultiEditState
    > {
    constructor(props: EditProps<CommandLineMultiEditState>) {
        super(props);
        this.state = props.initialState
    }

    onElementEdit = (ix: number, value: string) => {
        const oldState = this.state;
        const newState = this.state.unwrapped.set(ix,
            {
                type: "SHELL",
                commandLine: value,
                uniqueKey: oldState.unwrapped.get(ix).uniqueKey
            });
        this.setState(wrap(newState), () => this.props.onEdit(this.state, oldState))
    };

    onElementRemove = (ix: number) => {
        const oldState = this.state;
        const newState = this.state.unwrapped.remove(ix);
        this.setState(wrap(newState), () => this.props.onEdit(this.state, oldState))
    };

    doRender = () =>
        <form className="form-horizontal"> {
            this.state.unwrapped.map((kv, ix) =>
                <FormGroup key={"commands-" + kv!.uniqueKey}>
                    <div className="col-sm-11">
                        <input
                            className="form-control"
                            value={kv!.commandLine}
                            onChange={ e => this.onElementEdit(ix!, e.target.value) }
                        />
                    </div>
                    <div className="col-sm-1">
                        <a className="btn btn-danger" onClick={() => this.onElementRemove(ix!)}>
                            <Glyphicon glyph="minus"/>
                        </a>
                    </div>
                </FormGroup>
            )
        }
            <FormGroup>
                <div className="col-sm-12">
                    <a className="btn btn-success" onClick={ () => {
                        const oldState = this.state;
                        const newState = this.state.unwrapped.push({
                            type: "SHELL",
                            commandLine: "echo Hello",
                            uniqueKey: nextUniqueId()
                        });
                        this.setState(wrap(newState), () => this.props.onEdit(this.state, oldState))
                    }}><strong>New command</strong> <Glyphicon glyph="plus"/></a>
                </div>
            </FormGroup>
        </form>;

    render() { return this.doRender() }
}

export class BuildTemplateEditor extends ComponentWithLocalErrors<BuildTemplate, BuildTemplate, LocalErrors> {

    localErrorMessages: ErrorMessages<LocalErrors> = {
        emptySomething: "Something is suspiciously empty"
    };

    constructor(props: BuildTemplate) {
        super(props);
        this.state = {
            ...props,
            localErrors: {
                emptySomething: false
            }
        }
    }

    trySubmit = async () => {
        try {
            await eventBus.send(Kotoed.Address.Api.BuildTemplate.Update, toRemote(this.state));
        } catch (error) {
            throw error;
        }
    };

    tryCopy = async () => {
        try {
            const newBT = await eventBus.send(Kotoed.Address.Api.BuildTemplate.Create, toRemote(this.state));
            window.location.href = UrlPattern.reverse(UrlPattern.BuildTemplate.Edit, newBT as {id: number})
        } catch (error) {
            throw error;
        }
    };

    handleEnter = (event: KeyboardEvent<FormControl>) => event.key === "Enter" && this.trySubmit();

    onEditVars = (vars: Environment) => this.setState({ environment: vars.unwrapped });
    onEditCommands = (commands: CommandLineMultiEditState) => this.setState({ commandLine: commands.unwrapped });

    doRender = () => <div className="row">
        <div className="panel">
            <div className="panel-heading">
                <Row>
                    <div className="col-sm-12">
                        <h3>Environment variables</h3>
                    </div>
                </Row>
            </div>
            <div className="panel-body">
                <EnvironmentVariableMultiEdit
                    initialState={wrap(this.state.environment)}
                    onEdit={this.onEditVars}
                />
            </div>
        </div>
        <div className="panel">
            <div className="panel-heading">
                <Row>
                    <div className="col-sm-12">
                        <h3>Build script</h3>
                    </div>
                </Row>
            </div>
            <div className="panel-body">
                <CommandLineMultiEdit
                    initialState={wrap(this.state.commandLine)}
                    onEdit={this.onEditCommands}
                />
            </div>
        </div>
        <Panel>
            <div className="panel-body">
                <div className="form-horizontal">
                    <FormGroup>
                        <div className="col-sm-1"><a className="btn btn-primary btn-block" onClick={this.trySubmit}>
                            Save
                        </a></div>
                        <div className="col-sm-11"><a className="btn btn-default" onClick={this.tryCopy}>
                            Copy to a new template
                        </a></div>
                    </FormGroup>
                </div>
            </div>
        </Panel>
    </div>;

    render() {
        return this.doRender()
    }
}

type WrapperState = {
    loading: boolean
    template?: BuildTemplate
}

export class BuildTemplateEditorWrapper extends React.Component<WithId, WrapperState> {
    constructor(props: {id: number}) {
        super(props);
        this.state = { loading: true };
        this.tryLoad()
    }

    tryLoad = async () => {
        try {
            const remote =
                await eventBus.send(Kotoed.Address.Api.BuildTemplate.Read, this.props);
            const template = fromRemote(remote);
            this.setState({loading: false, template: assignKeys(template)})
        } catch (error) {
            throw error;
        }
    };

    render() {
        if(this.state.template) {
            return <BuildTemplateEditor {...this.state.template} />
        } else return <SpinnerWithVeil/>
    }
}

const params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.BuildTemplate.Edit, window.location.pathname) || new Map();
const templateId = parseInt(params.get("id")) || -1;

render(
    <BuildTemplateEditorWrapper id={templateId}/>,
    document.getElementById("build-template-app")
);
