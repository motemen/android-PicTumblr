import sbt._
import Process._

package sbt {

    object ProcessBuilderExtra {
        implicit def a2b (xml : scala.xml.Elem) : ProcessBuilderExtra
            = new ProcessBuilderExtra(Process(xml))
        implicit def a2b (p : ProcessBuilder) : ProcessBuilderExtra
            = new ProcessBuilderExtra(p)
    }

    class ProcessBuilderExtra (p : ProcessBuilder) {
        def #|! (other: ProcessBuilder): ProcessBuilder = {
            require(other.canPipeTo, "Piping to multiple processes is not supported.")
            new PipedProcessBuilder(p, other, true)
        }
    }

}

import sbt.ProcessBuilderExtra._

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

trait SuppressAaptWarnings extends AndroidProject {
    override def aaptPackageTask = execTask {
        <_>
            {aaptPath.absolutePath} package -f -M {androidManifestPath.absolutePath} -S {mainResPath.absolutePath}
            -A {mainAssetsPath.absolutePath} -I {androidJarPath.absolutePath} -F {resourcesApkPath.absolutePath}
        </_> #|! (
            "sed" :: "/skipping hidden file/d" :: Nil
        )
    } dependsOn directory (mainAssetsPath)

    override def aaptGenerateTask = execTask {
        <_>
          {aaptPath.absolutePath} package -m -M {androidManifestPath.absolutePath} -S {mainResPath.absolutePath}
          -I {androidJarPath.absolutePath} -J {mainJavaSourcePath.absolutePath}
        </_> #|! (
            "sed" :: "/skipping hidden file/d" :: Nil
        )
    } dependsOn directory (mainJavaSourcePath)
}

class PicTumblrProject (info: ProjectInfo) extends ParentProject(info) {
    override def shouldCheckOutputDirectories = false
    override def updateAction = task { None }

    lazy val main  = project(".",     "PicTumblr", new MainProject(_))
    lazy val tests = project("tests", "tests",     new TestProject(_), main)

    class MainProject(info: ProjectInfo) extends AndroidProject(info) with Defaults with MarketPublish with AutoRestartAdbDaemon with SuppressAaptWarnings {
        val keyalias  = "change-me"
        // val scalatest = "org.scalatest" % "scalatest" % "1.0" % "test"
    }

    class TestProject(info: ProjectInfo) extends AndroidTestProject(info) with Defaults with AutoRestartAdbDaemon with SuppressAaptWarnings {
        override def proguardInJars = runClasspath --- proguardExclude
        val scalatest = "org.scalatest" % "scalatest" % "1.0"
    }
}
