package org.jetbrains.research.kotoed.web

object UrlPattern {
    // TODO maybe make parameter names const values
    const val Star = "/*"
    const val Index = "/"

    object Redirect {
        const val ById = "/redirect/:entity/:id"
    }

    object CodeReview {
        const val Index = "/submission/:id/review/*"
    }

    object Auth {
        const val Index = "/auth/login"
        const val DoLogin = "/auth/login/doLogin"
        const val DoSignUp = "/auth/login/doSignUp"
        const val LoginDone = "/auth/login/done"
        const val Logout = "/auth/logout"
        const val OAuthStart = "/auth/oauth/start/:providerName"
        const val OAuthCallback = "/auth/oauth/callback/:providerName"
        const val ResetPassword = "/auth/resetPassword"
        const val RestorePassword = "/auth/restorePassword/:uid"
    }

    object AuthHelpers {
        const val WhoAmI = "/auth/whoAmI"
        const val RootPerms = "/auth/perms/root"
        const val CoursePerms = "/auth/perms/course/:id"
        const val ProjectPerms = "/auth/perms/project/:id"
        const val SubmissionPerms = "/auth/perms/submission/:id"
    }

    object Comment {
        const val ById = "/redirect/comment/:id"
        const val Search = "/search/comment"
    }

    object Course {
        const val Index = "/course/:id"
    }

    object Project {
        const val Index = "/project/:id"
        const val Search = "/search/project"
    }

    object Submission {
        const val Results = "/submission/:id/results"
        const val Index = "/submission/:id"
        const val NotificationRedirect = "/redirect/submission/:id" // TODO think about it

        const val SearchByTags = "/search/byTags"
    }

    object Profile {
        const val Index = "/auth/profile/:id"
        const val Edit = "/auth/profile/edit/:id"
    }

    object Denizen {
        const val Search = "/search/denizen"
    }

    object SubmissionResults {
        const val ById = "/redirect/submissionResults/:id"
    }

    const val EventBus = "/eventbus/*"
    const val Static = "/static/*"

    /**
     * Each Any in parameter will be converted to string
     */
    fun reverse(pattern: String, params: Map<String, Any>, star: Any = ""): String {
        var url = pattern
        for ((k, v) in params) {
            url = url.replace(":$k", "$v")
        }

        url = url.replace("*", "$star")

        return url
    }
}
