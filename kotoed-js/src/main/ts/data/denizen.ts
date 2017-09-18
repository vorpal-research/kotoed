
export interface DenizenPrincipal {
    denizenId: string
    id: number
}

export interface Denizen extends DenizenPrincipal {
    email?: string
}

export interface WithDenizen {
    denizen: Denizen
}


export interface Profile {
    firstName?: string
    lastName?: string
    groupId?: string
}

export interface WithProfiles {
    profiles?: Array<Profile>
}

export interface BloatDenizen extends Denizen, WithProfiles {}

export interface WithBloatDenizen {
    denizen: BloatDenizen
}
