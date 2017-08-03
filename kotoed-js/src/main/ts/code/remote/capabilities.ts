import axios from "axios"
import {keysToCamelCase} from "../../util/stringCase";

const CAPABILITES_URL = "/codereview-api/caps";

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

export async function fetchCapabilities() {
    let resp = await axios.get(CAPABILITES_URL);
    return keysToCamelCase(resp.data) as Capabilities
}