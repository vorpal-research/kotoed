import axios from "axios"
import {keysToCamelCase} from "../../util/stringCase";
import {Kotoed} from "../../util/kotoed-api";
import {DenizenPrincipal} from "../../data/denizen";
import {fetchPermissions, SubmissionPermissions} from "../../submissionDetails/remote";

export interface Capabilities {
    principal: DenizenPrincipal
    permissions: SubmissionPermissions
}

export async function fetchCapabilities(submissionId: number): Promise<Capabilities> {
    let principalResp = await axios.get(Kotoed.UrlPattern.AuthHelpers.WhoAmI);
    let permissions = await fetchPermissions(submissionId);
    return {principal: keysToCamelCase(principalResp.data), permissions}
}
