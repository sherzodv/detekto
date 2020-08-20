package detektobot.adt

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class CommandReq(
  userId: Int,
  userName: Option[String],
  userFirstName: Option[String],
  userLastName: Option[String],
  text: Option[String],
)

object CommandReq {
  implicit val codec: Codec[CommandReq] = deriveCodec
}