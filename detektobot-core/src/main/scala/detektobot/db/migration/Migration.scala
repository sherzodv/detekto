package detektobot.db.migration

import cats.effect.Bracket
import doobie.{LogHandler, Transactor}
import doobie.implicits._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import cats.syntax.applicativeError._

object Migration {

  private implicit val logHandler = LogHandler.jdkLogHandler

  def migrate[F[_] : Bracket[?[_], Throwable]](tx: Transactor[F]): F[Unit] = getLastKey(tx)
    .handleErrorWith(_ => createMig(tx).flatMap(_ => getLastKey(tx)))
    .flatMap(lastKey => applyDiff(lastKey.getOrElse(""), tx))

  private def applyDiff[F[_] : Bracket[?[_], Throwable]](lastKey: String, tx: Transactor[F]) = steps
    .diff(lastKey)
    .sequence
    .transact(tx)
    .void

  private def getLastKey[F[_] : Bracket[?[_], Throwable]](tx: Transactor[F]) = sql"select max(mkey) as mkey from migration"
    .query[Option[String]]
    .unique
    .transact(tx)

  private def createMig[F[_] : Bracket[?[_], Throwable]](tx: Transactor[F]) =
    sql"""
      create table migration(
        mkey varchar primary key,
        applied_at timestamp without time zone default (now() at time zone 'utc')
      )
    """
      .update
      .run
      .transact(tx)
      .void

}
