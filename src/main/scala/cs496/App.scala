import scala.io.Source
import scala.sys.process.Process
import scala.collection.mutable.{StringBuilder, ListBuffer}

import java.net.{URL, HttpURLConnection, URLDecoder}
import java.io.{FileOutputStream, File, PrintWriter}

import org.apache.commons.io.{IOUtils, FileUtils}

object App {
  def main(args: Array[String]): Unit = args.toList match {
    case name :: name0 :: out :: tail if name.endsWith(".tsv") && name0.endsWith(".tsv") =>
      val downloadFiles = tail.contains("-d")
      val forShare = tail.contains("-s")
      val dir = new File(out)

      if (dir.exists && dir.isDirectory) {
        val dir0 = out + File.separator + "photo0"
        val fdir0 = new File(dir0)
        if (downloadFiles) {
          if (fdir0.exists)
            FileUtils.cleanDirectory(fdir0)
          else
            fdir0.mkdirs()
        } else if (!fdir0.exists)
          throw new Exception("photo directory does not exist!")

        val nss = "application.tex"
        val texssf = out + File.separator + nss

        val fields :: lines = Source.fromFile(name).getLines.toList
          .map(_.filter(c => c < 127 || (0xAC00 <= c && c <= 0xD8FF)))
        val data = lines.map(_.split("\t"))

        val photos0 = 
          if (downloadFiles) getPhotos(data.map(_(4)), dir0)
          else (0 to fields.length).map(i => Some(i.toString))

        val _ :: prevs = Source.fromFile(name0).getLines.toList.map(PrevApplicant)
        val reApplicants = ListBuffer[String]()

        val entries = for (((rentry, photo0), i) <- (data zip photos0).zipWithIndex) yield {
          val entry = rentry.map(latexEscape)
          val rname = entry(3)
          val name = if (forShare) s"익명의 지원자 ${i + 1}" else rname
          val phone = if (forShare) "xxx-xxxx-xxxx" else entry(6)
          val email = if (forShare) "xxxxxxxx@xxxxx.xxx" else entry(7)
          val univ = entry(8)
          val high = entry(9)
          val ent = entry(10)
          val sem = entry(11)
          val birth = entry(12)
          val sex = entry(13)
          val mil = entry(14)
          val last = entry(15)
          val major = entry(16)
          val double = entry(17)
          val answers = entry.drop(18)

          val prev = prevs.filter(p => p.name == rname && p.birth.take(4) == birth.take(4))
          val prevStr = if (prev.nonEmpty) prev.mkString(", ") else "해당 없음"
          if (prev.nonEmpty) reApplicants += s"${i + 1}. $rname"

          val start = Tex.entryStart(name, photo0, sex, birth, ent, sem,
            mil, last, univ, high, major, double, phone, email)
          val end = Tex.entryEnd
          val tex = (("이전 지원 여부" +: description) zip (prevStr +: answers))
            .map(Tex.subsection).mkString(start, "", end)
          println(s"$name is processed")
          tex
        }

        val ss = entries.mkString(Tex.bodyStart, "", Tex.bodyEnd)
        val pwss = new PrintWriter(new File(texssf))
        pwss.print(ss)
        pwss.close()

        Process(s"pdflatex $nss", new File(out)).!
        Process(s"pdflatex $nss", new File(out)).!
        Process(s"pdflatex $nss", new File(out)).!

        println(reApplicants.mkString("\n"))
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

case class PrevApplicant(name: String, birth: String, year: String, desc: String) {
  override val toString: String = s"$year($desc)"
}
object PrevApplicant extends (String => PrevApplicant) {
  def apply(line: String): PrevApplicant = {
    val s = line.split("\t")
    PrevApplicant(s(3), s(8), s(1), s(2))
  }
}
