package org.jetbrains.research.kotoed.web.navigation

import org.jetbrains.research.kotoed.database.tables.records.*
import org.jetbrains.research.kotoed.web.UrlPattern


data class BreadCrumbElement(val text: String, val active: Boolean = false, val href: String? = null)
class BreadCrumb(val elements: List<BreadCrumbElement>) {
    constructor(elem: BreadCrumbElement) : this(listOf(elem))
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
    return BreadCrumbElement(
            text = course.name,
            active = active,
            href = UrlPattern.reverse(UrlPattern.Course.Index, mapOf("id" to course.id)))
}

fun ProjectBreadCrumbElement(active: Boolean,
                             author: DenizenRecord,
                             authorProfile: ProfileRecord?,
                             project: ProjectRecord): BreadCrumbElement {

    val group = authorProfile?.groupId?.let {", $it"} ?: ""
    val realName = authorProfile?.let {" (${authorProfile.firstName} ${authorProfile.lastName}$group)"} ?: ""

    return BreadCrumbElement(
            text = "${project.name} by ${author.denizenId}$realName",
            active = active,
            href = UrlPattern.reverse(UrlPattern.Project.Index, mapOf("id" to project.id)))
}


fun SubmissionBreadCrumbElement(active: Boolean, submission: SubmissionRecord) =
        BreadCrumbElement(
                text = "Submission #${submission.id}",
                active = active,
                href = UrlPattern.reverse(UrlPattern.Submission.Index, mapOf("id" to submission.id))) // TODO

fun SubmissionResultBreadCrumbElement(active: Boolean, submission: SubmissionRecord) =
        BreadCrumbElement(
                text = "Results",
                active = active,
                href = UrlPattern.reverse(UrlPattern.Submission.Results, mapOf("id" to submission.id)))

fun SubmissionReviewBreadCrumbElement(active: Boolean, submission: SubmissionRecord) =
        BreadCrumbElement(
                text = "Review",
                active = active,
                href = UrlPattern.reverse(UrlPattern.CodeReview.Index, mapOf("id" to submission.id)))

fun UtilitiesBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text = "Utilities", active = active)

fun CommentSearchBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text = "Comment search", active = active, href = UrlPattern.Comment.Search)

fun ProjectSearchBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text = "Project search", active = active, href = UrlPattern.Project.Search)

fun SubmissionByTagsSearchBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text = "Tag search", active = active, href = UrlPattern.Submission.SearchByTags)

fun DenizenSearchBreadCrumbElement(active: Boolean) =
        BreadCrumbElement(text = "User search", active = active, href = UrlPattern.Denizen.Search)


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// Crumbs
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

val RootBreadCrumb = BreadCrumb(RootBreadCrumbElement(true))

fun CourseBreadCrumb(course: CourseRecord) = RootBreadCrumbElement(false) + CourseBreadCrumbElement(true, course)


fun ProjectBreadCrumb(course: CourseRecord,
                      author: DenizenRecord,
                      authorProfile: ProfileRecord?,
                      project: ProjectRecord) =
        RootBreadCrumbElement(false) +
                CourseBreadCrumbElement(false, course) +
                ProjectBreadCrumbElement(true, author, authorProfile, project)

fun SubmissionBreadCrumb(course: CourseRecord,
                         author: DenizenRecord,
                         authorProfile: ProfileRecord?,
                         project: ProjectRecord,
                         submission: SubmissionRecord): BreadCrumb {
    return RootBreadCrumbElement(false) +
            CourseBreadCrumbElement(false, course) +
            ProjectBreadCrumbElement(false, author, authorProfile, project) +
            SubmissionBreadCrumbElement(true, submission)
}

fun SubmissionResultBreadCrumb(course: CourseRecord,
                               author: DenizenRecord,
                               authorProfile: ProfileRecord?,
                               project: ProjectRecord,
                               submission: SubmissionRecord) =
        RootBreadCrumbElement(false) +
                CourseBreadCrumbElement(false, course) +
                ProjectBreadCrumbElement(false, author, authorProfile, project) +
                SubmissionBreadCrumbElement(false, submission) +
                SubmissionResultBreadCrumbElement(true, submission)


fun SubmissionReviewBreadCrumb(course: CourseRecord,
                               author: DenizenRecord,
                               authorProfile: ProfileRecord?,
                               project: ProjectRecord,
                               submission: SubmissionRecord) =
        RootBreadCrumbElement(false) +
                CourseBreadCrumbElement(false, course) +
                ProjectBreadCrumbElement(false, author, authorProfile, project) +
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

val SubmissionByTagsSearchBreadCrumb =
        RootBreadCrumbElement(false) +
                UtilitiesBreadCrumbElement(true) +
                SubmissionByTagsSearchBreadCrumbElement(true)

val DenizenSearchBreadCrumb =
        RootBreadCrumbElement(false) +
                UtilitiesBreadCrumbElement(true) +
                DenizenSearchBreadCrumbElement(true)
