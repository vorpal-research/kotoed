
import {WithId} from "./common";

export type CourseState =  "open" | "closed" | "frozen"

export interface Course {
    name: string
    baseRepoUrl?: string
    baseRevision?: string
    buildTemplateId?: number
    state: CourseState
    icon?: string
}

export interface CourseToRead extends Course, WithId {}

export interface WithCourse {
    course: CourseToRead
}