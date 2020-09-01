package detektobot.adt

case class StatByUser(
  userId: Int,
  checkCount: Int,
  blockHits: Int,
  packHits: Int,
  firstName: String,
  lastName: String
)