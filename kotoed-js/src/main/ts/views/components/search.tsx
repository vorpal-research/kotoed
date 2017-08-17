import * as React from "react";
import * as _ from "lodash";

import {Pagination} from "react-bootstrap";
import {sendAsync} from "./common";
import * as QueryString from "query-string";

import "less/kotoed-bootstrap/bootstrap.less";
import "less/search.less";

export interface SearchBarProps {
    initialText: string
    onChange: (state: SearchBarState) => void
}

export interface SearchBarState {
    text: string
}

export class SearchBar extends React.Component<SearchBarProps, SearchBarState> {
    constructor(props: SearchBarProps) {
        super(props);
        this.state = {
            text: props.initialText
        };
    }

    private notify = _.debounce(() => this.props.onChange(this.state), 300);

    updateText = (newText: string) => {
        this.setState({text: newText}, this.notify);
    };

    render() {
        return (
            <div className="search-bar">
                <div className="input-group">
                    <input className="search-query form-control"
                           placeholder="Search"
                           type="text"
                           value={this.state.text}
                           onChange={(e) => this.updateText(e.target.value)}
                    />
                </div>
            </div>
        );
    }
}

const PAGESIZE = 20;

interface SearchResultProps {
    overlayContents?: JSX.Element
}

export class SearchResult extends React.PureComponent<SearchResultProps> {
    constructor(props: SearchResultProps) {
        super(props);
    }

    renderOverlay = () => {
        return <div className="search-preview-overlay">
            <div style={{right: 0, bottom: 0, position: "absolute"}}>
                {this.props.overlayContents}
            </div>
        </div>
    };

    render() {
        return (
            <div className="panel search-preview">
                {"overlayContents" in this.props ? this.renderOverlay() : null}
                {this.props.children}
            </div>
        )
    }

}

export interface SearchTableProps<DataType> {
    searchAddress: string,
    countAddress: string,
    elementComponent: (key: string, data: DataType) => JSX.Element
}

export interface SearchTableState<DataType> extends SearchBarState {
    currentPage: number,
    pageCount: number,
    currentResults: Array<DataType>
}

export class SearchTable<DataType> extends
    React.Component<SearchTableProps<DataType>, SearchTableState<DataType>> {
    constructor(props: SearchTableProps<DataType>) {
        super(props);

        this.state = {
            text: QueryString.parse(location.hash).text || "",
            currentPage: parseInt(QueryString.parse(location.hash).currentPage) || 0,
            pageCount: 0,
            currentResults: []
        };
    }

    private hash = () => {
        let hash = QueryString.parse(location.hash);
        return "#" + QueryString.stringify({
            ...hash,
            text: this.state.text,
            currentPage: this.state.currentPage
        });
    };

    private queryCount = () => {
        let message = {
            text: this.state.text
        };
        sendAsync(this.props.countAddress, message)
            .then((resp: any) => this.setState({pageCount: Math.ceil(resp.count / PAGESIZE)}))
    };

    private queryData = () => {
        history.replaceState(undefined, this.state.text, this.hash());
        let message = {
            text: this.state.text,
            currentPage: this.state.currentPage,
            pageSize: PAGESIZE
        };
        sendAsync(this.props.searchAddress, message)
            .then<any>(resp => this.setState({currentResults: resp as any[]}))
    };

    onPageChanged = (page: number) => {
        page = Math.round(Math.max(0, Math.min(page, this.state.pageCount - 1)));
        this.setState({currentPage: page}, this.queryData)
    };

    onSearchStateChanged = (state: SearchBarState) => {
        this.setState({...state, currentPage: 0}, () => {
            this.queryCount();
            this.queryData()
        });
    };

    onKeyPressedGlobal = _.debounce((e: any) => {
        if (e.ctrlKey && e.key === "ArrowLeft") {
            this.onPageChanged(this.state.currentPage - 1)
        } else if (e.ctrlKey && e.key === "ArrowRight") {
            this.onPageChanged(this.state.currentPage + 1)
        }
    }, 40);

    componentWillMount() {
        document.addEventListener("keydown", this.onKeyPressedGlobal);
    }

    componentDidMount() {
        if(this.state.text !== "" || this.state.currentPage != 0) {
            this.queryCount();
            this.queryData();
        }
    }

    componentWillUnmount() {
        document.removeEventListener("keydown", this.onKeyPressedGlobal);
    }

    render() {
        return (
            <div>
                <SearchBar
                    initialText={this.state.text}
                    onChange={this.onSearchStateChanged}
                />
                <Pagination
                    prev
                    next
                    first
                    last
                    ellipsis
                    boundaryLinks
                    items={this.state.pageCount}
                    maxButtons={5}
                    activePage={this.state.currentPage + 1}
                    onSelect={(e: any) => this.onPageChanged(e as number - 1)}
                />
                {this.state.currentResults.map((result, index) => this.props.elementComponent("result"+index, result))}
            </div>
        );
    }
}
