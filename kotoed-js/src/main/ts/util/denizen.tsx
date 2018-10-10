import * as React from "react"

import {BloatDenizen} from "../data/denizen";
import {Kotoed} from "./kotoed-api";
import {truncateString} from "./string";

export function makeRealName(denizen: BloatDenizen): string {
    if (!denizen.profiles || denizen.profiles.length == 0)
        return "";

    const {firstName, lastName, groupId} = denizen.profiles[0];

    if (!firstName && !lastName && !groupId)
        return "";

    const groupId_ = groupId ? `, ${groupId}`: "";

    // TODO truncation
    return `(${firstName} ${lastName}${groupId_})`
}

export function makeFullName(denizen: BloatDenizen): string {
    if (!denizen.profiles || denizen.profiles.length == 0)
        return "";

    let {firstName, lastName} = denizen.profiles[0];
    firstName = firstName || "";
    lastName = lastName || "";
    return (`${firstName} ${lastName}`).trim()
}

export function makeGroup(denizen: BloatDenizen): string {
    if (!denizen.profiles || denizen.profiles.length == 0)
        return "";

    return denizen.profiles[0].groupId || ""
}

export function makeProfileLink(denizen: BloatDenizen): JSX.Element {
    return <a href={Kotoed.UrlPattern.reverse(Kotoed.UrlPattern.Profile.Index, {id: denizen.id})}>
        {truncateString(denizen.denizenId, 16)}{" "}{makeRealName(denizen)}
    </a>
}