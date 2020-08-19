import sbt._
import Keys._

object Opts {

  object Compiler {
    lazy val version = "2.13.3"

    lazy val settings = Seq(
      crossScalaVersions := Seq(version),
      scalacOptions ++= options,
      addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
    )

    lazy val options = Seq(
      "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
      "-explaintypes",                     // Explain type errors in more detail.
      "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
      "-language:higherKinds",             // Allow higher-kinded types
      "-language:implicitConversions",     // Allow definition of implicit functions called views
      "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    )
  }

  object Jvm {
    lazy val settings = Seq(
    )
  }

  val common = Compiler.settings ++ Jvm.settings

  val core = common
  val main = common

}
