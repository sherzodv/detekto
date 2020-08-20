resolvers ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
)

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"             % "1.0.0")
