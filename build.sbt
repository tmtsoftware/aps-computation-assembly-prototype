
lazy val aggregatedProjects: Seq[ProjectReference] = Seq(
  `aps-computationprototypeassembly`,
  `aps-computationprototypehcd`,
  `aps-computationprototypedeploy`,
  `aps-computationclient`
)

lazy val `computationprototype-root` = project
  .in(file("."))
  .aggregate(aggregatedProjects: _*)

// assembly module
lazy val `aps-computationprototypeassembly` = project
  .settings(
    libraryDependencies ++= Dependencies.Computationprototypeassembly,
  )

// hcd module
lazy val `aps-computationprototypehcd` = project
  .settings(
    libraryDependencies ++= Dependencies.Computationprototypehcd
  )

// hcd module
lazy val `aps-computationclient` = project
  .settings(
    libraryDependencies ++= Dependencies.Computationclient
  )

// deploy module
lazy val `aps-computationprototypedeploy` = project
  .dependsOn(
    `aps-computationprototypeassembly`,
    `aps-computationprototypehcd`
  )
  .enablePlugins(CswBuildInfo)
  .settings(
    libraryDependencies ++= Dependencies.ComputationprototypeDeploy
  )
