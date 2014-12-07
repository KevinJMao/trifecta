package com.ldaniels528.trifecta

import java.io.PrintStream

/**
 * Trifecta Result Handler
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
trait TxResultHandler {

  /**
   * Allows a user defined handler to process the given result
   * @param result the given result
   * @param out the given [[PrintStream]] for outputting information to the uswer
   * @return true, if the result was processed
   */
  def handleResult(result: Any, out: PrintStream): Boolean

}
