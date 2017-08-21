import axios from "axios"

import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";

interface RootPermissions {
    createCourse: boolean
}

export async function fetchPermissions(): Promise<RootPermissions> {
    let permissionsResp = await axios.get(Kotoed.UrlPattern.AuthHelpers.RootPerms);

    return keysToCamelCase(permissionsResp.data)
}