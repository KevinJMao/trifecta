package com.ldaniels528.trifecta.modules

import java.io.{File, FileInputStream}
import java.util.Properties

import com.ldaniels528.trifecta.command.Command
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.util.ResourceHelper._
import com.ldaniels528.trifecta.{TxConfig, TxRuntimeContext}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

/**
 * Module Manager
 * @author Lawrence Daniels <lawrence.daniels@gmail.com>
 */
class ModuleManager()(implicit rt: TxRuntimeContext) {
  private var commands = Map[String, Command]()
  private var moduleSet = Set[Module]()
  private var currentModule: Option[Module] = None

  /**
   * Adds a module to this manager
   * @param module the given module
   * @return the module manager instance
   */
  def +=(module: Module) = {
    moduleSet += module

    // reset the commands & variables collections
    updateCollections()
    this
  }

  /**
   * Adds a module to this manager
   * @param modules the given collection of module
   * @return the module manager instance
   */
  def ++=(modules: Seq[Module]) = {
    moduleSet ++= modules

    // reset the commands & variables collections
    updateCollections()
    this
  }

  def activeModule: Option[Module] = {
    currentModule ?? moduleSet.find(_.moduleName == "zookeeper") ?? moduleSet.headOption
  }

  def activeModule_=(module: Module) = currentModule = Option(module)

  /**
   * Returns a mapping of commands
   * @return a mapping of command name to command instance
   */
  def commandSet = commands

  /**
   * Returns a collection of modules
   * @return a collection of modules
   */
  def modules: Seq[Module] = moduleSet.toSeq

  /**
   * Retrieves a command by name
   * @param name the name of the desired module
   * @return an option of a command
   */
  def findCommandByName(name: String): Option[Command] = commandSet.get(name.toLowerCase)

  /**
   * Retrieves a module by name
   * @param name the name of the desired module
   * @return an option of a module
   */
  def findModuleByName(name: String): Option[Module] = moduleSet.find(_.moduleName == name)

  /**
   * Retrieves a module by prefix
   * @param prefix the prefix of the desired module
   * @return an option of a module
   */
  def findModuleByPrefix(prefix: String): Option[Module] = moduleSet.find(_.supportedPrefixes.contains(prefix))

  /**
   * Sets the active module
   * @param module the given module
   */
  def setActiveModule(module: Module): Unit = currentModule = Option(module)

  /**
   * Shuts down all modules
   */
  def shutdown(): Unit = moduleSet.foreach(_.shutdown())

  private def updateCollections(): Unit = {
    // update the command collection
    commands = Map(moduleSet.toSeq flatMap (_.getCommands map (c => (c.name, c))): _*)
  }

}

object ModuleManager {
  private val logger = LoggerFactory.getLogger(getClass)

  def loadExternalModules(config: TxConfig, moduleConfigFile: File): List[Module] = {
    if (!moduleConfigFile.exists) {
      logger.info(s"Module file '${moduleConfigFile.getAbsolutePath}' not found. No external modules loaded.")
      Nil
    }
    else Try {
      new FileInputStream(moduleConfigFile) use { in =>
        val props = new Properties()
        props.load(in)

        props.flatMap { case (label, className) =>
          logger.info(s"Loading module '$label' (class '$className')")
          Try(Class.forName(className.trim)) match {
            case Success(moduleClass) => Option(instantiateModule(config, moduleClass))
            case Failure(e: ClassNotFoundException) =>
              logger.error(s"Module class '$className' not found")
              None
            case Failure(e) =>
              logger.error(s"Failed to load module class '$className'", e)
              None
          }
        }
      }
    } match {
      case Success(modules) => modules.toList
      case Failure(e) =>
        logger.error(s"Failed to load external modules", e)
        Nil
    }
  }

  private def instantiateModule[T](config: TxConfig, moduleClass: Class[T]): Module = {
    val cons = moduleClass.getConstructor(classOf[TxConfig])
    cons.newInstance(config).asInstanceOf[Module]
  }

}