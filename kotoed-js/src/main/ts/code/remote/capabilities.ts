import axios from "axios"
import {keysToCamelCase} from "../../util/stringCase";
import {Kotoed} from "../../util/kotoed-api";

export interface Principal {
    denizenId: string
    id: number
}

export interface Permissions {
    editOwnComments: boolean
    editAllComments: boolean,
    changeStateOwnComments: boolean,
    changeStateAllComments: boolean,
    postComment: boolean
}

export interface Capabilities {
    principal: Principal
    permissions: Permissions
}

export async function fetchCapabilities(submissionid: number): Promise<Capabilities> {
    let principalResp = await axios.get(Kotoed.UrlPattern.AuthHelpers.WhoAmI);
    let permissionsResp = await axios.get(
        Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.AuthHelpers.SubmissionPerms, {
            id: submissionid
        }));
    return {principal: keysToCamelCase(principalResp.data), permissions: keysToCamelCase(permissionsResp.data)}
}
