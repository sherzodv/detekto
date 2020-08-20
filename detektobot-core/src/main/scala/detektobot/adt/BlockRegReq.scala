package detektobot.adt

case class BlockRegReq(
  userId: Int,
  userName: Option[String],
  userFirstName: Option[String],
  userLastName: Option[String],
  text: Option[String],
  fileId: String,
  fileUniqueId: String,
  error: Option[String],
)

