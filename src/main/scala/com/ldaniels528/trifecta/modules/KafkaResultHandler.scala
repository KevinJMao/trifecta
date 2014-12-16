package com.ldaniels528.trifecta.modules

import java.io.PrintStream

import com.ldaniels528.tabular.Tabular
import com.ldaniels528.trifecta.io.avro.AvroTables
import com.ldaniels528.trifecta.io.kafka.KafkaMicroConsumer.MessageData
import com.ldaniels528.trifecta.io.kafka.StreamedMessage
import com.ldaniels528.trifecta.messages.BinaryMessaging
import com.ldaniels528.trifecta.messages.query.KQLResult
import com.ldaniels528.trifecta.{TxConfig, TxResultHandler}
import net.liftweb.json.JsonAST.JValue
import net.liftweb.json._
import org.apache.avro.generic.GenericRecord

import scala.util.{Failure, Success, Try}

/**
 * Kafka Result Handler
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class KafkaResultHandler(config: TxConfig) extends TxResultHandler with BinaryMessaging {
  private val tabular = new Tabular() with AvroTables

  /**
   * Allows a user defined handler to process the given result
   * @param result the given result
   * @param out the given [[PrintStream]] for outputting information to the user
   * @return true, if the result was processed
   */
  override def handleResult(result: Any, out: PrintStream): Boolean = {
    result match {
      // handle Avro records
      case g: GenericRecord =>
        Try(toJson(g)) match {
          case Success(js) => out.println(pretty(render(js)))
          case Failure(e) => out.println(g)
        }
        true

      case MessageData(_, offset, _, _, _, message) =>
        dumpMessage(offset, message)(config)
        true

      case StreamedMessage(_, _, offset, _, message) =>
        dumpMessage(offset, message)(config)
        true

      case KQLResult(topic, fields, values, runTimeMillis) =>
        if (values.isEmpty) out.println("No data returned")
        else {
          out.println(f"[Query completed in $runTimeMillis%.1f msec]")
          tabular.transform(fields, values) foreach out.println
        }
        true

      case _ => false
    }
  }


  /**
   * Converts the given record into a JSON value
   * @param record the given [[GenericRecord]]
   * @return the resultant [[JValue]]
   */
  private def toJson(record: GenericRecord): JValue = parse(record.toString)

}
