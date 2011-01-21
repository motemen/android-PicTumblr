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
    def androidPlatformName = "android-9"
}

// XXX これでいいのかよ
trait FixToolsPathLv9 extends AndroidProject {
    override def aaptPath = androidToolsPath / aaptName
    override def aidlPath = androidToolsPath / aidlName
    override def dxPath   = androidSdkPath / "platform-tools" / dxName
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

    class MainProject(info: ProjectInfo) extends AndroidProject(info)
            with Defaults with MarketPublish with TypedResources
            with AutoRestartAdbDaemon with SuppressAaptWarnings with FixToolsPathLv9
    {
        override def proguardOption = (
            "-dontnote scala.Enumeration" ::
            Nil
        ) mkString(" ")

        val keyalias  = "change-me"
        val commonsLang = "commons-lang" % "commons-lang" % "2.5"
    }

    class TestProject(info: ProjectInfo) extends AndroidTestProject(info)
            with Defaults with AutoRestartAdbDaemon with SuppressAaptWarnings with FixToolsPathLv9
    {
        override def proguardInJars = runClasspath --- proguardExclude
        override def proguardOption = (
            "-dontnote scala.Enumeration" ::
            "-dontnote scala.runtime.AbstractFunction1" ::
            "-dontnote net.tokyoenvious.droid.pictumblr.tests.*" ::
            Nil
        ) mkString(" ")

        val scalatest = "org.scalatest" % "scalatest" % "1.2"
    }
}
