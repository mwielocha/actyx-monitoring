package controllers

import javax.inject.Inject
import javax.inject.Singleton

import play.api.mvc.Controller
import play.api.mvc.Action

import service.SamplingService


/**
 * Created by Mikolaj Wielocha on 04/05/16
 */

@Singleton
class ApplicationController @Inject() (private val samplingService: SamplingService) extends Controller {

  def main = Action { Ok } 

}
