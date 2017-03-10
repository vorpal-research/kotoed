package controllers

import play.mvc.Controller
import play.mvc.Results

class KotlinController : Controller() {
    fun ping() = Results.ok("Pong!")
}
