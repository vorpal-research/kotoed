
import {WithId} from "./common";
import {WithBloatDenizen, WithDenizen} from "./denizen";
import {WithCourse} from "./course";

export type RepoType = "mercurial" | "git"

export interface Project {
    denizenId: number
    courseId: number
    repoType: RepoType
    repoUrl: string
    name: string
}

export interface ProjectToRead extends Project, WithId {}

export interface BloatProject extends ProjectToRead, WithBloatDenizen, WithCourse {}

export interface WithProject {
    project: Project
}
