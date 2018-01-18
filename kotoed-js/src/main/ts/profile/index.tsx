import * as React from "react";
import {render} from "react-dom";

import "sass/kotoed-bootstrap/bootstrap.sass";
import {Denizen, WithDenizen} from "../data/denizen";
import {Kotoed} from "../util/kotoed-api";
import {eventBus} from "../eventBus";
import {sendAsync} from "../views/components/common";
import {DbRecordWrapper} from "../data/verification";
import {WithId} from "../data/common";
import ComponentWithLoading, {LoadingProperty} from "../views/components/ComponentWithLoading";
import UrlPattern = Kotoed.UrlPattern;
import SocialButton from "../login/components/SocialButton";

let params = Kotoed.UrlPattern.tryResolve(Kotoed.UrlPattern.Profile.Index, window.location.pathname) || new Map();
let userId = parseInt(params.get("id")) || -1;

interface ProfileInfo {
    id: number
    denizenId: string
    email?: string
    oauth: [string, string | null][]
    firstName?: string
    lastName?: string
    group?: string
}

interface ProfileComponentProps extends LoadingProperty {
    denizen?: ProfileInfo
}

interface ProfileComponentState {
}

export class ProfileComponent extends ComponentWithLoading<ProfileComponentProps, ProfileComponentState> {
    constructor(props: ProfileComponentProps) {
        super(props);
        this.state = {}
    }

    editUrl = UrlPattern.reverse(UrlPattern.Profile.Edit, {id: userId});

    renderBody = () => {
        return <div className="panel">
            <div className="panel-heading">
                <div className="row">
                    <div className="col-sm-offset-1 col-sm-11">
                        <h2>{this.props.denizen!.denizenId}</h2>
                    </div>
                </div>
            </div>
            <div className="panel-body">
                <form className="form-horizontal">
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputEmail">Email</label>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.email || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputFirstName">First name</label>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.firstName || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputLastName">Last name</label>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.lastName || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <label className="control-label col-sm-2"
                               htmlFor="inputGroup">Group #</label>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.group || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <div className="col-sm-offset-2 col-sm-10">
                            <a className="btn btn-default" href={this.editUrl}>Edit</a>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    };

    render() {
        return <div className="row">
            { this.props.loading? this.renderVeil(): this.renderBody() }
        </div>
    }
}

interface ProfileWrapperState {
    denizen?: ProfileInfo
}

class ProfileWrapper extends React.Component<{}, ProfileWrapperState> {
    constructor(props: {}) {
        super(props);
        this.state = {}
    }

    loadDenizen = async () => {
        let profile =
            await sendAsync<WithId, ProfileInfo>(Kotoed.Address.Api.Denizen.Profile.Read,
                {id: userId});
        this.setState({denizen: profile})
    };

    componentDidMount() {
        this.loadDenizen()
    }

    render() {
        return <ProfileComponent
            loading={!this.state.denizen}
            denizen={this.state.denizen}
        />;
    }
}

render(
    <ProfileWrapper/>,
    document.getElementById('profile-app')
);
