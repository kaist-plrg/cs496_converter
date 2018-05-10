import scala.io.Source
import java.io.{PrintWriter, File}

object App {
  def main(args: Array[String]) {
    args.toList match {
      case name :: out :: Nil if name.endsWith(".tsv") =>
        val dir = new File(out)
        if (dir.exists && dir.isDirectory) {
          new File(out + SEP + dirName).mkdirs()

          val lines = Source.fromFile(name).getLines.toList
          val description = lines.head.split("\t").drop(3)

          val names = lines.tail.map((str: String) => {
            val datas = str.split("\t")
            val name = datas(3)

            val answers = datas.drop(3)
            val writer = new PrintWriter(new File(out + SEP + dirName + SEP + name + ".html"))
            writer.write(String.format(template, name, product(description, answers).map { case (d, a) =>
              if (d.contains("사진")) {
                if (a.length == 0)
                  noatag
                else
                  String.format(atag, a, d)
              } else
               String.format(tag, d, a)
            }.mkString("\n")))
            writer.close()
            name
          })

          val writer = new PrintWriter(new File(out + SEP + "index.html"))
          writer.write(String.format(template, "CS496 applicants", names.map(name => String.format(atag, dirName + SEP + name + ".html", name)).mkString("\n")))
          writer.close()
        }
      case _ => println("Need input tsv file and output directory")
    }
  }

  def product(a0: Array[String], a1: Array[String]) =
    (0 until Math.min(a0.length, a1.length)).map(i => (a0(i), a1(i)))

  val SEP = "/"
  val dirName = "applicants"

  val tag = """<h3>%s</h3>
<p>%s</p>"""

  val atag = """<a target="_blank" href="%s">%s</a><br>"""
  val noatag = """<p>No photo</p>"""

  val template = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<title>%s</title>
</head>
<body>
%s
</body>
</html>"""

}
