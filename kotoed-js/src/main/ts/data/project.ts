
import {WithId} from "./common";
import {WithDenizen} from "./denizen";
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

export interface JumboProject extends ProjectToRead, WithDenizen, WithCourse {}
