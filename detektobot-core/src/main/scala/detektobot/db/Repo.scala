package detektobot.db

import cats.effect.{ConcurrentEffect, Resource}
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.traverse._
import cats.instances.list._
import detektobot.adt.{Block, CommandReq, DmCodeCheckReq, FoundCode, Pack, StatByUser, TotalStat}
import detektobot.config.Conf
import doobie.{Query0, Transactor}
import doobie.implicits._
import logstage.LogIO

object Repo {

  trait Service[F[_]] {
    def checkDmCode(req: DmCodeCheckReq): F[Option[String]]
    def registerCmd(req: CommandReq): F[Unit]
    def registerPack(pack: Pack): F[Int]
    def statByUsers(): F[List[StatByUser]]
    def totalStat(): F[TotalStat]
    def foundBlocks(): F[List[FoundCode]]
    def foundPacks(): F[List[FoundCode]]
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

    def statByUsers(): F[List[StatByUser]] = {
      statByUsersSql
        .to[List]
        .transact(tx)
    }

    def totalStat(): F[TotalStat] = {
      totalStatSql
        .unique
        .transact(tx)
    }

    override def foundBlocks(): F[List[FoundCode]] = {
      foundBlocksSql
        .to[List]
        .transact(tx)
    }

    override def foundPacks(): F[List[FoundCode]] = {
      foundPacksSql
        .to[List]
        .transact(tx)
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

  private
  def totalStatSql =
    sql"""
      select
        d.check_cnt,
        d.block_cnt,
        d.pack_cnt
      from (
        select
          count(distinct d.code) as check_cnt,
          count(distinct block.code) as block_cnt,
          count(distinct pack.code) as pack_cnt
        from
          dm_code_check_req d
          left join block on block.code = d.code
          left join pack on pack.code = d.code
        where
          length(d.code) > 35
          or length(d.code) < 50
      ) d
    """.query[TotalStat]

  private
  def statByUsersSql =
    sql"""
      select
        d.user_id,
        d.check_cnt,
        d.block_cnt,
        d.pack_cnt,
        (select coalesce(first_name, '') from dm_code_check_req where user_id = d.user_id limit 1) as first_name,
        (select coalesce(last_name, '') from dm_code_check_req where user_id = d.user_id limit 1) as last_name
      from (
        select
          d.user_id,
          count(distinct d.code) as check_cnt,
          count(distinct block.code) as block_cnt,
          count(distinct pack.code) as pack_cnt
        from
          dm_code_check_req d
          left join block on block.code = d.code
          left join pack on pack.code = d.code
        where
          length(d.code) > 35
          or length(d.code) < 50
        group by
          d.user_id
      ) d
    """.query[StatByUser]

  private
  val foundBlocksSql =
    fr"""
      select distinct on(user_id, code)
        d.user_id                   as userId,
        d.code                      as code,
        coalesce(d.first_name, '')  as firstName,
        coalesce(d.last_name, '')   as lastName,
        d.received_at               as foundAt
      from
        dm_code_check_req d
        left join block on block.code = d.code
        left join pack on pack.code = d.code
      where
        length(d.code) > 40 and length(d.code) < 50
        and block.code is not null
    """.query[FoundCode]

  private
  val foundPacksSql =
    fr"""
      select distinct on(user_id, code)
        d.user_id                   as userId,
        d.code                      as code,
        coalesce(d.first_name, '')  as firstName,
        coalesce(d.last_name, '')   as lastName,
        d.received_at               as foundAt
      from
        dm_code_check_req d
        left join block on block.code = d.code
        left join pack on pack.code = d.code
      where
        length(d.code) > 40 and length(d.code) < 50
        and pack.code is not null
    """.query[FoundCode]
}