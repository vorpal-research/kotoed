import axios from "axios"

import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";

interface CoursePermissions {
    createProject: boolean
}

export async function fetchPermissions(id: number): Promise<CoursePermissions> {
    let permissionsResp = await axios.get(
        Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.AuthHelpers.CoursePerms, {id})
    );

    return keysToCamelCase(permissionsResp.data)
}
