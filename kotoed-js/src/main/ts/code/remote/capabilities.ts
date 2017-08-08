import axios from "axios"
import {keysToCamelCase} from "../../util/stringCase";

const CAPABILITES_URL = "/codereview-api/caps";

function getCapabilitiesUrl(submissionId: number) {
    return `/codereview-api/caps/${submissionId}`
}

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

export async function fetchCapabilities(submissionid: number) {
    let resp = await axios.get(getCapabilitiesUrl(submissionid));
    return keysToCamelCase(resp.data) as Capabilities
}