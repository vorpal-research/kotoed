import * as React from "react";
import {render} from "react-dom";

import "less/kotoed-bootstrap/bootstrap.less"
import {Denizen, WithDenizen} from "../data/denizen";
import {Kotoed} from "../util/kotoed-api";
import {eventBus} from "../eventBus";
import {sendAsync} from "../views/components/common";
import {DbRecordWrapper} from "../data/verification";
import {WithId} from "../data/common";
import ComponentWithLoading, {LoadingProperty} from "../views/components/ComponentWithLoading";
import UrlPattern = Kotoed.UrlPattern;
import SocialButton from "../login/components/SocialButton";
import "less/profile.less"

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
                    <div className="col-sm-12 text-center">
                        <h2>{this.props.denizen!.denizenId}</h2>
                    </div>
                </div>
            </div>
            <div>
                <hr></hr>
            </div>
            <div className="panel-body">
                <div className="card-body">
                    <div className="form-group">
                        <small className="control-label text-muted col-sm-2">Email</small>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.email || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <small className="control-label text-muted col-sm-2">First name</small>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.firstName || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <small className="control-label text-muted col-sm-2">Last name</small>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.lastName || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <small className="control-label text-muted col-sm-2">Group #</small>
                        <div className="col-sm-10">
                            <p className="form-control-static">{this.props.denizen!.group || "not specified"}</p>
                        </div>
                    </div>
                    <div className="form-group">
                        <div className="col-4 text-center">
                            <a className="btn btn-default" href={this.editUrl}>Edit</a>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    };

    render() {
        return <div className="row">
            {this.props.loading ? this.renderVeil() : this.renderBody()}
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
            await sendAsync(Kotoed.Address.Api.Denizen.Profile.Read, {id: userId});
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
