package detektobot.adt

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class DmCodeCheckReq(
  userId: Int,
  userName: Option[String],
  userFirstName: Option[String],
  userLastName: Option[String],
  text: Option[String],
  fileId: String,
  fileUniqueId: String,
  code: Option[String],
  error: Option[String],
)

object DmCodeCheckReq {
  implicit val codec: Codec[DmCodeCheckReq] = deriveCodec
}