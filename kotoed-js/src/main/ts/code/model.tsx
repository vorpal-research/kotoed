/**
 * Created by gagarski on 7/10/17.
 */

import * as moment from 'moment'
import {List} from "immutable";


export type CommentState = "unpublished" | "open" | "closed";

export interface CommentLocation {
    file: string
    line: number
}

export interface Comment {
    id: number,
    text: string
    dateTime: moment.MomentInput
    author: string
    location: CommentLocation
    state: CommentState
}

export type FileType = "file" | "directory"

export interface File {
    type: string;
    filename: string
}


export interface StoredFile extends File {
    isExpanded: boolean
    isLoaded: boolean
    children: List<StoredFile>,
    isSelected: boolean
}

export function fileToStoredFile(file: File): StoredFile {
    return {
        ...file,
        isExpanded: false,
        isLoaded: false,
        children: List(),
        isSelected: false
    }
}