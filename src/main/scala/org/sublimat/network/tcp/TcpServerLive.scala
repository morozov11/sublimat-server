package org.sublimat.network.tcp

import zio.Task


object TcpServerLive {

  override def bind(host: String, port: Int): Task[TcpServerLive] = {
    ???
  }

}

//ToDo: implement Netty TCP server with this interface and manage his inside ZLayer

class TcpServerLive(host: String, port: Int) extends TcpServer {



  override def close(): Task[Unit] = {
    ???
  }

}
