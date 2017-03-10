package controllers

import play.mvc.Controller
import play.mvc.Results
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KotlinController @Inject constructor() : Controller() {
    fun ping() = Results.ok("Pong!")
}
