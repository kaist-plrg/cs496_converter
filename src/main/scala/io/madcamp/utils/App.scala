package io.madcamp.utils

import unfiltered.jetty.{Server => JServer}

object App {
  def main(args: Array[String]): Unit = run(
    args match {
      case Array()     => 8080
      case Array(port) => port.toInt
      case _           => sys.error("too many arguments")
    }
  )

  def run(port: Int): Unit = {
    JServer
      .http(port)
      .plan(new ServerPlan)
      .run(_ => ())
  }
}
