package com.ldaniels528.trifecta

import com.ldaniels528.trifecta.io.{AsyncIO, InputSource, OutputSource}
import com.ldaniels528.trifecta.messages.MessageDecoder
import com.ldaniels528.trifecta.messages.query.BigDataQuery
import com.ldaniels528.trifecta.modules.ModuleManager

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Trifecta Runtime Context
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait TxRuntimeContext {

  /**
   * Returns the configuration
   * @return the configuration
   */
  def config: TxConfig

  /**
   * Returns the module manager
   * @return the module manager
   */
  def moduleManager: ModuleManager

  /**
   * Attempts to resolve the given topic or decoder URL into an actual message decoder
   * @param topicOrUrl the given topic or decoder URL
   * @return an option of a [[MessageDecoder]]
   */
  def resolveDecoder(topicOrUrl: String): Option[MessageDecoder[_]]

  /**
   * Returns the input handler for the given output URL
   * @param url the given input URL (e.g. "es:/quotes/quote/GDF")
   * @return an option of an [[InputSource]]
   */
  def getInputHandler(url: String): Option[InputSource]

  /**
   * Returns the output handler for the given output URL
   * @param url the given output URL (e.g. "es:/quotes/$exchange/$symbol")
   * @return an option of an [[OutputSource]]
   */
  def getOutputHandler(url: String): Option[OutputSource]

  def handleResult(result: Any, input: String)(implicit ec: ExecutionContext)

  def interpret(input: String): Try[Any]

  /**
   * Attempts to retrieve a message decoder by name
   * @param name the name of the desired [[MessageDecoder]]
   * @return an option of a [[MessageDecoder]]
   */
  def lookupDecoderByName(name: String): Option[MessageDecoder[_]]

  /**
   * Registers a message decoder, which can be later retrieved by name
   * @param name the name of the [[MessageDecoder]]
   * @param decoder the [[MessageDecoder]] instance
   */
  def registerDecoder(name: String, decoder: MessageDecoder[_]): Unit

  def shutdown(): Unit

  /**
   * Executes the given query
   * @param query the given [[BigDataQuery]]
   */
  def executeQuery(query: BigDataQuery)(implicit ec: ExecutionContext): AsyncIO

}
