package controllers

import common.{ExecutionContexts, Logging}
import conf.Configuration
import play.Play
import play.api.libs.json._
import services.S3

import scala.concurrent.Future

trait S3BreakingNews extends S3 {
  override lazy val bucket = Configuration.aws.bucket
  lazy val stage = if (Play.isTest) "TEST" else Configuration.environment.stage.toUpperCase
  val namespace = "notifications"
  lazy val location = s"$stage/$namespace"
  def getKeyForPath(path: String): String = s"$location/$path.json"
}
object S3BreakingNews extends S3BreakingNews

trait BreakingNewsApi extends Logging with ExecutionContexts {

  val s3: S3BreakingNews
  val breakingNewskey = s3.getKeyForPath("breaking-news")

  @throws[Exception]
  def getBreakingNews: Option[JsValue] = {
    try {
      s3.get(breakingNewskey) match {
        case Some(s) =>
          Some(Json.parse(s))
        case _ =>
          val e = new Exception("No Breaking News content")
          log.error(e.getMessage)
          throw e
      }
    } catch {
      case e: Exception =>
        log.error(s"Cannot fetch Breaking News json (${e.getMessage})")
        throw e
    }
  }

  @throws[Exception]
  def putBreakingNews(json: JsValue): Boolean = {
    try {
      s3.putPublic(breakingNewskey, json.toString, "application/json")
      true
    } catch {
      case e: Exception =>
        log.error(s"Cannot write Breaking News json (${e.getMessage})")
        throw e
    }
  }

}

object BreakingNewsApi extends BreakingNewsApi {
  lazy val s3 = S3BreakingNews
}