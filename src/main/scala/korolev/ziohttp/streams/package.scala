package korolev.ziohttp

import korolev.effect.syntax._
import korolev.effect.{Queue, Effect => KorolevEffect, Stream => KorolevStream}
import korolev.zio.ZioEffect
import zio.{RIO, ZIO}
import zio.stream.ZStream

package object streams {

  implicit class Fs2StreamOps[R, O](stream: ZStream[R, Throwable, O])
                                   (implicit eff:  ZioEffect[R, Throwable]) {

    def toKorolev(bufferSize: Int = 1): RIO[R, KorolevStream[RIO[R, *], O]] = {

      val queue = new Queue[RIO[R, *], O](bufferSize)
      val cancelToken: Either[Throwable, Unit] = Right(())
      stream
        .interruptWhen(queue.cancelSignal.as(cancelToken))
        .mapM(queue.enqueue)
        .mapM(_ => queue.stop())
        .runDrain
        .as(queue.stream)
    }
  }

  implicit class KorolevStreamOps[R, O](stream: KorolevStream[RIO[R, *], O])
                                        (implicit eff:  ZioEffect[R, Throwable]){
    def toZStream: ZStream[R, Throwable, O] =
      ZStream.unfoldM(()) { _ =>
        stream
          .pull()
          .map(mv => mv.map(v => (v, ())))
      }
  }

}
