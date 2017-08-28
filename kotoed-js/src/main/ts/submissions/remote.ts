import axios from "axios"

import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";

interface ProjectPermissions {
    createSubmission: boolean
}

export async function fetchPermissions(id: number): Promise<ProjectPermissions> {
    let permissionsResp = await axios.get(
        Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.AuthHelpers.ProjectPerms, {id})
    );

    return keysToCamelCase(permissionsResp.data)
}
