
import {WithId} from "./common";

export interface Course {
    name: string
}

export interface CourseToRead extends Course, WithId {}

export interface WithCourse {
    course: CourseToRead
}