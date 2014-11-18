package com.ldaniels528.trifecta.modules

import java.io.{File, FilenameFilter}

import com.ldaniels528.trifecta.command.Command
import com.ldaniels528.trifecta.util.OptionHelper._
import com.ldaniels528.trifecta.{TxConfig, TxRuntimeContext}
import org.clapper.classutil.{ClassFinder, ClassInfo}

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

  private val moduleClassName = classOf[Module].getName

  def loadExternalModules(config: TxConfig): Option[Iterator[ClassInfo]] = {
    for {
      homePath <- Option(System.getenv("TRIFECTA_HOME"))
      homeDir = new File(s"$homePath/lib")
      jarFiles <- Option(homeDir.listFiles(new FilenameFilter {
        override def accept(dir: File, name: String): Boolean = name.endsWith(".jar")
      })) if homeDir.exists()
      _ = jarFiles.foreach(s => println(s"jar: $s"))
      finder = ClassFinder(jarFiles) if jarFiles.nonEmpty
      classInfo = finder.getClasses().filter(c => c.isConcrete && c.implements(moduleClassName))
      _ = classInfo.foreach(s => println(s"modules: $s"))
     // classes = ClassFinder.concreteSubclasses(moduleClassName, classInfo)
    //instances = classes map { ci =>  ci.}

    } yield classInfo
  }

}