package detektobot

import detektobot.config.{Conf, DbConf}
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import detektobot.Application.token
import detektobot.db.Repo
import detektobot.db.migration.Migration
import doobie.ExecutionContexts
import logstage.{IzLogger, LogIO}
import io.circe.config.parser

object Yay {

  def createBot[F[_]: ContextShift: ConcurrentEffect: LogIO: Timer]: Resource[F, Detekto.Service[F]] = {
    for {
      conf <- Resource.liftF(parser.decodePathF[F, Conf]("detekto"))
      _ = println(conf.admin)
      httpCp <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      tranEc <- ExecutionContexts.cachedThreadPool[F]
      tx <- DbConf.createTransactor(conf.db, connEc, Blocker.liftExecutionContext(tranEc))
      _ <- Resource.liftF(Migration.migrate(tx))
      dmCodeRepo <- Repo.createRepo(tx, conf)
      bot <- Detekto.createService(conf, httpCp, token, dmCodeRepo)
    } yield bot
  }

}

object Application extends IOApp {

  val token: String = System.getenv("TOKEN")
  val baseUrl: String = s"https://api.telegram.org/bot$token"

  val logger = IzLogger()

  def run(args: List[String]): IO[ExitCode] = {
    implicit val log: LogIO[IO] = LogIO.fromLogger[IO](logger)
    Yay.createBot[IO]
      .use(bot => bot.start())
      .as(ExitCode.Success)
  }

}