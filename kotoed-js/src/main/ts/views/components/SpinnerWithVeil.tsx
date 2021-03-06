import * as React from "react"
import * as Spinner from "react-spinkit"

import "less/util.less"
import "react-spinkit/css/base.css"
import "react-spinkit/css/loaders-css.css"


export default class SpinnerWithVeil extends React.Component {
    render() {
        return <div className="loading-veil">
            <Spinner name="pacman" color="white" fadeIn="none"/>
        </div>
    }
}

export class SpinnerWithBigVeil extends React.Component {
    render() {
        return <div style={{
            position: "relative",
            width: "100%",
            height: "500px"
        }}>
            <SpinnerWithVeil/>
        </div>;
    }
}