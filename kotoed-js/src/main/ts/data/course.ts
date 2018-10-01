
import {WithId} from "./common";

export interface Course {
    name: string
    baseRepoUrl?: string
    baseRevision?: string
    buildTemplateId?: number
    state: "open" | "closed" | "frozen"
    icon?: string
}

export interface CourseToRead extends Course, WithId {}

export interface WithCourse {
    course: CourseToRead
}