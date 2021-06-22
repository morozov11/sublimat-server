package korolev.ziohttp

import korolev.effect.{Effect, Queue, Stream => KStream}
import korolev.server.{KorolevService, KorolevServiceConfig, HttpRequest => KorolevHttpRequest}
import korolev.web.{PathAndQuery => PQ, Request => KorolevRequest, Response => KorolevResponse}
import korolev.state.{StateDeserializer, StateSerializer}
import korolev.scodec._
import korolev.data.Bytes
import _root_.zio.{RIO, Task, ZIO, ZQueue, Queue => ZIOQueue}
import _root_.zio.duration._
import _root_.zio.stream.ZStream
import _root_.zhttp.http._
import _root_.zhttp.service._
import _root_.zhttp.socket._
import io.netty.handler.codec.http.HttpResponseStatus
import korolev.zio.ZioEffect


trait ZioHttpKorolev[R] {

  import SubDirSyntax._
  import streams._

  //type R = Any with Clock
  type ZEffect = ZioEffect[R, Throwable]

  def http4sKorolevService[S: StateSerializer: StateDeserializer, M]
   (config: KorolevServiceConfig[RIO[R, *], S, M])
   (implicit eff:  ZEffect): HttpApp[R, Throwable] = {

    val korolevServer = korolev.server.korolevService(config)


//    def route(p: SubPath): ResponseM[R, Throwable] = SubRoute.collectM(p) {
//
//      case Method.GET --> Root if containsUpgradeHeader(p.req) =>
//        routeWsRequest(p.req, p.subPath, korolevServer)
//
//      case otherReqs =>
//        routeHttpRequest(otherReqs, korolevServer)
//    }
//
//    val root = config.rootPath.replace("/", "").replace("\\", "")
//
//    HttpApp.collectM {
//      case Prefix(root) andSub subPath => route(subPath)
//    }

    HttpApp.collectM {
      case req: Request if req.method == Method.GET && containsUpgradeHeader(req) =>
        routeWsRequest(req, req.endpoint._2.path, korolevServer)

      case req =>
        routeHttpRequest(SubPath(req, req.endpoint._2.path), korolevServer)
    }

  }

  private def routeHttpRequest
  (p: SubPath, korolevServer: KorolevService[RIO[R, *]])
  (implicit eff:  ZEffect): ResponseM[R, Throwable] = {

    SubRoute.collectM(p)  {
      case Method.GET --> Root =>
        println("GET")
        val body = KStream.empty[RIO[R, *], Bytes]
        val korolevRequest = mkKorolevRequest(p.req, p.subPath.toString(), body)
        handleHttpResponse(korolevServer, korolevRequest)

      case _ =>
        println("other")
        for {
          stream <- toKorolevBody(p.req.content)
          korolevRequest = mkKorolevRequest(p.req, p.subPath.toString(), stream)
          response <- handleHttpResponse(korolevServer, korolevRequest)
        } yield {
          response
        }
    }
  }

  private def containsUpgradeHeader(req: Request): Boolean = {
    val headers = req.headers
    val found = for {
      _ <- headers.find(h => String.valueOf(h.name).toLowerCase == "connection" && String.valueOf(h.value).toLowerCase == "upgrade")
      _ <- headers.find(h => String.valueOf(h.name).toLowerCase == "upgrade" && String.valueOf(h.value).toLowerCase == "websocket")
    } yield {}
    found.isDefined
  }

  private def routeWsRequest[S: StateSerializer: StateDeserializer, M]
  (req: Request, path: Path, korolevServer: KorolevService[RIO[R, *]])
  (implicit eff:  ZEffect): ResponseM[R, Throwable] = {

    val (fromClientIO, kStream) = makeSinkAndSubscriber()

    val fullPath = path.toString
    val korolevRequest = mkKorolevRequest[KStream[RIO[R, *], String]](req, fullPath, kStream)

    for {
      response <- korolevServer.ws(korolevRequest)
      fromClient <- fromClientIO
      toClient = response match {
        case KorolevResponse(_, outStream, _, _) =>
          outStream
            .map { out =>
              println(s"OUT ${out}")
              WebSocketFrame.Text(out)
            }
            .toZStream
        case _ =>
          throw new RuntimeException
      }
      route <- buildSocket(toClient, fromClient)
      _ <- fromClient.take.forever.forkDaemon
    } yield {
      route
    }
  }

  def makeSinkAndSubscriber()(implicit eff:  ZEffect) = {
    val queue = Queue[RIO[R, *], String]()

    val zSink = for {
      zQueue <- ZIOQueue.unbounded[WebSocketFrame]
      result = zQueue.mapM[R, Throwable, Unit] {
        case WebSocketFrame.Text(t) =>
          println(s"KIN ${t}")
          queue.enqueue(t)
        case _: WebSocketFrame.Close =>
          println("CLOSE")
          queue.close()
        case frame =>
          println("EXCEPTION FRAME")
          ZIO.fail(new Exception(s"Invalid frame type ${frame.getClass.getName}"))
      }
      _ <- (result.awaitShutdown *> queue.close()).forkDaemon
    } yield {
      result
    }

    (zSink, queue.stream)
  }

  private def buildSocket(
                           toClientStream: ZStream[R, Throwable, WebSocketFrame],
                           fromClientQueue: ZQueue[Any, R, Nothing, Throwable, WebSocketFrame, Unit]
                         )
                         (implicit eff:  ZEffect): RIO[R ,Response[R, Throwable]] = {


    val onMessage: Socket[R, Nothing, WebSocketFrame, WebSocketFrame] = Socket
      .fromFunction[WebSocketFrame] { _ => ZStream.empty }
      .contramapM {frame =>
        println(s"IN ${frame.toString}")
        fromClientQueue.offer(frame).map { b =>
          println(s"RES ${b}")
          frame
        }
      }

    val app =
      SocketApp.open(Socket.fromFunction[Any] { _ => toClientStream}) ++
      SocketApp.message(onMessage) ++
      SocketApp.close(_ => fromClientQueue.shutdown) ++
      SocketApp.decoder(SocketDecoder.allowExtensions)

    ZIO(Response.socket(app))
  }

  private def mkKorolevRequest[Body](request: Request,
                                           path: String,
                                           body: Body): KorolevRequest[Body] = {
    val cookies = findCookieHeader(request.headers)
    val params = request.url.queryParams.collect { case (k, v) if v.nonEmpty => (k, v.head) }
    KorolevRequest(
      pq = PQ.fromString(path).withParams(params),
      method = KorolevRequest.Method.fromString(request.method.toString()),
      renderedCookie = cookies.orNull,
      contentLength = findHeaderValue(request.headers, "content-length").map(_.toLong),
      headers = {
        val contentType = request.getContentType
        val contentTypeHeaders = {
          contentType.map { ct =>
            if(ct.contains("multipart")) Seq("content-type" -> contentType.toString) else Seq.empty
          }.getOrElse(Seq.empty)
        }
        request.headers.map(h => (String.valueOf(h.name), String.valueOf(h.value))) ++ contentTypeHeaders
      },
      body = body
    )
  }

  private def handleHttpResponse(korolevServer: KorolevService[RIO[R, *]],
                                 korolevRequest: KorolevHttpRequest[RIO[R, *]])
                                (implicit eff:  ZEffect): ResponseM[R, Throwable] = {
    korolevServer.http(korolevRequest).map {
      case KorolevResponse(status, stream, responseHeaders, _) =>
       val headers = korolevToZioHttpHeaders(responseHeaders)
        val body = stream.toZStream.flatMap { bytes: Bytes =>
          ZStream.fromIterable(bytes.as[Array[Byte]])
        }

        println(s"RESPONSE ${status}, ${responseHeaders}")

        Response.http(
          //ToDo: match korolev status directly
          status = Status.fromJHttpResponseStatus(HttpResponseStatus.valueOf(status.code)),
          headers = headers,
          content = HttpData.fromStream(body)
        )
    }

  }

  private def toKorolevBody(data: HttpData[R, Throwable])
                           (implicit eff:  ZEffect): RIO[R, KStream[RIO[R, *], Bytes]]  = {
    data match {
      case HttpData.Empty => Task(KStream.empty)
      case HttpData.CompleteData(chunk) => KStream(Bytes.wrap(chunk.toArray)).mat()
      case HttpData.StreamData(zstream) =>
        zstream.grouped(100).map(ch => Bytes.wrap(ch.toArray)).toKorolev()
    }
  }

  private def korolevToZioHttpHeaders(responseHeaders: Seq[(String, String)]): List[Header] = {
    responseHeaders.toList.map { case (name, value) => Header(name, value) }
  }

  private def findCookieHeader(headers: List[Header]): Option[String] = {
    findHeaderValue(headers, "cookie")
  }

  private def findHeaderValue(headers: List[Header],
                              name: String
                             ): Option[String] = {
    headers
      .find { h =>String.valueOf(h.name).toLowerCase() == name }
      .map(h => String.valueOf(h.value))
  }

}
