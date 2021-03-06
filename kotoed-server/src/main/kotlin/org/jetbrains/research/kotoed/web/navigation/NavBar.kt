package org.jetbrains.research.kotoed.web.navigation

import io.vertx.ext.auth.User
import org.jetbrains.research.kotoed.util.isAuthorisedAsync
import org.jetbrains.research.kotoed.util.safeNav
import org.jetbrains.research.kotoed.web.UrlPattern
import org.jetbrains.research.kotoed.web.auth.Authority

val NavBarContextName = "navbar"

interface NavBarElement {
    val type: String // To make things easier for template engine. It cannot do instanceof
}

data class NavBarLink(val text: String, val href: String = "#") : NavBarElement {
    override val type = "link"
}

// Only one nesting level, sorry
data class NavBarMenu(val text: String, val children: List<NavBarLink>, val id: String? = null) : NavBarElement {
    override val type = "menu"
}

data class NavBar(val leftElems: List<NavBarElement> = listOf(),
                  val rightElems: List<NavBarElement> = listOf(),
                  val rootHref: String = "#")


suspend private fun User?.getUtilities() =
        when {
            this == null -> listOf()
            isAuthorisedAsync(Authority.Teacher) -> listOf(
                    NavBarLink("Comment templates", UrlPattern.CommentTemplate.Show),
                    NavBarLink("User search", UrlPattern.Denizen.Search),
                    NavBarLink("Comment search", UrlPattern.Comment.Search),
                    NavBarLink("Current builds", UrlPattern.BuildSystem.Summary))
            else -> listOf()
        }


suspend fun kotoedNavBar(user: User?): NavBar {
    val utils = user.getUtilities()

    val leftElemsSeq = sequence {
        if (utils.isNotEmpty())
            yield(NavBarMenu("Utilities", children = utils))

        yield(NavBarMenu("Updates", children = listOf(), id = "notifications-menu"))
    }

    val rightElems =
            if (user == null)
                listOf(NavBarLink("Anonymous"))
            else
                listOf(
                        NavBarMenu(
                                user.principal()?.getString("denizenId") ?:
                                        throw IllegalArgumentException("I can work only with UavUsers"),
                                listOf(
                                        NavBarLink("Profile",
                                                UrlPattern.reverse(UrlPattern.Profile.Index, mapOf("id" to
                                                        user.principal().safeNav("id")!!
                                                ))
                                        ),
                                        NavBarLink("Logout", UrlPattern.Auth.Logout)
                                )))
    return NavBar(leftElems = leftElemsSeq.toList(), rightElems = rightElems, rootHref = UrlPattern.Index)
}
