
import java.time._
import java.time.format.DateTimeFormatter

object Version {

  import scala.util.Try

  case class BuildException(override val getMessage: String) extends Exception

  val timestamp: String = ZonedDateTime
    .now(ZoneId.of("UTC"))
    .format(DateTimeFormatter.ofPattern("yyMMddHHmm"))

  def debVersion(version: String, postfix: String): String = {
    s"$version-$timestamp-$postfix"
  }

  def getBranch(gitBranchName: String): String = {
    gitBranchName
  }

  def getCommit(gitHeadCommit: Option[String]): String = {
    gitHeadCommit
      .map(_.take(6))
      .getOrElse(
        throw BuildException("Empty repository, can't get commit hash")
      )
  }

  def getVersion(gitDescribedVersion: Option[String],
                 gitUncommittedChanges: Boolean,
                 axisBranch: String,
                 axisCommit: String): String = {
    val sha1 = axisCommit
    val dirty = if (gitUncommittedChanges) "-DIRTY" else ""
    val tagver = gitDescribedVersion.getOrElse(throw BuildException("No tag found to make an RC version"))
    axisBranch match {
      case "dev" =>
        // When we are on `dev` branch, this is a snapshot.
        // It means that version of the libraries stay the same, but API may change.
        s"${tagver}${dirty}-SNAPSHOT"
      case "test" =>
        // When we are on test branch, this is a release candidate.
        // This means that API is frozen, only bug fix merges or hotfixes are allowed.
        // Each time when we want to deploy the test branch a new tag must be created with RC number increased.
        // When test branch becomes stable, it merges to a prod branch.
        // Tags on test branch need to match "x.y.z-RCn"
        // where x, y, z, n are numbers
        s"${tagver}${dirty}"
      case "prod" =>
        // When we on prod branch, this is a release.
        // Tags on prod branch need to match "x.y.z"
        s"${tagver}${dirty}"
      case _ =>
        // Otherwise this is a local development build (must not be exposed in any way)
        s"${tagver}-LOCAL-SNAPSHOT"
    }
  }

}
