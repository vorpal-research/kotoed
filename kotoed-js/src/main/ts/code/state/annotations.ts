import {List, Map} from "immutable";

export interface CodeAnnotation {
    message: string,
    severity: "warning" | "error",
    position: {
        line: number,
        col: number
    }
}

export type ReviewAnnotations = Map<string, CodeAnnotation[]>
export const ReviewAnnotations = () => Map<string, CodeAnnotation[]>();

export interface CodeAnnotationsState {
    annotations: ReviewAnnotations,
    loading: boolean
}
