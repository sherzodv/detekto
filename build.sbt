enablePlugins(GitPlugin)

ThisBuild / name         := "DetektoBot"
ThisBuild / maintainer   := "Detekto Bot Team"
ThisBuild / organization := "io.github.sherzodv"

val axisBranch  = settingKey[String]("Build branch, one of: dev, test, prod")
val axisVersion = settingKey[String]("Build version")
val axisCommit  = settingKey[String]("Build commit")
val axisTstamp  = settingKey[String]("Build timestamp")

ThisBuild / axisTstamp  := Version.timestamp
ThisBuild / axisBranch  := Version.getBranch(git.gitCurrentBranch.value)
ThisBuild / axisCommit  := Version.getCommit(git.gitHeadCommit.value)
ThisBuild / axisVersion := Version.getVersion(
  git.gitDescribedVersion.value,
  git.gitUncommittedChanges.value,
  axisBranch.value,
  axisCommit.value,
)

ThisBuild / version      := axisVersion.value
ThisBuild / scalaVersion := Opts.Compiler.version
ThisBuild / logLevel     := Level.Info
ThisBuild / resolvers    := Deps.resolvers

lazy val `detektobot-core` = project
  .settings(Opts.core)
  .settings(libraryDependencies ++= Deps.core)

lazy val `detektobot-main` = project
  .enablePlugins(
    JavaAppPackaging,
  )
  .dependsOn(
    `detektobot-core`,
  )
  .settings(Opts.main)
  .settings(libraryDependencies ++= Deps.main)

lazy val DetektoBot = (project in file("."))
  .aggregate(
    `detektobot-core`,
    `detektobot-main`,
  )
  .settings(
    crossScalaVersions := Nil,
    publish := {}
  )
