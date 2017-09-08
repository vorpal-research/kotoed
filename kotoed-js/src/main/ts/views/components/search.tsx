import * as React from "react";
import * as _ from "lodash";

import {Pagination} from "react-bootstrap";
import {sendAsync} from "./common";
import * as QueryString from "query-string";

import "less/kotoed-bootstrap/bootstrap.less";
import "less/search.less";
import "less/util.less";
import {identity} from "../../util/common";
import {isStatusFinal, WithVerificationData} from "../../data/verification";
import {pollDespairing} from "../../util/poll";

export interface SearchBarProps {
    initialText: string
    onChange: (state: SearchBarState) => void
}

export interface SearchBarState {
    text: string
}

export class SearchBar extends React.Component<SearchBarProps, SearchBarState> {
    private input: HTMLInputElement;

    constructor(props: SearchBarProps) {
        super(props);
        this.state = {
            text: props.initialText
        };
    }

    componentDidMount() {
        this.input.focus();
    }

    private notify = _.debounce(() => this.props.onChange(this.state), 300);

    updateText = (newText: string) => {
        this.setState({text: newText}, this.notify);
    };

    render() {
        return (
                <div className="has-feedback">
                    <input className="form-control input"
                           ref={(me: HTMLInputElement) => this.input = me}
                           placeholder="Search"
                           type="text"
                           value={this.state.text}
                           onChange={(e) => this.updateText(e.target.value)}
                    />
                    <span className="glyphicon glyphicon-search form-control-feedback grayed-out"/>
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

type ShouldPerformInitialSearch = {
    (text: string, page: number): boolean
}

export type MakeBaseQuery<T> = {
    (): T
}

interface GroupProps {
    by: number
    using: (children: Array<JSX.Element>) => JSX.Element
}

export interface SearchTableProps<DataType, QueryType = {}> {
    searchAddress: string,
    countAddress: string,
    elementComponent: (key: string, data: DataType) => JSX.Element
    shouldPerformInitialSearch?: ShouldPerformInitialSearch
    makeBaseQuery?: MakeBaseQuery<QueryType>
    forcePagination?: boolean
    pageSize?: number
    wrapResults?: (children: Array<JSX.Element>) => JSX.Element // Wanna table? This is for you!
    group?: GroupProps // Wanna group to columns or something other?
    toolbarComponent?: (toggleSearch: () => void) => JSX.Element | null
    withSearch?: boolean
}

export interface SearchTableState<DataType> extends SearchBarState {
    currentPage: number,
    pageCount: number,
    currentResults: Array<DataType>
    touched: boolean
    inProgress: boolean
}

interface SearchQuery {
    text: string,
    currentPage?: number,
    pageSize?: number
}

interface CountResponse {
    count: number
}

export class SearchTable<DataType, QueryType = {}> extends
    React.Component<SearchTableProps<DataType, QueryType>, SearchTableState<DataType>> {

    private shouldPerformInitialSearch: ShouldPerformInitialSearch;
    protected makeBaseQuery: MakeBaseQuery<Partial<QueryType>>;
    private wrapResults: (children: Array<JSX.Element>) => JSX.Element | Array<JSX.Element>; // Array if if we're doing no wrap
    private pageSize: number;
    private renderToolbar: () => JSX.Element | null;
    private withSearch: boolean;

    private setPrivateFields(props: SearchTableProps<DataType, QueryType> ) {
        this.shouldPerformInitialSearch = props.shouldPerformInitialSearch || ((text, page) => (text !== "" || page != 0));
        this.makeBaseQuery = props.makeBaseQuery || (() => {return {}});
        this.wrapResults = props.wrapResults || identity;
        this.pageSize = props.pageSize || PAGESIZE;
        this.withSearch = props.withSearch === undefined ? true : props.withSearch;
        if (props.toolbarComponent !== undefined) {
            let tbc = props.toolbarComponent;
            this.renderToolbar =  () => <div className="search-toolbar">
                <div className="pull-right">
                    {tbc(this.redoSearch)}
                </div>
                <div className="clearfix"/>
                <div className="vspace-10"/>
            </div>;
        } else {
            this.renderToolbar = () => null
        }
    }

    constructor(props: SearchTableProps<DataType, QueryType>) {
        super(props);

        this.setPrivateFields(props);

        let text = QueryString.parse(location.hash).text || "";
        let currentPage = parseInt(QueryString.parse(location.hash).currentPage) || 0;
        this.state = {
            text,
            currentPage,
            pageCount: 0,
            currentResults: [],
            touched: false,
            inProgress: this.shouldPerformInitialSearch(text, currentPage)
        };
    }


    componentWillReceiveProps(props: SearchTableProps<DataType, QueryType>) {
        this.setPrivateFields(props);
    }

    private hash = () => {
        let hash = QueryString.parse(location.hash);
        return "#" + QueryString.stringify({
            ...hash,
            text: this.state.text,
            currentPage: this.state.currentPage
        });
    };

    protected doQueryCount = async () => {
        let searchQuery = {
            text: this.state.text
        };
        let message: SearchQuery & Partial<QueryType> = Object.assign({}, searchQuery, this.makeBaseQuery());

        return sendAsync<SearchQuery & Partial<QueryType>, CountResponse>(this.props.countAddress, message);
    };

    protected queryCount = async () => {
        let resp = await this.doQueryCount();
        this.setState({pageCount: Math.ceil(resp.count / PAGESIZE)});
    };

    protected doQueryData = async () => {
        history.replaceState(undefined, this.state.text, this.hash());
        let searchQuery: SearchQuery = {
            text: this.state.text,
            currentPage: this.state.currentPage,
            pageSize: this.pageSize
        };

        let message: SearchQuery & Partial<QueryType> = Object.assign({}, searchQuery, this.makeBaseQuery());
        return sendAsync<SearchQuery & Partial<QueryType>, Array<DataType>>(this.props.searchAddress, message);
    };

    protected queryData = async () => {
        let resp = await this.doQueryData();
        this.setState({currentResults: resp, touched: true});
    };

    onPageChanged = (page: number) => {
        page = Math.round(Math.max(0, Math.min(page, this.state.pageCount - 1)));
        this.setState({currentPage: page}, () => {
            this.query(false)
        })
    };

    query = (withCount: boolean = true) => {
        this.setState({inProgress: true}, async () => {
            let promises = [this.queryData()];
            if (withCount) {
                promises.push(this.queryCount())
            }
            await Promise.all(promises);
            this.setState({inProgress: false});
        });


    };


    redoSearch = () => {
        this.setState({currentPage: 0}, () => {
            this.query()
        });
    };

    onSearchStateChanged = (state: SearchBarState) => {
        this.setState({...state, currentPage: 0}, () => {
            this.query()
        });
    };

    onKeyPressedGlobal = _.debounce((e: any) => {
        if ((e.metaKey || e.ctrlKey) && e.key === "ArrowLeft") {
            this.onPageChanged(this.state.currentPage - 1)
        } else if ((e.metaKey || e.ctrlKey) && e.key === "ArrowRight") {
            this.onPageChanged(this.state.currentPage + 1)
        }
    }, 40);

    componentWillMount() {
        document.addEventListener("keydown", this.onKeyPressedGlobal);
    }

    componentDidMount() {
        if(this.shouldPerformInitialSearch(this.state.text, this.state.currentPage)) {
            this.query();
        }
    }

    componentWillUnmount() {
        document.removeEventListener("keydown", this.onKeyPressedGlobal);
    }

    renderPagination = () => {
        if (this.state.pageCount > 1 || this.props.forcePagination)
            return <div className="text-center">
                <Pagination
                    className="pagination"
                    prev
                    next
                    first
                    last
                    ellipsis
                    boundaryLinks
                    items={this.state.pageCount}
                    maxButtons={5}
                    activePage={this.state.currentPage + 1}
                    onSelect={(e: any) => this.onPageChanged(e as number - 1)}/>
            </div>;
        else
            return <div className="vspace-10"/>
    };

    renderEmptyString = (): JSX.Element => {
        let str;
        if (this.state.inProgress)
            str = "Please wait...";
        else if (this.state.touched)
            str = "No results";
        else
            str = "Please start typing";
        return <div className="text-center no-results">
            {str}
        </div>
    };

    renderResults = () => {
        if (this.state.currentResults.length === 0)
            return this.renderEmptyString();
        else {
            let resEls = this.state.currentResults.map((result, index) => this.props.elementComponent("result"+index, result));
            let toWrap: Array<JSX.Element>;
            if (this.props.group) {
                toWrap = _.chunk(resEls, this.props.group.by).map(this.props.group.using);
            } else {
                toWrap = resEls;
            }
            return this.wrapResults(toWrap)
        }

    };

    render() {
        return (
            <div>
                {this.renderToolbar()}
                {this.withSearch && <SearchBar
                    initialText={this.state.text}
                    onChange={this.onSearchStateChanged}
                />}
                {this.renderPagination()}
                {this.renderResults()}
                {this.renderPagination()}
            </div>
        );
    }
}


export abstract class ChoosySearchTable<DataType, QueryType = {}> extends
        SearchTable<DataType, QueryType> {
    constructor(props: SearchTableProps<DataType & WithVerificationData, QueryType>) {
        super(props);

    }

    componentWillReceiveProps(props: SearchTableProps<DataType, QueryType>) {
        super.componentWillReceiveProps(props);
    }

    protected abstract isGoodEnough(data: Array<DataType & WithVerificationData>): boolean;

    private isQueryChanged = (oldState: SearchTableState<DataType>) => {
        return oldState.text !== this.state.text || oldState.currentPage !== this.state.currentPage
    };

    protected queryData = async () => {
        let oldState = this.state;

        const handleResult = (results: Array<DataType & WithVerificationData>) => {
            this.setState({currentResults: results, touched: true}, () => oldState = this.state);
        };

        await pollDespairing({
            action: this.doQueryData,
            isGoodEnough: this.isGoodEnough,
            onFinal: handleResult,
            onIntermediate: handleResult,
            strategyParams: {shouldGiveUp: () => this.isQueryChanged(oldState)}
        });
    }
}

export class ChoosyByVerDataSearchTable<DataType extends WithVerificationData, QueryType = {}>
        extends ChoosySearchTable<DataType, QueryType> {
    protected isGoodEnough(data: Array<DataType>) {
        return data.every((value: DataType & WithVerificationData) => isStatusFinal(value.verificationData.status))
    };
}