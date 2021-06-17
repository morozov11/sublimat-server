package org.sublimat.network.tcp

import zio.{Queue, Task}


trait TcpServer {

  def offer(data: String): Task[Unit]

  def outQueue: Queue[String]

}

