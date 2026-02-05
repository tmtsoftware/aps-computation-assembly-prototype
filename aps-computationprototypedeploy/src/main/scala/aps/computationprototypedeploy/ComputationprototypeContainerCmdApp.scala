package aps.computationprototypedeploy

import csw.framework.deploy.containercmd.ContainerCmd
import csw.prefix.models.Subsystem

object ComputationprototypeContainerCmdApp {

  def main(args: Array[String]): Unit = {
    ContainerCmd.start("computationprototype_container_cmd_app", Subsystem.withNameInsensitive("APS"), args)
  }
}
