import sbt._
import Process._

trait Defaults {
  def androidPlatformName = "android-7"
}

trait AutoRestartAdbDaemon extends AndroidProject {

    override def adbTask (emulator : Boolean, action : String) = dynamic {
        restartAdbDaemon(emulator)
        super.adbTask(emulator, action)
    }

    def restartAdbDaemon (emulator : Boolean) {
        val deviceFlag = if (emulator) "-e" else "-d"
        if (! (<_> {adbPath.absolutePath} {deviceFlag} get-state </_>.lines_! exists { _ == "device" })) {
            ( <_> {adbPath.absolutePath} kill-server </_> ## <_> {adbPath.absolutePath} start-server </_> ) !
        }
    }
}

class PicTumblr(info: ProjectInfo) extends ParentProject(info) {
  override def shouldCheckOutputDirectories = false
  override def updateAction = task { None }

  lazy val main  = project(".",     "PicTumblr - app",   new MainProject(_))
  lazy val tests = project("tests", "PicTumblr - tests", new TestProject(_), main)

  class MainProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish with AutoRestartAdbDaemon {
    val keyalias  = "change-me"
    // val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"
  }

  class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults with AutoRestartAdbDaemon {
    override def proguardInJars = runClasspath --- proguardExclude
    val scalatest = "org.scalatest" % "scalatest" % "1.0"
  }
}
