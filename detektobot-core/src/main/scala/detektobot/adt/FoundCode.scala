package detektobot.adt

case class FoundCode(
  userId: Int,
  code: String,
  firstName: String,
  lastName: String,
  foundAt: String,
)