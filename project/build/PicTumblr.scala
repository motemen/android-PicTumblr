import sbt._
import Process._

trait Defaults {
  def androidPlatformName = "android-7"
}

class PicTumblr(info: ProjectInfo) extends ParentProject(info) {
  override def shouldCheckOutputDirectories = false
  override def updateAction = task { None }

  lazy val main  = project(".",     "PicTumblr", new MainProject(_))
  lazy val tests = project("tests", "tests",     new TestProject(_), main)

  class MainProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish {
    val keyalias  = "change-me"
    // val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"

    override def adbTask (emulator : Boolean, action : String) = {
        restartAdbServer(emulator).run
        super.adbTask(emulator, action)
    }

    def restartAdbServer (emulator : Boolean) = {
      if (emulator) {
        execTask {
          (adbPath.absolutePath + " devices") #| "grep emulator" #|| {
            <x> {adbPath.absolutePath} kill-server </x> #&& <x> {adbPath.absolutePath} devices </x>
          }
        }
      } else {
        task { None }
      }
    }
  }

  class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults {
    override def proguardInJars = runClasspath --- proguardExclude
    val scalatest = "org.scalatest" % "scalatest" % "1.0"
  }
}
