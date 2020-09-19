package org.jetbrains.research.kotoed.web.data

import io.vertx.core.json.JsonObject
import org.jetbrains.research.kotoed.util.Jsonable

object Permissions {
    data class Root(val createCourse: Boolean = false): Jsonable
    data class Course(val createProject: Boolean = false,
                      val editCourse: Boolean = false,
                      val viewTags: Boolean = false): Jsonable
    data class Project(val createSubmission: Boolean = false, val deleteProject: Boolean = false): Jsonable
    data class Submission(val editOwnComments: Boolean = false,
                          val editAllComments: Boolean = false,
                          val changeStateOwnComments: Boolean = false,
                          val changeStateAllComments: Boolean = false,
                          val postComment: Boolean = false,
                          val resubmit: Boolean = false,
                          val changeState: Boolean = false,
                          val clean: Boolean = false,
                          val tags: Boolean = false,
                          val klones: Boolean = false) : Jsonable
}
