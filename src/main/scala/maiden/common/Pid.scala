package maiden.common

import java.lang.management.ManagementFactory
import java.io._
import java.nio.file.{Paths, Files}
import scala.util.Properties


//handy wrapper to deal with PIDs
class Pid(name: String) {


  private[this] val pidPath = Properties.envOrElse("MAIDEN_PID_DIR", s"./pids")
  private[this] val pidFile: String = s"$pidPath/$name.pid"

  //create pid directory if it doesn't exist
  Files.createDirectories(Paths.get(pidPath))


  def getPid() = {
    val vmName = ManagementFactory.getRuntimeMXBean().getName()
    val p = vmName.indexOf("@")
    vmName.substring(0, p)
  }

  def pidFileExists() =
    Files.exists(Paths.get(pidFile))

  def removePidFile() =
    Files.deleteIfExists(Paths.get(pidFile))

  def writePid() = {
    val pid = getPid()
    if (!pidFileExists()) {
      val file = new File(pidFile)
      val pw = new PrintWriter(file)
      pw.write(pid)
      pw.close
    } else {
      throw(new Exception(s"PID file exists at $pidFile... is $name already running?"))
    }
  }

}
