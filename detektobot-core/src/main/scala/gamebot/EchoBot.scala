package detekto

import java.io.InputStream

import cats.data.OptionT
import cats.effect.{ConcurrentEffect, Sync, Timer}
import cats.syntax.apply._
import cats.syntax.functor._
import cats.syntax.applicativeError._
import com.google.zxing.{BarcodeFormat, BinaryBitmap, DecodeHintType}
import com.google.zxing.common.GlobalHistogramBinarizer
import fs2.io.toInputStream
import javax.imageio.ImageIO
import org.http4s.client.Client
import telegramium.bots.high.implicits._
import telegramium.bots.high.{Api, LongPollBot}
import com.google.zxing.client.j2se.{BufferedImageLuminanceSource, MatrixToImageWriter}
import com.google.zxing.datamatrix.DataMatrixReader

class EchoBot[F[_]: Api: Timer: ConcurrentEffect](http: Client[F], token: String)
  extends LongPollBot[F](implicitly[Api[F]]) {

  import telegramium.bots._

  private val F = implicitly[Sync[F]]

  override def onMessage(msg: Message): F[Unit] = {
    OptionT
      .fromOption(msg.photo.maxByOption(_.width))
      .map(_.fileId)
      .flatMapF(fileId => getFile(fileId).exec.map(_.filePath))
      .semiflatMap(path => readFileAndDecode(path))
      .semiflatMap(code => sendMessage(ChatIntId(msg.chat.id), code).exec)
      .void
      .getOrElseF(F.unit)
      .handleErrorWith { e =>
        F.delay(e.printStackTrace()) *>
        sendMessage(
          chatId = ChatIntId(msg.chat.id),
          text = "Не удалось обнаружить код, попробуйте сфотографировать код крупнее",
        ).exec.void
      }
  }

  private def readFileAndDecode(path: String): F[String] = {
    http
      .get(s"https://api.telegram.org/file/bot$token/$path") { res =>
        res.body
          .through(toInputStream)
          .evalMap(decodeDmCode)
          .compile
          .toList
          .map(_.head)
      }
  }

  private def decodeDmCode(is: InputStream): F[String] = F.delay {
    import scala.jdk.CollectionConverters._
    val img = ImageIO.read(is)
    val bin = new BinaryBitmap(new GlobalHistogramBinarizer(new BufferedImageLuminanceSource(img)))

    ImageIO.write(MatrixToImageWriter.toBufferedImage(bin.getBlackMatrix), "jpg", new java.io.File("/home/sherzod/temp/dmresult/img.jpg"))

    val hints = new java.util.HashMap[DecodeHintType, Any]()
    hints.put(DecodeHintType.TRY_HARDER, ())
    hints.put(DecodeHintType.POSSIBLE_FORMATS, List(BarcodeFormat.DATA_MATRIX).asJava)
    val res = (new DataMatrixReader).decode(bin, hints)
    res.getText
  }

}
