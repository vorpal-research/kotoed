import * as React from "react";
import * as Autosuggest from "react-autosuggest";
import "less/autosuggest.less"

interface SimpleAutoSuggestProps<T> {
    values: Array<T>
    onSelect: (selected: T) => void
    valueToString?: (v: T) => string
    renderSuggestion?: (v: T) => JSX.Element
    disabled?: boolean
}

interface SimpleAutosuggestState<T> {
    currentValues: Array<T>
    search: string
}

export class SimpleAutoSuggest<T> extends React.Component<SimpleAutoSuggestProps<T>, SimpleAutosuggestState<T>> {
    constructor(props: SimpleAutoSuggestProps<T>) {
        super(props);
        this.state = {
            currentValues: [],
            search: ""
        }
    }

    private isDisabled = () => {
        if (this.props.disabled === undefined)
            return false;

        return this.props.disabled
    };

    private filterValues = ({value: search}: {value: string}) => {
        this.setState({
            currentValues: this.props.values
                .filter(v => this.valueToString(v).toLowerCase().startsWith(search.toLowerCase()))
        })
    };

    private valueToString = (v: T) => {
        return this.props.valueToString ? this.props.valueToString(v) : v.toString()
    };

    private renderSuggestion = (v: T) => {
        return this.props.renderSuggestion ? this.props.renderSuggestion(v) : <span>{this.valueToString(v)}</span>
    };

    render() {
        return <Autosuggest
            suggestions={this.isDisabled() ? [] : this.state.currentValues}
            onSuggestionsFetchRequested={this.filterValues}
            onSuggestionsClearRequested={() => this.setState({currentValues: []})}
            getSuggestionValue={() => ""}
            renderSuggestion={this.renderSuggestion}
            focusInputOnSuggestionClick={false}
            highlightFirstSuggestion={true}
            inputProps={{
                value: this.state.search,
                onChange: (evt, params) => {this.setState({search: params!.newValue})},
                disabled: this.isDisabled()
            }}
            onSuggestionSelected={(ev, v) => {
                this.props.onSelect(v.suggestion);
            }}
            shouldRenderSuggestions={() => true}
            theme={{
                container:                'react-autosuggest__container',
                containerOpen:            'react-autosuggest__container--open',
                input:                    'react-autosuggest__input form-control',
                inputOpen:                'react-autosuggest__input--open',
                inputFocused:             'react-autosuggest__input--focused',
                suggestionsContainer:     'react-autosuggest__suggestions-container',
                suggestionsContainerOpen: 'react-autosuggest__suggestions-container--open',
                suggestionsList:          'react-autosuggest__suggestions-list',
                suggestion:               'react-autosuggest__suggestion',
                suggestionFirst:          'react-autosuggest__suggestion--first',
                suggestionHighlighted:    'react-autosuggest__suggestion--highlighted',
                sectionContainer:         'react-autosuggest__section-container',
                sectionContainerFirst:    'react-autosuggest__section-container--first',
                sectionTitle:             'react-autosuggest__section-title'
            }}
        />
    }
}