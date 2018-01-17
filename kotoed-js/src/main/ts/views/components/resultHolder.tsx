import {Component} from "react";
import {components} from "griddle-react";

export interface ResultFilter<ResultT> {
    name: string
    predicate: (r: ResultT) => boolean
    isOnByDefault: boolean
}

export interface ResultHolderProps<ResultT> {
    name: string
    rowDefinition: components.RowDefinition
    selector: (r: ResultT) => boolean
    transformer: (r: ResultT) => ResultT[]
    merger: (rs: ResultT[]) => ResultT[]
    filters: ResultFilter<ResultT>[]
    isVisible: (state: any) => boolean
}

export class ResultHolder<ResultT> extends Component<ResultHolderProps<ResultT>, any> {
    constructor(props: ResultHolderProps<ResultT>, context: undefined) {
        super(props, context);
    }
}
