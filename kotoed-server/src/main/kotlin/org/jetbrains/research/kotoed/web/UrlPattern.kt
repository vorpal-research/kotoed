package org.jetbrains.research.kotoed.web

object UrlPattern {
    // TODO maybe make parameter names const values
    const val Star = "/*"
    const val Index =  "/"
    const val NotImplemented = "/notImplemented"

    object CodeReview {
        const val Index = "/codereview/:id/*"
    }

    object Auth {
        const val Index = "/login"
        const val DoLogin = "/login/doLogin"
        const val DoSignUp = "/login/doSignUp"
        const val LoginDone = "/login/done"
        const val Logout = "/logout"
        const val OAuthStart = "/login/oauth/start/:providerName"
        const val OAuthCallback = "/login/oauth/callback/:providerName"
    }

    object AuthHelpers {
        const val WhoAmI = "/whoAmI"
        const val RootPerms = "/perms/root"
        const val CoursePerms = "/perms/course/:id"
        const val ProjectPerms = "/perms/project/:id"
        const val SubmissionPerms = "/perms/submission/:id"
    }

    object Comment {
        const val ById = "/views/comment/id/:id"
        const val Search = "/views/comment/search"
    }

    object Course {
        const val Index = "/course/:id"
    }

    object Project {
        const val Index = "/project/:id"
        const val Search = "/views/project/search"
    }

    object Submission {
        const val Results = "/views/submission/:id/results"
        const val Index = "/submission/:id"
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
