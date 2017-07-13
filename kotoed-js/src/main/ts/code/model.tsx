/**
 * Created by gagarski on 7/10/17.
 */

import * as moment from 'moment'


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
    type: "file";
    name: string;
}

export interface Directory {
    type: "directory";
    name: string;
    children: Array<File | Directory>
}

