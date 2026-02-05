package aps.computationprototypedeploy

import csw.framework.deploy.hostconfig.HostConfig
import csw.prefix.models.Subsystem

object ComputationprototypeHostConfigApp {

  def main(args: Array[String]): Unit = {
    HostConfig.start("computationprototype_host_config_app", Subsystem.withNameInsensitive("APS"), args)
  }
}
