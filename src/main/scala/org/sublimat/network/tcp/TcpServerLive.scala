package org.sublimat.network.tcp

import zio.{Queue, Task}


object TcpServerLive {

  def bind(host: String, port: Int): Task[TcpServerLive] = {
    ???
  }

}

//ToDo: implement Netty TCP server with this interface and manage his inside ZLayer

class TcpServerLive(host: String, port: Int) extends TcpServer {

  def close(): Task[Unit] = {
    ???
  }

  override def offer(data: String): Task[Unit] = ???

  override def outQueue: Queue[String] = ???

}
