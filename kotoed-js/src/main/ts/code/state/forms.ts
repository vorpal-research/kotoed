import {Map} from "immutable";

export interface FormState {
    processing: boolean
}

export const DEFAULT_FORM_STATE: FormState = {processing: false};

export type FileForms = Map<number, FormState>
export const FileForms = () => Map<number, FormState>();

export type ReviewForms = Map<string, FileForms>
export const ReviewForms = () => Map<string, FileForms>();
