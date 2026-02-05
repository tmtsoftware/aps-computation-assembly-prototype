import sbt._

object Dependencies {

  val Computationprototypeassembly = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test,
    Libs.`scalatest` % Test,
    Libs.`junit4-interface` % Test,
    Libs.`testng-6-7` % Test,
  )

  val Computationprototypehcd = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test,
    Libs.`scalatest` % Test,
    Libs.`junit4-interface` % Test,
    Libs.`testng-6-7` % Test,
  )

  val ComputationprototypeDeploy = Seq(
    CSW.`csw-framework`,
    CSW.`csw-testkit` % Test
  )
}
