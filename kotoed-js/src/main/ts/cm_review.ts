/**
 * Created by gagarski on 7/6/17.
 */

import * as moment from 'moment'
import * as cm from "codemirror"

// TODO Templating with jQuery is ugly. Maybe switch to React or some template engine.

namespace CmReview {

    namespace Util {

        export function groupByLine(comments: Comment[]): Comment[][] {
            let grouped: Comment[][] = [];

            for (let comment of comments) {
                let current = grouped[comment.location.line] || [];
                current.push(comment);
                grouped[comment.location.line] = current;
            }

            return grouped;
        }
    }

    export type CommentState = "unpublished" | "open" | "closed";

    export class CommentLocation {
        constructor(
            readonly file: string,
            readonly line: number
        ) {}
    }

    export abstract class Comment {
        public dateTime: moment.Moment;
        constructor(
            public text: string,
            dateTime_: moment.MomentInput,
            public author: string,
            public location: CommentLocation,
            public state: CommentState = "unpublished"
        ) {
            this.dateTime = moment(dateTime_);
        }

        abstract render(): HTMLElement
    }


    export class VintageComment extends Comment {
        render(): HTMLElement {
            let el = document.createElement("div");
            el.className = "dummy_comment";
            el.innerHTML = this.author + " wrote at " + this.dateTime.format('LLLL') + ": " + this.text;
            return el;
        }

    }

    export class BootstrapComment extends Comment {
        render(): HTMLElement {
             let el = $('<div class="panel panel-danger comment">' +
                          '<div class="panel-heading comment-heading">' +
                          '</div>' +
                         '<div class="panel-body"><p class="comment-body">' +
                         '</p></div>' +
                        '</div>'
             );
             el.find(".comment-heading").text(this.author + " @ " + this.dateTime.format('LLLL'));
             el.find(".comment-body").html(this.text);
             return el[0]
        }

    }

    export abstract class CommentPublisher {
        private queue: Array<Comment> = [];

        enqueue(comment: Comment): void {
            this.queue.push(comment);
        }

        getQueueLength(): number {
            return this.queue.length;
        }

        abstract publish(comment: Comment): void

        publishAll(): void {
            for (let comment of this.queue) {
                this.publish(comment);
            }
        }

        abstract update(comment: Comment, patch: {state?: CommentState, text?: string}): void

    }

    export class FileReview {

        private widgetNodes: Array<HTMLElement> = [];
        private editor: cm.Editor;

        constructor(textArea: HTMLTextAreaElement,
                    editorConf: cm.EditorConfiguration,
                    private publisher: CommentPublisher | null,
                    private readOnly: boolean = true,
                    private initialComments: Array<Comment> = []
        ) {
            this.editor = cm.fromTextArea(textArea, editorConf);
            this.editor.setSize(null, 800);

            // Start kill me pls
            let arrowOffset = 0.0;

            $(this.editor.getGutterElement()).children().each(function(ix, elem) {
                let jqel = $(elem);
                if (!jqel.hasClass("review-gutter")) {
                    arrowOffset += jqel.width();
                } else {
                    arrowOffset += jqel.width() / 2;
                    return false;
                }
            });

            arrowOffset -= 5;  // Arrow border

            // End kill me pls

            let grouped = Util.groupByLine(initialComments);


            for (let line in grouped) {
                let comments = grouped[line];

                let normLine = parseInt(line) - 1;  // CM line numeration is zero-based

                let badge = $(
                    '<div class="comments-counter-wrapper">' +
                        '<span class="label label-danger comments-counter review-hidden">' +
                            comments.length +
                        '</span>' +
                    '</div>'
                );

                this.editor.setGutterMarker(normLine, "review-gutter", badge[0]);

                let multipleCommentsDiv = $('<div class="line-comments col-md-12"></div>');
                let arrow = $('<div class="line-comments-arrow"></div>');

                arrow.css("left", arrowOffset);
                for (let comment of comments) {
                    multipleCommentsDiv.append(comment.render());
                }


                let multipleCommentsDivWithArrow = $('<div></div>');
                multipleCommentsDivWithArrow.append(multipleCommentsDiv);
                multipleCommentsDivWithArrow.append(arrow);


                this.widgetNodes[normLine] = multipleCommentsDivWithArrow[0];
            }
            this.editor.on('gutterClick', (instance, line, gutter, clickEvent) => {
                if (gutter !== "review-gutter")
                    return;

                let li = this.editor.lineInfo(line);

                if (!li.gutterMarkers)
                    return;

                let marker = $(li.gutterMarkers["review-gutter"]);

                if (!marker)
                    return;

                let counter = marker.find(".comments-counter");

                if ($(counter).hasClass("review-hidden")) {
                    $(counter).removeClass("review-hidden");
                    $(counter).removeClass("label-danger");
                    $(counter).addClass("review-shown");
                    $(counter).addClass("label-default");
                    this.editor.addLineWidget(line, this.widgetNodes[line], {
                        coverGutter: true,
                        noHScroll: false,
                        above: false,
                        showIfHidden: false
                    });
                } else {
                    $(counter).removeClass("review-shown");
                    $(counter).removeClass("label-default");
                    $(counter).addClass("review-hidden");
                    $(counter).addClass("label-danger");
                    li.widgets[0].clear();
                }
            });

        }

    }
}

export = CmReview