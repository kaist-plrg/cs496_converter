import scala.io.Source
import scala.sys.process.Process
import scala.collection.mutable.StringBuilder

import java.net.{URL, HttpURLConnection, URLDecoder}
import java.io.{FileOutputStream, File, PrintWriter}

import org.apache.commons.io.{IOUtils, FileUtils}

object App {
  def main(args: Array[String]): Unit = args.toList match {
    case name :: out :: Nil if name.endsWith(".tsv") =>
      val dir = new File(out)
      if (dir.exists && dir.isDirectory) {
        FileUtils.cleanDirectory(dir)
        val dir0 = out + File.separator + "photo0"
        val dir1 = out + File.separator + "photo1"
        val texf = out + File.separator + "application.tex"
        new File(dir0).mkdirs()
        new File(dir1).mkdirs()

        val fields :: lines = Source.fromFile(name).getLines.toList
        val data = lines.map(_.split("\t"))
        val photos0 = getPhotos(data.map(_(4)), dir0)
        val photos1 = getPhotos(data.map(_(5)), dir1)

        val entries = for (((rentry, photo0), photo1) <- data zip photos0 zip photos1) yield {
          val entry = rentry.map(latexEscape)
          val name = entry(3)
          val phone = entry(6)
          val email = entry(7)
          val univ = entry(8)
          val high = entry(9)
          val sem = entry(10)
          val ent = entry(11)
          val birth = entry(12)
          val sex = entry(13)
          val mil = entry(14)
          val last = entry(15)
          val major = entry(16)
          val double = entry(17)
          val answers = entry.drop(18)

          val start = Tex.entryStart(name, photo0, photo1, sex, birth, ent, sem,
            mil, last, univ, high, major, double, phone, email)
          val end = Tex.entryEnd
          val tex = (description zip answers).map(Tex.subsection).mkString(start, "", end)
          println(s"$name is processed")
          tex
        }

        val pw = new PrintWriter(new File(texf))
        pw.print(entries.mkString(Tex.bodyStart, "", Tex.bodyEnd))
        pw.close()

        Process("pdflatex application.tex", new File(out)).!
        Process("pdflatex application.tex", new File(out)).!
        Process("pdflatex application.tex", new File(out)).!
      } else {
        println(s"Directory $out not found")
      }
    case _ => println("Need input tsv file and output directory")
  }

  private def latexEscape(raw: String): String = {
    val esc = "_^$%#&{}".toSet
    val builder = new StringBuilder
    for (ch <- raw)
      if (esc(ch)) builder += '\\' += ch
      else if (ch == '~') builder ++= "\\texttildelow"
      else builder += ch
    builder.toString
  }

  private def getPhotos(urls: List[String], dir: String): List[Option[String]] =
    urls.zipWithIndex.par.map{ case (url, i) => {
      download(url).map{ case (ext, buf) => {
        val name = s"$i$ext"
        val out = new FileOutputStream(s"$dir${File.separator}$name")
        out.write(buf)
        out.flush()
        out.close()
        i.toString
      }
    }}}.toList

  private def getConnection(url: String): HttpURLConnection = {
    val connection = new URL(url).openConnection.asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    connection.setUseCaches(false)
    connection.setDoInput(true)
    connection.setDoOutput(false)
    val response = connection.getResponseCode

    if (response == 200) connection
    else {
      println(s"fail for $url with $response... try again")
      getConnection(url)
    }
  }

  private def download(preurl: String): Option[(String, Array[Byte])] =
    if (preurl.nonEmpty) {
      val url = preurl.replace("open?", "uc?export=download&")
      val connection = getConnection(url)

      val nameStr = connection.getHeaderField("Content-Disposition")
      if (nameStr == null) println(connection.getHeaderFields)
      val key = "filename*=UTF-8\'\'"
      val i = nameStr.indexOf(key)
      val name = nameStr.substring(i + key.length)
      println(s"${URLDecoder.decode(name, "UTF-8")} is downloaded")

      val input = connection.getInputStream
      val buf = IOUtils.toByteArray(input)
      input.close()
      connection.disconnect()

      Some(extension(name), buf)
    } else None

  private def extension(name: String): String = name.substring(name.lastIndexOf("."))

  private val description = Array(
    "수강 과목",
    "열정적 방학",
    "휴학 경험",
    "동아리",
    "인턴 경험",
    "프로젝트 경험",
    "재수/삼수/검정고시",
    "해외생활",
    "대회 수상",
    "취미",
    "지원 동기",
    "추가 이야기"
  )
}
