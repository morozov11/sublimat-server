package korolev.ziohttp

import zhttp.http._
import zio.{Task, ZIO}

object SubDirSyntax {

  object SubRoute {

    def collect[R, E](p: SubPath)(pf: PartialFunction[SubPath, Response[R, E]]): Response[R, E] =
      pf.applyOrElse(p, {p: SubPath => Response.HttpResponse(Status.NOT_FOUND, Nil, HttpData.empty)})

    def collectM[R, E](p: SubPath)(pf: PartialFunction[SubPath, ResponseM[R, E]]): ZIO[R, E, Response[R, E]] =
      pf.applyOrElse(
        p,
        {_: SubPath => Task(Response.HttpResponse(Status.NOT_FOUND, Nil, HttpData.empty)).asInstanceOf[ResponseM[R, E]]}
      )
  }

  case class Prefix(name: String)

  case class SubPath(req: Request, subPath: Path)

  object --> {
    def unapply(sub: SubPath): Option[Route] =
      Option(sub.req.endpoint._1 -> sub.subPath)
  }

  object andSub {
    def unapply(request: Request): Option[(Prefix, SubPath)] =
      request.endpoint._2.path.toList.headOption.map { prefix =>
        if(prefix.nonEmpty) {
          Prefix(prefix) -> SubPath(request, Path(request.endpoint._2.path.toList.tail))
        } else {
          Prefix(prefix) -> SubPath(request, Path(request.endpoint._2.path.toList))
        }
      }
  }

}
