package detektobot.adt

case class Block(code: String)
case class Pack(code: String, blocks: List[Block])
