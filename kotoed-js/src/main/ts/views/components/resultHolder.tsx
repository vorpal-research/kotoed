import {Component, ReactNode} from "react";
import {components} from "griddle-react";
import {isArray} from "util";
import RowDefinition = components.RowDefinition;

export interface ResultHolderProps<ResultT> {
    name: string
    selector: (r: ResultT) => boolean
    transformer: (r: ResultT) => ResultT[]
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

    componentWillReceiveProps(nextProps: ResultHolderProps<ResultT> & { children?: ReactNode }) {
        if (nextProps.children != this.props.children) {
            if (isArray(nextProps.children)) {
                this.setState({rowDefinition: nextProps.children[0] as any});
            } else {
                this.setState({rowDefinition: nextProps.children as any});
            }
        }
    }
}
