package detektobot.config

import cats.effect.{Async, Blocker, ContextShift, Resource}
import doobie.hikari.HikariTransactor

import scala.concurrent.ExecutionContext

final case class AdminConf(admins: List[Int], storage: String)
final case class HttpClientConf(opt: String)
final case class HttpConf(host: String, port: Int, path: String, timeout: Int, client: HttpClientConf)
final case class DbPoolConf(poolSize: Int)
final case class DbConf(driver: String, url: String, user: String, password: String, connections: DbPoolConf)
final case class ApiConfAuth(codeLen: Int, tokenLen: Int)
final case class ApiConf(limits: ApiConfLimits, auth: ApiConfAuth)
final case class ApiConfLimits(
  maxCodesHourIp: Int,
  maxCodesHourMsisdn: Int,
  minMinutesBetweenCodesMsisdn: Int,
)
final case class Conf(admin: AdminConf, db: DbConf, http: HttpConf, api: ApiConf)

object DbConf {

  def createTransactor[F[_] : Async : ContextShift](
    dbConf: DbConf,
    connEc: ExecutionContext,
    blocker: Blocker,
  ): Resource[F, HikariTransactor[F]] = HikariTransactor
    .newHikariTransactor[F](dbConf.driver, dbConf.url, dbConf.user, dbConf.password, connEc, blocker)
}