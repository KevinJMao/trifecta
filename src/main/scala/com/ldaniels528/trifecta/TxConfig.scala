package com.ldaniels528.trifecta

import java.io.File._
import java.io.{File, FileInputStream, FileOutputStream}
import java.util.Properties

import com.ldaniels528.trifecta.util.PropertiesHelper._
import com.ldaniels528.trifecta.util.ResourceHelper._

import scala.util.Properties._
import scala.util.Try

/**
 * Trifecta Configuration
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class TxConfig(val configProps: Properties) {
  // set the current working directory
  configProps.setProperty("trifecta.common.cwd", new File(".").getCanonicalPath)

  // the default state of the application is "alive"
  var alive = true

  // capture standard output
  val out = System.out
  val err = System.err

  // define the job manager
  val jobManager = new JobManager()

  // Zookeeper connection string
  def zooKeeperConnect = configProps.getOrElse("trifecta.zookeeper.host", "127.0.0.1:2181")

  def kafkaZkConnect = configProps.getOrElse("trifecta.kafka.zookeeper.host", zooKeeperConnect)

  // various shared state variables
  def autoSwitching: Boolean = configProps.getOrElse("trifecta.common.autoSwitching", "true").toBoolean

  def autoSwitching_=(enabled: Boolean) = configProps.setProperty("trifecta.common.autoSwitching", enabled.toString)

  // the number of columns to display when displaying bytes
  def columns: Int = configProps.getOrElse("trifecta.common.columns", "25").toInt

  def columns_=(width: Int): Unit = configProps.setProperty("trifecta.common.columns", width.toString)

  def cwd: String = configProps.getProperty("trifecta.common.cwd")

  def cwd_=(path: String) = configProps.setProperty("trifecta.common.cwd", path)

  def debugOn: Boolean = configProps.getOrElse("trifecta.common.debugOn", "false").toBoolean

  def debugOn_=(enabled: Boolean): Unit = configProps.setProperty("trifecta.common.debugOn", enabled.toString)

  def encoding: String = configProps.getOrElse("trifecta.common.encoding", "UTF-8")

  def encoding_=(charSet: String): Unit = configProps.setProperty("trifecta.common.encoding", charSet)

  /**
   * Returns an option of the value corresponding to the given key
   * @param key the given key
   * @return an option of the value
   */
  def get(key: String): Option[String] = Option(configProps.getProperty(key))

  /**
   * Retrieves the value of the key or the default value
   * @param key the given key
   * @param default the given value
   * @return the value of the key or the default value
   */
  def getOrElse(key: String, default: => String): String = configProps.getOrElse(key, default)

  /**
   * Retrieves either the value of the key or the default value
   * @param key the given key
   * @param default the given value
   * @return either the value of the key or the default value
   */
  def getEither[T](key: String, default: => T): Either[String, T] = {
    Option(configProps.getProperty(key)) match {
      case Some(value) => Left(value)
      case None => Right(default)
    }
  }

  /**
   * Sets the value for the given key
   * @param key the given key
   * @param value the given value
   * @return an option of the previous value for the key
   */
  def set(key: String, value: String): Option[AnyRef] = Option(configProps.setProperty(key, value))

  /**
   * Saves the current configuration to disk
   * @param configFile the configuration file
   */
  def save(configFile: File): Unit = {
    Try {
      new FileOutputStream(configFile) use { fos =>
        configProps.store(fos, "Trifecta configuration properties")
      }
    }
    ()
  }

}

/**
 * Trifecta Configuration Singleton
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
object TxConfig {

  // define the history properties
  var historyFile: File = new File(s"$userHome$separator.trifecta${separator}history.txt")

  // define the configuration file & properties
  var configFile: File = new File(s"$userHome$separator.trifecta${separator}config.properties")

  /**
   * Returns the default configuration
   * @return the default configuration
   */
  def defaultConfig: TxConfig = new TxConfig(getDefaultProperties)

  /**
   * Loads the configuration file
   */
  def load(configFile: File): TxConfig = {
    val p = getDefaultProperties
    if (configFile.exists()) new FileInputStream(configFile) use (in => Try(p.load(in)))
    else new FileOutputStream(configFile) use (out => Try(p.store(out, "Trifecta configuration file")))
    new TxConfig(p)
  }

  private def getDefaultProperties: java.util.Properties = {
    Map(
      "trifecta.zookeeper.host" -> "localhost:2181",
      "trifecta.kafka.zookeeper.host" -> "localhost:2181",
      "trifecta.elasticsearch.hosts" -> "localhost",
      "trifecta.cassandra.hosts" -> "localhost ",
      "trifecta.storm.hosts" -> "localhost",
      "trifecta.common.autoSwitching" -> "true",
      "trifecta.common.columns" -> "25",
      "trifecta.common.debugOn" -> "false",
      "trifecta.common.encoding" -> "UTF-8").toProps
  }

}

