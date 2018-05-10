import scala.io.Source
import java.io.{PrintWriter, File}

object App {
  def main(args: Array[String]) {
    args.toList match {
      case "--photo" :: name :: out :: Nil if name.endsWith(".tsv") =>
        val dir = new File(out)
        if (dir.exists && dir.isDirectory) {
          val lines = Source.fromFile(name).getLines.toList

          val photos0 = lines.tail.map(str => String.format(download, str.split("\t").apply(4).replace("open?", "uc?export=download&")))
          val photos1 = lines.tail
            .map(_.split("\t").apply(5))
            .map(link => if (link.length == 0) nolink else link)
            .map(_.replace("open?", "uc?export=download&"))
            .map(String.format(download, _))

          val writer0 = new PrintWriter(new File(out + SEP + "down0.txt"))
          writer0.write(downloadpre)
          writer0.write(photos0.mkString("\n"))
          writer0.close()

          val writer1 = new PrintWriter(new File(out + SEP + "down1.txt"))
          writer1.write(downloadpre)
          writer1.write(photos1.mkString("\n"))
          writer1.close()
        }
      case "--html" :: name :: photo0 :: photo1 :: out :: Nil if name.endsWith(".tsv") =>
        val dir = new File(out)
        if (dir.exists && dir.isDirectory) {
          new File(out + SEP + dirName).mkdirs()

          val lines = Source.fromFile(name).getLines.toList.tail
          val photoName0 = Source.fromFile(photo0).getLines.toList
          val photoName1 = Source.fromFile(photo1).getLines.toList

          val names = (0 until lines.length).map(i => {
            val datas = lines(i).split("\t")
            val photo0 = photoName0(i)
            val photo1 = if (datas(5).length == 0) "no.png" else photoName1(i)

            val name = datas(3)
            val phone = datas(6)
            val mail = datas(7)
            val univ = datas(8)
            val high = datas(9)
            val sem = datas(10)
            val ent = datas(11)
            val birth = datas(12)
            val sex = datas(13)
            val mil = datas(14)
            val major = datas(15)
            val double = datas(16)

            val tablehtml = String.format(table, photo0, photo1, sex, birth, sem, ent, mil, univ, high, major, double, phone, mail)

            val answers = datas.drop(17)

            val writer = new PrintWriter(new File(out + SEP + dirName + SEP + i + ".html"))
            writer.write(String.format(template, (i + 1) + ". " + name, 
              String.format(appTemplate, (i + 1) + ". " + name, tablehtml, product(description.map(" - " + _), answers).map { case (d, a) =>
                String.format(tag, d, a)
              }.mkString("\n"))
            ))
            writer.close()
            (name, i)
          })

          val writer = new PrintWriter(new File(out + SEP + "index.html"))
          writer.write(String.format(template, "CS496 applicants", names.map(name => String.format(atag, dirName + SEP + name._2 + ".html", name._1)).mkString("<ol>", "\n", "</ol>")))
          writer.close()
        }
      case _ => println("Need input tsv file and output directory")
    }
  }

  def product(a0: Array[String], a1: Array[String]) =
    (0 until Math.min(a0.length, a1.length)).map(i => (a0(i), a1(i)))

  val downloadpre = """function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}
var link;
"""
  val download = """link = document.createElement("a");
link.href="%s";
link.click();
await sleep(5000);"""
  val nolink = "https://drive.google.com/open?id=1b9Jij0cCl21zDXlZ2jRfethPHTxT4DRm"

  val SEP = "/"
  val dirName = "applicants"

  val tag = """<h3>%s</h3>
<p>%s</p>"""
  val atag = """<li><a target="_blank" href="%s">%s</a></li>"""

  val description = Array("수강 과목", "열정적 방학", "휴학 경험", "동아리", "인턴 경험", "프로젝트 경험", "재수/삼수/검정고시", "해외생활", "대회 수상", "취미", "지원 동기", "추가 이야기")

  val template = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
h3 {
	font-family: "Malgun Gothic", "굴림", "Gulim", "Arial";
    font-size: 14px;
	font-weight: 900;
	padding-bottom: 0px;
	margin-bottom: 0px;
}
p, th {
	font-family: "Malgun Gothic", "굴림", "Gulim", "Arial";
    font-size: 14px;
	font-weight: 100;
	margin-top: 3px;
	padding-top: 0px;
	padding-bottom: 10px;
	padding-left: 10px;
}
table {
    border-collapse: collapse;
}
table, th, td {
    border: 1px solid black;
    font-size: 14px;
}
</style>
<title>%s</title>
</head>
<body>
%s
</body>
</html>"""

  val appTemplate = """<h1>%s</h1>
%s
%s"""

  val table = """<table style="width:100%%">
  <tr>
    <td rowspan="5" width="150px">
      <img src="../photos0/%s" height="200" />
    </td>
    <td rowspan="5" width="100px">
      <img src="../photos1/%s" height="200" />
    </td>
    <th><b>성별</b></th>
    <th>%s</th> 
    <th><b>생년</b></th>
    <th>%s</th>
  </tr>
  <tr>
    <th><b>학기/학번</b></th>
    <th>%s/%s</th> 
    <th><b>병역</b></th>
    <th>%s</th>
  </tr>
  <tr>
    <th><b>대학교</b></th>
    <th>%s</th> 
    <th><b>고교</b></th>
    <th>%s</th>
  </tr>
  <tr>
    <th><b>주전공</b></th>
    <th>%s</th> 
    <th><b>복수/부전공</b></th>
    <th>%s</th>
  </tr>
  <tr>
    <th><b>전화번호</b></th>
    <th>%s</th> 
    <th><b>이메일</b></th>
    <th>%s</th>
  </tr>
</table>"""

}
