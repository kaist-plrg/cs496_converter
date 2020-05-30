package io.madcamp.apptopdf

import unfiltered.jetty.{Server => JServer}

object App {
  def main(args: Array[String]): Unit =
    JServer.http(8080).plan(new ServerPlan).run()
}
