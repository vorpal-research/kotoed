import {FileTreePath} from "./state";

export abstract class CodeReviewError extends Error {
    constructor(message: string) {
        super(message);
    }
}

export class FileTreeError extends CodeReviewError {
    constructor(numPath: FileTreePath) {
        super(`Path ${numPath.join(".")} not found in tree.`);
    }
}


export class FileNotFoundError extends CodeReviewError {
    constructor(path: string) {
        super(`Path ${path} not found in filetree.`);
    }
}