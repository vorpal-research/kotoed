import axios from "axios"
import {keysToCamelCase} from "../../util/stringCase";
import {Kotoed} from "../../util/kotoed-api";
import {DenizenPrincipal, Profile, ProfileInfo} from "../../data/denizen";
import {fetchPermissions, SubmissionPermissions} from "../../submissionDetails/remote";
import {sendAsync} from "../../views/components/common";

export interface Capabilities {
    principal: DenizenPrincipal
    permissions: SubmissionPermissions
    profile: ProfileInfo
}

export async function fetchCapabilities(submissionId: number): Promise<Capabilities> {
    let principalResp = await axios.get(Kotoed.UrlPattern.AuthHelpers.WhoAmI);
    let permissions = await fetchPermissions(submissionId);
    let profile = await sendAsync(Kotoed.Address.Api.Denizen.Profile.Read, {id: principalResp.data.id})
    return {principal: keysToCamelCase(principalResp.data), permissions, profile}
}
