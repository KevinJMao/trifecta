package com.ldaniels528.trifecta.modules

import java.io.PrintStream

import com.ldaniels528.trifecta.TxResultHandler
import com.ldaniels528.trifecta.io.json.JsonHelper
import com.ldaniels528.trifecta.modules.MongoResultHandler._
import com.mongodb.casbah.Imports.{DBObject => Q, _}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._

/**
 * MongoDB Result Handler
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class MongoResultHandler() extends TxResultHandler {

  /**
   * Allows a user defined handler to process the given result
   * @param result the given result
   * @param out the given [[PrintStream]] for outputting information to the uswer
   * @return true, if the result was processed
   */
  override def handleResult(result: Any, out: PrintStream): Boolean = {
    result match {
      case dbo: Q =>
        out.println(pretty(render(documentToJson(dbo))))
        true

      case _ => false
    }
  }

}

/**
 * MongoDB Result Handler Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object MongoResultHandler {

  /**
   * Converts the given MongoDB document into a JSON value
   * @param result the given  MongoDB document
   * @return the resultant [[JValue]]
   */
  def documentToJson(result: Q): JValue = JsonHelper.toJson(result.toString)

  /**
   * Converts the given JSON value into a MongoDB document
   * @param js the given [[JValue]]
   * @return the resultant MongoDB document
   */
  def toDocument(js: JValue): Q = {
    js.values match {
      case m: Map[String, Any] => convertToMDB(m).asInstanceOf[Q]
      case x => throw new IllegalArgumentException(s"$x (${Option(x).map(_.getClass.getName)})")
    }
  }

  private def convertToMDB[T](input: T): Any = {
    input match {
      case m: Map[String, Any] =>
        m.foldLeft(Q()) { case (result, (key, value)) =>
          result ++ Q(key -> convertToMDB(value))
        }
      case x => x
    }
  }

}
