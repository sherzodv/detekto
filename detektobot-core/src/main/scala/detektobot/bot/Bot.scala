package detektobot
package bot

import telegramium.bots.high.implicits._
import telegramium.bots.high.{Api, LongPollBot}
import java.io.InputStream

import cats.syntax.option._
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.syntax.applicativeError._
import cats.data.OptionT
import cats.effect.{ConcurrentEffect, Sync, Timer}
import com.google.zxing.{BarcodeFormat, BinaryBitmap, DecodeHintType}
import com.google.zxing.client.j2se.{BufferedImageLuminanceSource, MatrixToImageWriter}
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.datamatrix.DataMatrixReader
import detektobot.adt.{Block, CommandReq, DmCodeCheckReq, Pack}
import detektobot.config.Conf
import detektobot.db.Repo
import fs2.io.toInputStream
import javax.imageio.ImageIO
import logstage.LogIO
import logstage.LogIO.log
import org.http4s.client.Client

class Bot[F[_]: Api: ConcurrentEffect: Timer: LogIO](
  http: Client[F],
  conf: Conf,
  dmCodeRepo: Repo.Service[F],
) extends LongPollBot[F](implicitly[Api[F]]) {

  import telegramium.bots._

  private val F = implicitly[Sync[F]]

  override def onMessage(msg: Message): F[Unit] = {
    msg.text.fold(F.unit)(onCommand(_, msg)) *>
      msg.photo.maxByOption(_.width).fold(F.unit)(onPhoto(_, msg)) *>
        msg.document.fold(F.unit)(onPack(_, msg))
  }

  private def execCmd(cmd: String, msg: Message, user: User): F[Unit] = {
    cmd match {
      case "/stat" => {
        for {
          byUsers <- dmCodeRepo.statByUsers()
          total   <- dmCodeRepo.totalStat()
          rep = byUsers.map{ stat =>
            s"""*${stat.firstName} ${stat.lastName}*
               |`Проверено: ${stat.checkCount}`
               |`x Блоков : ${stat.blockHits}`
               |`x Паков  : ${stat.packHits}`
               |""".stripMargin
          }.mkString("\n") +
            s"""
               |*Всего*
               |`Проверено: ${total.checkCount}`
               |`x Блоков : ${total.blockHits}`
               |`x Паков  : ${total.packHits}`
               |""".stripMargin
          _ <- sendMessage(ChatIntId(msg.chat.id), rep, Markdown.some).exec.void
        } yield ()
      }
      case x => sendMessage(ChatIntId(msg.chat.id), s"Неопознанная команда: $x").exec.void
    }
  }

  private def onPack(doc: Document, msg: Message): F[Unit] = {
    val notice = log.error("Принят файл от неавторизованного пользователя")
    msg.from.fold(notice) { user =>
      conf.admin.admins.find(_ == user.id).fold(notice) { _ =>
        OptionT(getFile(doc.fileId).exec.map(_.filePath))
          .getOrElseF(new RuntimeException("Файл не найден").raiseError[F, String])
          .flatMap(readPackAndParse)
          .flatMap(pack => dmCodeRepo.registerPack(pack))
          .flatMap{ blockCount =>
            sendMessage(
              ChatIntId(msg.chat.id),
              s"Пакет блоков успешно зарегистрирован в системе. Кол-во $blockCount шт."
            ).exec.void
          }
          .handleErrorWith { e =>
            log.error(s"Pack processing error: $e") *>
              sendMessage(
                ChatIntId(msg.chat.id),
                s"""
                   |Не удалось распарсить блоки. Пожалуйста, убедитесь что файл соблюдает формат файла блоков.
                   |Первые 10 строчек аттрибуты пака. Пустая строка. Затем строки кодов блоков.
                   |
                   |Ошибка: ${e.getMessage}
                   |""".stripMargin
              ).exec.void
          }
      }
    }
  }

  private def wrongMsg(msg: Message): F[Unit] = {
    sendMessage(
      ChatIntId(msg.chat.id),
      "Неверный формат кода",
    ).exec.void
  }

  private def onCommand(cmd: String, msg: Message): F[Unit] = {
    val defaultReq = mkCommandReq(cmd, msg)
    val checkCodeReq = DmCodeCheckReq(
      userId        = msg.from.map(_.id).getOrElse(-1),
      userName      = msg.from.flatMap(_.username),
      userFirstName = msg.from.map(_.firstName),
      userLastName  = msg.from.flatMap(_.lastName),
      text          = msg.text,
      fileId        = "",
      fileUniqueId  = "",
      code          = Some(
                        cmd
                          .replaceAll("[^0-9A-Za-z]+", "")
                          .replaceAll(" ", "")
                          .replaceAll("\\p{C}", "")
                      ),
      error         = None,
    )
    dmCodeRepo.registerCmd(defaultReq) *> (
      if (cmd.startsWith("/") || cmd.length < 40) {
        msg.from.fold(wrongMsg(msg)){ user =>
          if (conf.admin.admins.contains(user.id)) {
            execCmd(cmd, msg, user)
          } else {
            wrongMsg(msg)
          }
        }
      } else {
        dmCodeRepo.checkDmCode(checkCodeReq).map(x => (x, cmd))
          .flatMap{ case (checkResult, code) =>
            checkResult.fold(
              sendMessage(
                ChatIntId(msg.chat.id),
                s"""
                   |✅
                   |Код не найден в базе: $code
                   |""".stripMargin
              ).exec.void
            )(packCode => sendMessage(
              ChatIntId(msg.chat.id),
              s"""
                 |⚠️⛔️⚠️
                 |Код обнаружен в базе: $code
                 |Пакет: $packCode
                 |""".stripMargin
            ).exec.void)
          }
          .void
      }
    )
  }

  private def onPhoto(photo: PhotoSize, msg: Message): F[Unit] = {
    val defaultReq = mkDmCheckReq(photo, msg)
    OptionT(getFile(photo.fileId).exec.map(x => x.filePath.map(x.fileUniqueId -> _)))
      .getOrElseF(new RuntimeException("Файл не найден").raiseError)
      .flatMap{ case (id, path) => readCodeAndDecode(id, path) }
      .flatMap(code => dmCodeRepo.checkDmCode(defaultReq.copy(code = Some(code))).map(x => (x, code)))
      .flatMap{ case (checkResult, code) =>
        checkResult.fold(
          sendMessage(
            ChatIntId(msg.chat.id),
            s"""
              |Блок не найден в базе: $code
              |""".stripMargin
          ).exec.void
        )(packCode => sendMessage(
          ChatIntId(msg.chat.id),
          s"""
            |Блок найден: $code
            |Пакет: $packCode
            |""".stripMargin
        ).exec.void)
      }
      .handleErrorWith { e =>
        log.error(s"DM decoding error: $e") *>
          dmCodeRepo.checkDmCode(defaultReq.copy(error = Some(e.getMessage))) *>
            sendMessage(
              ChatIntId(msg.chat.id),
              "Не удалось распознать код. Пожалуйста, убедитесь что код в самом центре изображения."
            ).exec.void
      }
  }

  private def mkCommandReq(text: String, msg: Message): CommandReq = {
    CommandReq(
      userId        = msg.from.map(_.id).getOrElse(-1),
      userName      = msg.from.flatMap(_.username),
      userFirstName = msg.from.map(_.firstName),
      userLastName  = msg.from.flatMap(_.lastName),
      text          = msg.text,
    )
  }

  private def mkDmCheckReq(photo: PhotoSize, msg: Message): DmCodeCheckReq = {
    DmCodeCheckReq(
      userId        = msg.from.map(_.id).getOrElse(-1),
      userName      = msg.from.flatMap(_.username),
      userFirstName = msg.from.map(_.firstName),
      userLastName  = msg.from.flatMap(_.lastName),
      text          = msg.text,
      fileId        = photo.fileId,
      fileUniqueId  = photo.fileUniqueId,
      code          = None,
      error         = None,
    )
  }

  private def readPackAndParse(path: String): F[Pack] = {
    http
      .get(s"https://api.telegram.org/file/bot${conf.bot.token}/$path") { res =>
        res.body
          .through(toInputStream)
          .evalMap(parsePack)
          .compile
          .toList
          .map(_.head)
      }
  }

  private def readCodeAndDecode(id: String, path: String): F[String] = {
    http
      .get(s"https://api.telegram.org/file/bot${conf.bot.token}/$path") { res =>
        res.body
          .through(toInputStream)
          .evalMap(decodeDmCode(id, _))
          .compile
          .toList
          .map(_.head)
      }
  }

  private def parsePack(is: InputStream): F[Pack] = F.delay {
    val lines = scala.io.Source.fromInputStream(is).getLines().toList
    val packCode = lines.take(10)
      .map(_.split("="))
      .map(arr => arr(0) -> arr(1))
      .toMap.getOrElse("Code", throw new RuntimeException("Не удалось обнаружить код пака"))
    val blocks = lines.drop(11).map(line => Block(code = line.split("\t")(1)))
    Pack(code = packCode, blocks = blocks)
  }

  private def decodeDmCode(id: String, is: InputStream) = F.delay {
    import scala.jdk.CollectionConverters._
    val img = ImageIO.read(is)
    val bin = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(img)))
    ImageIO.write(MatrixToImageWriter.toBufferedImage(bin.getBlackMatrix), "jpg", new java.io.File(s"${conf.admin.storage}/$id.jpg"))
    val hints = new java.util.HashMap[DecodeHintType, Any]()
    hints.put(DecodeHintType.TRY_HARDER, ())
    hints.put(DecodeHintType.POSSIBLE_FORMATS, List(BarcodeFormat.DATA_MATRIX).asJava)
    val res = (new DataMatrixReader).decode(bin, hints)
    res.getText
  }

}