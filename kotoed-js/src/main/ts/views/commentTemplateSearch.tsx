import * as React from "react";
import {SearchCallback, SearchTable} from "./components/search";
import CommentComponent from "../code/components/CommentComponent";
import {doNothing} from "../util/common";
import {Kotoed} from "../util/kotoed-api";
import {render} from "react-dom";
import {CommentTemplates} from "../code/remote/templates";
import {sendAsync, setStateAsync} from "./components/common";
import {Button, Modal} from "react-bootstrap";
import {CommentButton} from "../code/components/CommentButton";

import "less/comments.less"

type CommentTemplate = CommentTemplates[number]

type UpdateCallback = {
    callback: () => void
}

type ModifySearchCallback = {
    callback: SearchCallback
}

type CommentTemplateState = CommentTemplate

class CommentTemplateEditComponent extends React.Component<CommentTemplateState & ModifySearchCallback, CommentTemplateState> {
    constructor(props: CommentTemplateState & ModifySearchCallback) {
        super(props);
        let temp = { ...props } as CommentTemplateState;
        this.state = temp;
    }

    componentWillReceiveProps(nextProps: CommentTemplateState) {
        this.setState({ ...nextProps })
    }

    onEdit = async (id: number, text: string) => {
        await setStateAsync(this,{ text: text });
        await sendAsync(Kotoed.Address.Api.CommentTemplate.Update,
            {id: this.props.id, text: this.state.text, name: this.state.name})
    };

    onDelete = async () => {
        await sendAsync(Kotoed.Address.Api.CommentTemplate.Delete,
            {id: this.props.id});
        this.props.callback().toggleSearch()
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
                    commentTemplates={[]}
                    customHeaderComponent={ newState =>
                        newState.editState === "edit" ?
                            <input value={this.state.name}
                                   className="form-control"
                                   onChange={e => this.onNameChange(e.target.value)}
                                   readOnly={newState.editState !== 'edit'}
                            /> :
                            <span><strong>{this.state.name}</strong></span>
                    }
                    customHeaderButton={ newState =>
                        <CommentButton title={"Remove"} icon={"remove"} onClick={this.onDelete} />
                    }
            />
        )
    }

}

type CommentTemplateTableState = {
    creating: boolean
}

class CommentTemplateCreator
    extends React.Component<CommentTemplateTableState & UpdateCallback,
                            CommentTemplateTableState & CommentTemplate> {

    constructor(props: CommentTemplateTableState & UpdateCallback) {
        super(props);
        let temp = {...props, name: "", text: "", id: -1 };
        this.state = temp
    }

    onNameChange = (text: string) => this.setState({name: text});

    onEdit = async (id: number, text: string) => {
        await setStateAsync(this,{ text: text, creating: false });
        let newState = await sendAsync(Kotoed.Address.Api.CommentTemplate.Create,
            { text: this.state.text, name: this.state.name });
        await setStateAsync(this, { text: "", name: "", creating: false });
        this.props.callback();
    };

    onCancel = () => this.setState({text: "", name: "", creating: false});

    render() {
        return <div>
            <Button bsStyle="success"
                    onClick={() => this.setState({creating: true})}
            >Create new template</Button>

            <Modal show={this.state.creating} onHide={this.onCancel}>
                <Modal.Header>
                    <Modal.Title>Create new template</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <CommentComponent
                        id={-1}
                        text={this.state.text}
                        canStateBeChanged={false}
                        canBeEdited={true}
                        collapsed={false}
                        onUnresolve={doNothing}
                        onResolve={doNothing}
                        notifyEditorAboutChange={doNothing}
                        onEdit={this.onEdit}
                        onCancelEdit={this.onCancel}
                        processing={false}
                        defaultEditState={'edit'}
                        commentTemplates={[]}
                        customHeaderComponent={ newState =>
                            newState.editState === "edit" ?
                                <input value={this.state.name}
                                       className="form-control"
                                       onChange={e => this.onNameChange(e.target.value)}
                                       readOnly={newState.editState !== 'edit'}
                                /> :
                                <span><strong>{this.state.name}</strong></span>
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
                toolbarComponent={(toggleSearch: () => void) =>
                    <CommentTemplateCreator {...this.state} callback={toggleSearch} />
                }
                elementComponent={(key, c: CommentTemplate, toggleSearch: SearchCallback) =>
                    <CommentTemplateEditComponent key={key} {...c} callback={toggleSearch} />}
            />
        );
    }
}

render(
    <CommentTemplateTable/>,
    document.getElementById('template-app')
);
