package ru.ololo.db

import cats.effect._
import cats.implicits._
import monix.eval._

object Main extends TaskApp {
  def run(args: List[String]): Task[ExitCode] =
    args.headOption match {
      case Some(name) =>
        Task(println(s"Hello, \${name}.")).as(ExitCode.Success)
      case None =>
        Task(System.err.println("Usage: MyApp name")).as(ExitCode(2))
    }

  def run(args: List[String]): Task[ExitCode] =
    BlazeClientBuilder[Task](scheduler).resource.use { c =>
      val config = StudRankConfig.apply
      val module = Module(ClientLogger(true, true)(c), config)
      val routes = Routes(module, config)
      BlazeServerBuilder[Task]
        .bindHttp(8080, "0.0.0.0")
        .withHttpApp(routes.routes())
        .withIdleTimeout(120 seconds)
        .serve
        .compile
        .drain
        .as(ExitCode.Success)
    }
}
