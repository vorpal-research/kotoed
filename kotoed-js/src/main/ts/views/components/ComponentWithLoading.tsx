import * as React from "react"
import SpinnerWithVeil from "../../views/components/SpinnerWithVeil";
export interface LoadingProperty {
    loading: boolean
}

export default abstract class ComponentWithLoading<Props, State> extends
    React.Component<Props & LoadingProperty, State> {

    renderVeil = () => {
        if (this.props.loading)
            return <SpinnerWithVeil/>;
        else
            return null
    }
}