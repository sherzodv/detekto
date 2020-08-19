resolvers ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
)

addSbtPlugin("com.eed3si9n"     % "sbt-assembly"    % "0.14.10")
addSbtPlugin("io.spray"         % "sbt-revolver"    % "0.9.1")
addSbtPlugin("com.timushev.sbt" % "sbt-updates"     % "0.5.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-git"         % "1.0.0")
