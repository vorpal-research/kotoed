package org.jetbrains.research.kotoed.data.web

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable

object CodeReview {
    data class Permissions(val editOwnComments: Boolean = false,
                           val editAllComments: Boolean = false,
                           val changeStateOwnComments: Boolean = false,
                           val changeStateAllComments: Boolean = false,
                           val postComment: Boolean = false) : Jsonable
    data class Capabilities(val principal: JsonObject, val permissions: Permissions) : Jsonable
}