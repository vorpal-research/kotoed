package org.jetbrains.research.kotoed.web.navigation

import io.vertx.core.Vertx
import org.jetbrains.research.kotoed.database.tables.records.CourseRecord
import org.jetbrains.research.kotoed.database.tables.records.DenizenRecord
import org.jetbrains.research.kotoed.database.tables.records.ProjectRecord
import org.jetbrains.research.kotoed.database.tables.records.SubmissionRecord
import org.jetbrains.research.kotoed.web.UrlPattern

val BreadCrumbContextName = "breadcrumb"

data class BreadCrumbElement(val text: String, val active: Boolean = false, val href: String? = null)
class BreadCrumb(val elements: List<BreadCrumbElement>) {
    constructor(elem: BreadCrumbElement): this(listOf(elem))
}

// TODO it's not very effective. Consider adding builder.
operator fun BreadCrumb.plus(elem: BreadCrumbElement) = BreadCrumb(elements + elem)
operator fun BreadCrumb.plus(otherElems: List<BreadCrumbElement>) = BreadCrumb(elements + otherElems)
operator fun BreadCrumbElement.plus(other: BreadCrumbElement) = BreadCrumb(listOf(this, other))

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Elements
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

fun RootBreadCrumbElement(active: Boolean) = BreadCrumbElement(text = "Root", href = UrlPattern.Index, active = active)

fun CourseBreadCrumbElement(active: Boolean, course: CourseRecord): BreadCrumbElement {
    return BreadCrumbElement(text = course.name, active = active, href=UrlPattern.NotImplemented) // TODO
}

fun ProjectBreadCrumbElement(active: Boolean,
                             author: DenizenRecord,
                             project: ProjectRecord): BreadCrumbElement {
    return BreadCrumbElement(text = "${project.name} by ${author.denizenId}", active = active, href=UrlPattern.NotImplemented)
}


fun SubmissionBreadCrumbElement(active: Boolean, submission: SubmissionRecord) =
        BreadCrumbElement(text = "Submission #${submission.id}", active = active, href=UrlPattern.NotImplemented) // TODO

fun SubmissionResultBreadCrumbElement(active: Boolean, submission: SubmissionRecord) =
        BreadCrumbElement(text = "Results", active = active, href=UrlPattern.NotImplemented) // TODO

fun SubmissionReviewBreadCrumbElement(active: Boolean, submission: SubmissionRecord) =
        BreadCrumbElement(text = "Review", active = active, href=UrlPattern.NotImplemented) // TODO

fun UtilitiesBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text = "Utilities", active = active)

fun CommentSearchBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text= "Comment search", active = active, href=UrlPattern.Comment.Search)

fun ProjectSearchBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text= "Project search", active = active, href=UrlPattern.Comment.Search)

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Crumbs
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

val RootBreadCrumb = BreadCrumb(RootBreadCrumbElement(true))

fun CourseBreadCrumb(course: CourseRecord) = RootBreadCrumbElement(false) + CourseBreadCrumbElement(true, course)


fun ProjectBreadCrumb(course: CourseRecord, author: DenizenRecord, project: ProjectRecord) =
        RootBreadCrumbElement(false) +
                CourseBreadCrumbElement(false, course) +
                ProjectBreadCrumbElement(true, author, project)

fun SubmissionBreadCrumb(course: CourseRecord, author: DenizenRecord, project: ProjectRecord, submission: SubmissionRecord): BreadCrumb {
    return RootBreadCrumbElement(false) +
            CourseBreadCrumbElement(false, course) +
            ProjectBreadCrumbElement(false, author, project) +
            SubmissionBreadCrumbElement(true, submission)
}

fun SubmissionResultBreadCrumb(course: CourseRecord, author: DenizenRecord, project: ProjectRecord, submission: SubmissionRecord) =
        RootBreadCrumbElement(false) +
            CourseBreadCrumbElement(false, course) +
            ProjectBreadCrumbElement(false, author, project) +
            SubmissionBreadCrumbElement(false, submission) +
            SubmissionResultBreadCrumbElement(true, submission)


fun SubmissionReviewBreadCrumb(course: CourseRecord, author: DenizenRecord, project: ProjectRecord, submission: SubmissionRecord) =
        RootBreadCrumbElement(false) +
            CourseBreadCrumbElement(false, course) +
            ProjectBreadCrumbElement(false, author, project) +
            SubmissionBreadCrumbElement(false, submission) +
            SubmissionReviewBreadCrumbElement(true, submission)


val CommentSearchBreadCrumb =
        RootBreadCrumbElement(false) +
                UtilitiesBreadCrumbElement(true) +
                CommentSearchBreadCrumbElement(true)

val ProjectSearchBreadCrumb =
        RootBreadCrumbElement(false) +
                UtilitiesBreadCrumbElement(true) +
                ProjectSearchBreadCrumbElement(true)

