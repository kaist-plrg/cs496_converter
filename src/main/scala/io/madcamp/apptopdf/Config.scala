package io.madcamp.apptopdf

import play.api.libs.json._

import scala.io.Source

case class Config(
  credentialFile: String,
  applicationFileId: String,
  previousFileId: String,
  summaryFileId: String,
  typeface: String,
  out: String,
)

object Config {
  implicit val reads: Reads[Config] = Json.reads[Config]
  def fromFile(name: String): Config = {
    val str = Source.fromFile(name, "UTF-8").mkString
    val jsonVal = Json.parse(str)
    Json.fromJson[Config](jsonVal) match {
      case JsSuccess(c, _) => c
      case e: JsError => throw new Exception(JsError.toJson(e).toString)
    }
  }
}
