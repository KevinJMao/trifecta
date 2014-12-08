package com.ldaniels528.trifecta.io.json

import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._

import scala.util.{Failure, Success, Try}

/**
 * JSON Helper Utility
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object JsonHelper {
  implicit val formats = DefaultFormats

  def makePretty(jsonString: String): String = {
    Try(toJson(jsonString)) match {
      case Success(js) => pretty(render(js))
      case Failure(e) => jsonString
    }
  }

  /**
   * Converts the given string into a JSON value
   * @param jsonString the given JSON string
   * @return the resultant [[JValue]]
   */
  def toJson(jsonString: String): JValue = parse(jsonString)

  def toJson[T](results: Seq[T]): JValue = Extraction.decompose(results)

  def makeCompact[T](results: Seq[T]): String = compact(render(Extraction.decompose(results)))

}
