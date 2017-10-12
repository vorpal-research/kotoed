import * as React from "react";
import {SearchTable} from "./components/search";
import CommentComponent from "../code/components/CommentComponent";
import {doNothing} from "../util/common";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {CommentTemplates} from "../code/remote/templates";
import {sendAsync, setStateAsync} from "./components/common";
import {Button, Modal} from "react-bootstrap";

type CommentTemplate = CommentTemplates[number]

type CommentTemplateState = CommentTemplate

class CommentTemplateEditComponent extends React.Component<CommentTemplateState, CommentTemplateState> {
    constructor(props: CommentTemplateState) {
        super(props);
        this.state = { ...props };
    }

    componentWillReceiveProps(nextProps: CommentTemplateState) {
        this.setState({ ...nextProps })
    }

    onEdit = async (id: number, text: string) => {
        await setStateAsync(this,{ text: text });
        await sendAsync(Kotoed.Address.Api.CommentTemplate.Update,
            {id: this.props.id, text: this.state.text, name: this.state.name})
    };

    onNameChange = (text: string) => this.setState({name: text});

    render() {
        let template = this.state;
        return (
            <CommentComponent
                    id={-1}
                    text={template.text}
                    canStateBeChanged={false}
                    canBeEdited={true}
                    collapsed={false}
                    onUnresolve={doNothing}
                    onResolve={doNothing}
                    notifyEditorAboutChange={doNothing}
                    onEdit={this.onEdit}
                    processing={false}
                    customHeaderComponent={ newState =>
                        <input value={this.state.name}
                               onChange={e => this.onNameChange(e.target.value)}
                               readOnly={newState.editState !== 'edit'}
                        />
                    }
            />
        )
    }

}

type CommentTemplateTableState = {
    creating: boolean
}

class CommentTemplateCreator extends React.Component<CommentTemplateTableState, CommentTemplateTableState & CommentTemplate> {

    constructor(props: CommentTemplateTableState) {
        super(props);
        this.state = {...props, name: "", text: "", id: -1 }
    }

    onNameChange = (text: string) => this.setState({name: text});

    onEdit = async (id: number, text: string) => {
        await setStateAsync(this,{ text: text });
        let newState = await sendAsync(Kotoed.Address.Api.CommentTemplate.Create,
            {id: this.state.id, text: this.state.text, name: this.state.name});
        await setStateAsync(this, newState);
    };

    render() {
        return <div>
            <Button bsStyle="success"
                    onClick={() => this.setState({creating: true})}
            >Create project</Button>

            <Modal show={this.state.creating} onHide={() => this.setState({creating: false})}>
                <Modal.Header>
                    <Modal.Title>Create new project</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <CommentComponent
                        id={-1}
                        text={""}
                        canStateBeChanged={false}
                        canBeEdited={true}
                        collapsed={false}
                        onUnresolve={doNothing}
                        onResolve={doNothing}
                        notifyEditorAboutChange={doNothing}
                        onEdit={this.onEdit}
                        processing={false}
                        customHeaderComponent={ newState =>
                            <input value={this.state.name}
                                   onChange={e => this.onNameChange(e.target.value)}
                                   readOnly={newState.editState !== 'edit'}
                            />
                        }
                    />
                </Modal.Body>
            </Modal>
        </div>
    }
}

class CommentTemplateTable extends React.Component<{}, CommentTemplateTableState> {

    constructor(props: {}) {
        super(props);
        this.state = {
            creating: false
        }
    }

    render() {
        return (
            <SearchTable
                shouldPerformInitialSearch={() => true}
                searchAddress={Kotoed.Address.Api.CommentTemplate.Search}
                countAddress={Kotoed.Address.Api.CommentTemplate.SearchCount}
                toolbarComponent={() =>
                    <CommentTemplateCreator {...this.state} />
                }
                elementComponent={(key, c: CommentTemplate) =>
                    <CommentTemplateEditComponent key={key} {...c} />}
            />
        );
    }
}

render(
    <CommentTemplateTable/>,
    document.getElementById('template-app')
);
