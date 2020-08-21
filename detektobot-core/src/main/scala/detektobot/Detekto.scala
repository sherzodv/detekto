package detektobot

import detektobot.bot.Bot
import detektobot.config.{Conf, HttpClientConf}
import cats.syntax.apply._
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Resource, Timer}
import detektobot.db.Repo
import org.http4s.client.Client
import logstage.LogIO
import logstage.LogIO.log
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.high.{Api, BotApi, LongPollBot}

import scala.concurrent.ExecutionContext

object Detekto {

  abstract class Service[F[_]] {
    def start(): F[Unit]
  }

  def createService[F[_]: LogIO: Timer: ConcurrentEffect: ContextShift](
    conf: Conf,
    ec: ExecutionContext,
    dmCodeRepo: Repo.Service[F],
  ): Resource[F, Detekto.Service[F]] = {
    for {
      http <- BlazeClientBuilder[F](ec).resource
      srv  <- create(http, conf, ec, dmCodeRepo)
    } yield srv
  }

  private def create[F[_]: Timer: ConcurrentEffect : ContextShift: LogIO](
    http: Client[F],
    conf: Conf,
    ec: ExecutionContext,
    dmCodeRepo: Repo.Service[F],
  ): Resource[F, Detekto.Service[F]] = {
    val blocker = Blocker.liftExecutionContext(ec)
    val httpWithLog = Logger(logBody = false, logHeaders = true)(http)
    val token = conf.bot.token
    implicit val api: BotApi[F] = BotApi(httpWithLog, baseUrl = s"https://api.telegram.org/bot$token", blocker)
    for {
      bot <- createBot(httpWithLog, conf, dmCodeRepo)
    } yield new Service[F] {
      override def start(): F[Unit] = {
        log.info(s"Starting") *>
          bot.start()
      }
    }
  }

  private def createBot[F[_]: Api: Timer: ConcurrentEffect : LogIO](
    http: Client[F],
    conf: Conf,
    dmCodeRepo: Repo.Service[F],
  ): Resource[F, LongPollBot[F]] = {
    val F = implicitly[ConcurrentEffect[F]]
    Resource.make(F.delay(new Bot[F](http, conf, dmCodeRepo)))(_ => F.unit)
  }

}