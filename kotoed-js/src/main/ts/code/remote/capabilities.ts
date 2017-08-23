import axios from "axios"
import {keysToCamelCase} from "../../util/stringCase";
import {Kotoed} from "../../util/kotoed-api";
import {DenizenPrincipal} from "../../data/denizen";
export interface Permissions {
    editOwnComments: boolean
    editAllComments: boolean,
    changeStateOwnComments: boolean,
    changeStateAllComments: boolean,
    postComment: boolean
}

export interface Capabilities {
    principal: DenizenPrincipal
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
