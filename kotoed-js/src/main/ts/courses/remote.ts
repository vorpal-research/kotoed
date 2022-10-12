import axios from "axios"

import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";
import {sendAsync} from "../views/components/common";
import {WithId} from "../data/common";
import {DbRecordWrapper} from "../data/verification";
import {CourseToRead} from "../data/course";

export interface RootPermissions {
    createCourse: boolean,
    tags: boolean
}

export async function fetchPermissions(): Promise<RootPermissions> {
    let permissionsResp = await axios.get(Kotoed.UrlPattern.AuthHelpers.RootPerms);

    return keysToCamelCase(permissionsResp.data)
}

export async function fetchCourse(id: number): Promise<DbRecordWrapper<CourseToRead>> {
    return await sendAsync(
        Kotoed.Address.Api.Course.Read, {
            id
        });
}
