package detektobot.db

import cats.effect.{ConcurrentEffect, Resource}
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import detektobot.adt.{Block, CommandReq, DmCodeCheckReq, Pack}
import detektobot.config.Conf
import doobie.Transactor
import doobie.implicits._
import logstage.LogIO

object Repo {

  trait Service[F[_]] {
    def checkDmCode(req: DmCodeCheckReq): F[Option[String]]
    def registerCmd(req: CommandReq): F[Unit]
    def registerPack(pack: Pack): F[Int]
  }

  def createRepo[F[_] : ConcurrentEffect : LogIO](tx: Transactor[F], conf: Conf): Resource[F, Service[F]] =
    Resource.make(ConcurrentEffect[F].delay(create(tx, conf)))(_ => ConcurrentEffect[F].delay(()))

  private
  def create[F[_] : ConcurrentEffect : LogIO](tx: Transactor[F], conf: Conf): Service[F] = new Service[F] {

    private val F = implicitly[ConcurrentEffect[F]]

    def checkDmCode(req: DmCodeCheckReq): F[Option[String]] = {
      insertCodeCheckReq(req).run.transact(tx) *>
        req.code.fold(F.pure[Option[String]](None))(code => {
          if (code.length == 41) {
            selectPackByBlockCode(code).option.transact(tx)
          } else {
            selectPackByCode(code).option.transact(tx)
          }
        })
    }

    def registerCmd(req: CommandReq): F[Unit] = {
      for {
        _ <- insertCommandReq(req).run.transact(tx)
      } yield ()
    }

    def registerPack(pack: Pack): F[Int] = {
      insertPack(pack).run.transact(tx) *>
        pack.blocks.traverse(insertBlock(pack, _).run.transact(tx))
          .map(_ => pack.blocks.size)
    }

  }

  private
  def insertCodeCheckReq(req: DmCodeCheckReq) =
    fr"""
    insert into dm_code_check_req(user_id, user_name, first_name, last_name, text, file_id, file_unique_id, code)
    values(
      ${req.userId},
      ${req.userName},
      ${req.userFirstName},
      ${req.userLastName},
      ${req.text},
      ${req.fileId},
      ${req.fileUniqueId},
      ${req.code}
    )
  """.update

  private
  def insertCommandReq(req: CommandReq) =
    fr"""
    insert into command_req(user_id, user_name, first_name, last_name, text)
    values(
      ${req.userId},
      ${req.userName},
      ${req.userFirstName},
      ${req.userLastName},
      ${req.text}
    )
  """.update

  private
  def insertPack(pack: Pack) =
    fr"""
    insert into pack(code)
    values(${pack.code})
  """.update

  private
  def insertBlock(pack: Pack, block: Block) =
    fr"""
    insert into block(code, pack_code)
    values(${block.code}, ${pack.code})
  """.update

  private
  def selectPackByCode(code: String) =
    fr"""
    select code from pack where code = $code
  """.query[String]

  private
  def selectPackByBlockCode(blockCode: String) =
    fr"""
    select pack_code from block where code = $blockCode
  """.query[String]

}