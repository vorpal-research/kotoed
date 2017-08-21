package org.jetbrains.research.kotoed.web.data

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable

object Permissions {
    data class Root(val createCourse: Boolean = false): Jsonable
    data class Course(val createProject: Boolean = false): Jsonable
    data class Project(val createSubmission: Boolean = false): Jsonable
    data class Submission(val editOwnComments: Boolean = false,
                          val editAllComments: Boolean = false,
                          val changeStateOwnComments: Boolean = false,
                          val changeStateAllComments: Boolean = false,
                          val postComment: Boolean = false) : Jsonable

}