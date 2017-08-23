
export interface DenizenPrincipal {
    denizenId: string
    id: number
}

export interface Denizen extends DenizenPrincipal {
    email: string
}

export interface WithDenizen {
    denizen: Denizen
}
