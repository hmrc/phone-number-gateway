import sbt.*

object AppDependencies {

  private val bootstrapVersion = "9.19.0"
  private val playSuffix       = "-play-30"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-backend$playSuffix"  % bootstrapVersion
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% s"bootstrap-test$playSuffix"     % bootstrapVersion
  ).map(_ % Test)

  val it = Seq.empty
}
