package detektobot.db.migration.steps

import doobie.ConnectionIO

object m20200820Init extends Step {
  def run(): ConnectionIO[Unit] = execBatchFromResource("20200820A.sql")
}

