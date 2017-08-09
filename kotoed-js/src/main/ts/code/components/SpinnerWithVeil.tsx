import * as React from "react"
import {Spinner} from "@blueprintjs/core";

export default class SpinnerWithVeil extends React.Component {
    render() {
        return <div className="loading-veil"><Spinner/></div>
    }
}