
import {eventBus} from "../../eventBus";
import {CommentsResponse} from "./comments";
import {Kotoed} from "../../util/kotoed-api";
import {sendAsync} from "../../views/components/common";

export interface CommentTemplate {
    id: number
    name: string
    text: string
}

export type CommentTemplates = CommentTemplate[]
export const CommentTemplates = () => [];

export async function fetchCommentTemplates(): Promise<CommentTemplates> {
    return sendAsync<{}, CommentTemplates>(Kotoed.Address.Api.CommentTemplate.ReadAll, {});
}
