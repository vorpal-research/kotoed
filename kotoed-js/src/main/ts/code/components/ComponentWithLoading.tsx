import * as React from "react"
import SpinnerWithVeil from "./SpinnerWithVeil";

export interface LoadingProperty {
    loading: boolean
}

export default abstract class ComponentWithLoading<Props extends LoadingProperty, State> extends
    React.Component<Props, State> {

    renderVeil = () => {
        if (this.props.loading)
            return <SpinnerWithVeil/>;
        else
            return null
    }
}