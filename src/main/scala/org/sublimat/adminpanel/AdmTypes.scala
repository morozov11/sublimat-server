package org.sublimat.adminpanel

import org.sublimat.Layers.AppEnv
import zio.RIO

object AdmTypes {

  type AdmTask[A] = RIO[AppEnv, A]

}
