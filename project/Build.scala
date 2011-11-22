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
        AndroidInstall.settings ++ Seq(
            keyalias in Android := "motemen",
            useProguard in Android := true
        )

    val androidTestProjectSettings =
        androidSettings ++
        AndroidTest.androidSettings ++ Seq(
            libraryDependencies ++= Seq(
                "org.scalatest" % "scalatest" % "1.2"
            )
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
