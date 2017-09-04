import axios from "axios"

import {Kotoed} from "../util/kotoed-api";
import {keysToCamelCase} from "../util/stringCase";
import {ProjectToRead} from "../data/project";
import {sendAsync} from "../views/components/common";
import {DbRecordWrapper} from "../data/verification";
import {WithId} from "../data/common";

interface ProjectPermissions {
    createSubmission: boolean
}

export async function fetchPermissions(id: number): Promise<ProjectPermissions> {
    let permissionsResp = await axios.get(
        Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.AuthHelpers.ProjectPerms, {id})
    );

    return keysToCamelCase(permissionsResp.data)
}

export async function fetchProject(id: number): Promise<DbRecordWrapper<ProjectToRead>> {
    return await sendAsync<WithId, DbRecordWrapper<ProjectToRead>>(
        Kotoed.Address.Api.Project.Read, {
            id
        });
}
