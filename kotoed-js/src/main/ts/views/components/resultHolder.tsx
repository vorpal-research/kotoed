import {Component} from "react";
import {components} from "griddle-react";
import {isArray} from "util";
import RowDefinition = components.RowDefinition;

export interface ResultHolderProps<ResultT> {
    name: string
    selector: (r: ResultT) => boolean
}

export interface ResultHolderState {
    rowDefinition: RowDefinition
}

export class ResultHolder<ResultT> extends Component<ResultHolderProps<ResultT>, ResultHolderState> {
    constructor(props: ResultHolderProps<ResultT>, context: undefined) {
        super(props, context);

        if (isArray(this.props.children)) {
            this.state = {rowDefinition: this.props.children[0] as any};
        } else {
            this.state = {rowDefinition: this.props.children as any};
        }
    }
}
