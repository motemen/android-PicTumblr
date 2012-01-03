import sbt._
import Keys._
import AndroidKeys._

object Common {
    val androidSettings =
        Defaults.defaultSettings ++ 
        AndroidProject.androidSettings ++ Seq(
            platformName := "android-11",
            versionCode := 4
        )

    val androidFullProjectSettings =
        androidSettings ++
        TypedResources.settings ++
        AndroidMarketPublish.settings ++
        AndroidManifestGenerator.settings ++
        AndroidInstall.settings ++
        Seq(
            keyalias in Android := "motemen",
            useProguard in Android := false /* true */
        ) ++
        Seq(
            startEmulator <<= (startEmulator in Android) dependsOn (installEmulator in Android) dependsOn (packageDebug in Android),
            installDevice <<= (installDevice in Android) dependsOn (packageDebug in Android)
        )

    val androidTestProjectSettings =
        androidSettings ++
        AndroidTest.settings ++
        // AndroidTest.androidSettings ++
        Seq(
            useProguard in Android := false /* true */,
            proguardOption in Android := (
                "-keep public class * implements org.scalatest.junit.ShouldMatchersForJUnit" ::
                "-keep public class org.scalatest.**" ::
                "-keep public class org.scalatest.junit.**" ::
                Nil
            ) mkString ("\n")
        ) ++
        Seq(
            TaskKey[Unit]("suite", "compiles, installs and runs tests in emulator") <<= (testEmulator in Android) dependsOn (installEmulator in Android) dependsOn (packageDebug in Android)
        )
}

object PicTumblrBuild extends Build {
    lazy val app = Project(
        id = "app",
        base = file("."),
        settings = Common.androidFullProjectSettings
    )

    lazy val tests = Project(
        id = "tests",
        base = file("tests"),
        settings = Common.androidTestProjectSettings
    ) dependsOn app
}
