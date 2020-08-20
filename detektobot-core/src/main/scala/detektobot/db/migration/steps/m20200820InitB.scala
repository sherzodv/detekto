package detektobot.db.migration.steps

import doobie.ConnectionIO

object m20200820InitB extends Step {
  def run(): ConnectionIO[Unit] = execBatchFromResource("20200820B.sql")
}

