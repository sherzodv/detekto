package detekto

import cats.effect.Blocker
import monix.eval.Task
import monix.execution.Scheduler
import monix.execution.Scheduler.Implicits.global
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.middleware.Logger
import telegramium.bots.high.{Api, BotApi}

import scala.concurrent.duration.Duration

object Application extends App {

  val token: String = System.getenv("TOKEN")
  val baseUrl: String = s"https://api.telegram.org/bot$token"

  BlazeClientBuilder[Task](global).resource.use { httpClient =>
    val blocker = Blocker.liftExecutionContext(Scheduler.io())
    val http = Logger(logBody = true, logHeaders = true)(httpClient)
    implicit val api: Api[Task] = BotApi(http, baseUrl = baseUrl, blocker)
    val echoBot = new EchoBot(httpClient, token)
    echoBot.start()
  }.runSyncUnsafe(Duration.Inf)

}
