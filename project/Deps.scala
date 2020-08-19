import sbt._

object Deps {

  val resolvers: Seq[Resolver] = Seq(
    Resolver.defaultLocal,
    Resolver.typesafeRepo("releases"),
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots"),
  )

  object V {
    val monix          = "3.2.2"
    val catsEffect     = "2.2.0-RC3"
    val fs2            = "2.4.2"
    val http4s         = "0.21.6"
    val circe          = "0.13.0"
    val circeConfig    = "0.8.0"
    val doobie         = "0.9.0"
    val logback        = "1.2.3"
    val logstage       = "0.10.16"
    val uaParser       = "5.19"
    val scalaCheck     = "1.14.3"
    val scalaTest      = "3.2.0"
    val scalaTestP     = "3.2.0.0"
    val tsec           = "0.2.1"
    val telegramium    = "2.49.0"
    val zxing          = "3.4.0"
  }

  object Lib {
    def fs2(artifact: String)      : ModuleID = "co.fs2"         %% ("fs2-" + artifact) % V.fs2
    def circe(artifact: String)    : ModuleID = "io.circe"       %% artifact % V.circe
    def http4s(artifact: String)   : ModuleID = "org.http4s"     %% artifact % V.http4s
    def doobie(artifact: String)   : ModuleID = "org.tpolecat"   %% ("doobie-" + artifact) % V.doobie
    def logstage(artifact: String) : ModuleID = "io.7mind.izumi" %% artifact % V.logstage
    def tsec(artifact: String)     : ModuleID = "io.github.jmcardon" %% ("tsec-" + artifact) % V.tsec

    val catsEffect   = "org.typelevel" %% "cats-effect" % V.catsEffect
    val fs2Core      = fs2("core")
    val fs2IO        = fs2("io")
    val monix        = "io.monix" %% "monix" % V.monix

    val http4sServer = http4s("http4s-blaze-server")
    val http4sDsl    = http4s("http4s-dsl")
    val http4sClient = http4s("http4s-blaze-client")
    val http4sCirce  = http4s("http4s-circe")

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeGExtras = circe("circe-generic-extras")
    val circeParser  = circe("circe-parser")
    val circeConfig  = "io.circe" %% "circe-config" % V.circeConfig

    val doobieCore   = doobie("core")
    val doobieQuill  = doobie("quill")
    val doobiePg     = doobie("postgres")
    val doobieHikari = doobie("hikari")

    val logstageCore      = logstage("logstage-core")
    val logstageSlf4jSink = logstage("logstage-sink-slf4j")
    val logback           = "ch.qos.logback" % "logback-classic" % V.logback

    val uaParser = "nl.basjes.parse.useragent" % "yauaa" % V.uaParser

    val scalaCheck = "org.scalacheck" %% "scalacheck" % V.scalaCheck % Test
    val scalaTest  = "org.scalatest" %% "scalatest" % V.scalaTest % Test
    val scalaTestP = "org.scalatestplus" %% "scalacheck-1-14" % V.scalaTestP % Test

    val tsecCommon     = tsec("common")
    val tsecPassword   = tsec("password")
    val tsecMac        = tsec("mac")
    val tsecSignatures = tsec("signatures")
    val tsecJwtMac     = tsec("jwt-mac")
    val tsecJwtSig     = tsec("jwt-sig")
    val tsecHttp4s     = tsec("http4s")

    val telegramiumCore = "io.github.apimorphism" %% "telegramium-core" % V.telegramium
    val telegramiumHigh = "io.github.apimorphism" %% "telegramium-high" % V.telegramium

    val zxingCore   = "com.google.zxing" % "core" % V.zxing
    val zxingJavaSe = "com.google.zxing" % "javase" % V.zxing
  }

  lazy val core = Seq(
    Lib.monix,
    Lib.catsEffect,
    Lib.fs2Core,
    Lib.fs2IO,
    Lib.http4sServer,
    Lib.http4sDsl,
    Lib.http4sClient,
    Lib.http4sCirce,
    Lib.circeCore,
    Lib.circeParser,
    Lib.circeConfig,
    Lib.circeGeneric,
    Lib.circeGExtras,
    Lib.doobieCore,
    Lib.doobiePg,
    Lib.doobieHikari,
    Lib.logback,
    Lib.logstageCore,
    Lib.logstageSlf4jSink,
    Lib.uaParser,

    Lib.tsecCommon,
    Lib.tsecPassword,
    Lib.tsecMac,
    Lib.tsecSignatures,
    Lib.tsecJwtMac,
    Lib.tsecJwtSig,
    Lib.tsecHttp4s,

    Lib.telegramiumCore,
    Lib.telegramiumHigh,

    Lib.zxingCore,
    Lib.zxingJavaSe,

    Lib.scalaCheck,
    Lib.scalaTest,
    Lib.scalaTestP,
  )

  lazy val main = core ++ Seq(
  )
}
