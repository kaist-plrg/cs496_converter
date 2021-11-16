package io.madcamp.utils

import unfiltered.filter.Plan
import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._
import org.apache.commons.io.FileUtils
import org.apache.poi.ss.usermodel.{WorkbookFactory, Row}
import play.api.libs.json.{Json, JsValue}
import java.io.{File, PrintWriter}
import java.net.URLDecoder.decode
import scala.collection.parallel.CollectionConverters._
import scala.jdk.CollectionConverters._
import scala.sys.process.Process
import scala.util.Random
import scala.io.Source
import scala.collection.mutable.{Map => MMap}

class ServerPlan extends Plan {

  val home = sys.env("MADCAMP_UTIL_HOME")
  val out = home + File.separator + "out"
  val dir = new File(out)
  val sheetDir = new File(out + File.separator + "sheets")
  val photoDir = new File(out + File.separator + "photos")
  val texDir = new File(out + File.separator + "tex")
  val pickDir = new File(out + File.separator + "pick")

  if (!dir.exists) dir.mkdir()
  if (!sheetDir.exists) sheetDir.mkdir()
  if (!photoDir.exists) photoDir.mkdir()
  if (!texDir.exists) texDir.mkdir()
  if (!pickDir.exists) pickDir.mkdir()

  val afile = sheetDir.getAbsolutePath + File.separator + "applicants.xlsx"
  val pfile = sheetDir.getAbsolutePath + File.separator + "prevs.xlsx"
  val sfile = sheetDir.getAbsolutePath + File.separator + "summaries.xlsx"
  val efile = sheetDir.getAbsolutePath + File.separator + "evals.xlsx"
  val cfile = sheetDir.getAbsolutePath + File.separator + "config.xlsx"

  val service =
    GoogleDriveUtil.service(home + File.separator + "credential.json")

  var students: List[(Student, Int)] = null
  var summaries: List[Row] = null

  def intent = Directive.Intent {
    case r @ GET(Path("/")) =>
      success(
        HtmlContent ~> ResponseString(
          Source.fromFile(home + File.separator + "index.html").mkString
        )
      )
    case r @ GET(Path("/index.js")) =>
      success(
        ResponseHeader("Content-Type", List("text/javascript")) ~>
          ResponseString(
            Source.fromFile(home + File.separator + "index.js").mkString
          )
      )

    case r @ GET(Path("/downloads") & Params(params)) =>
      run {
        val id = getId(params("id"))
        val files = GoogleDriveUtil.findFiles(service, id)
        val aid = files.find(_.getName.contains("지원서(응답)")).get.getId
        val pid = files.find(_.getName.contains("재지원자 리스트")).get.getId
        val sid = files.find(_.getName.contains("질문 요약")).get.getId
        FileUtils.cleanDirectory(sheetDir)
        GoogleDriveUtil.downloadExcel(service, aid, afile)
        GoogleDriveUtil.downloadExcel(service, pid, pfile)
        GoogleDriveUtil.downloadExcel(service, sid, sfile)
      }

    case r @ GET(Path("/students")) =>
      run {
        makeStudents()
      }

    case r @ GET(Path("/photos") & Params(params)) =>
      run {
        FileUtils.cleanDirectory(photoDir)
        def aux(ps: collection.parallel.ParSeq[(Student, Int)]): Unit = {
          val failed = ps.filterNot { case (s, i) =>
            GoogleDriveUtil.downloadPhoto(service, s.photo, i, photoDir)
          }
          if (failed.length == ps.length)
            println(s"${failed.length} failed again. Stop!")
          else if (failed.nonEmpty) {
            println(s"${failed.length} failed. Retrying in 10 seconds.")
            Thread.sleep(10000)
            aux(failed)
          }
        }
        aux(students.par)
        success(Ok)
      }

    case r @ GET(Path("/tex") & Params(params)) =>
      run {
        val parent = getId(params("id"))
        FileUtils.cleanDirectory(texDir)
        val (n1, f1) = printTex(false)
        val (n2, f2) = printTex(true)
        buildTex(n1, f1)
        buildTex(n2, f2)
        GoogleDriveUtil.uploadPdf(
          service,
          parent,
          "CS496: 20xx 여름/겨울 지원서.pdf",
          new File(f1)
        )
        GoogleDriveUtil.uploadPdf(
          service,
          parent,
          "CS496: 20xx 여름/겨울 지원서(공유).pdf",
          new File(f2)
        )
      }

    case r @ GET(Path("/evals") & Params(params)) =>
      run {
        val id = getId(params("id"))
        val files = GoogleDriveUtil.findFiles(service, id)
        val eid = files.find(_.getName.contains("평가 양식")).get.getId
        val cid = files.find(_.getName.contains("선발 비율")).get.getId
        GoogleDriveUtil.downloadExcel(service, eid, efile)
        GoogleDriveUtil.downloadExcel(service, cid, cfile)
      }

    case r @ GET(Path("/pick") & Params(params)) =>
      runJson {
        if (summaries == null)
          makeStudents()

        // configurations
        val seed = params("seed").mkString
        val parent = getId(params("id"))
        val size = params("size").mkString.toInt
        val numOfSessions = params("num").mkString.toInt

        val head :: _data = getRows(efile)
        val configs = getRows(cfile)
          .map(ExcelUtil.getString(_, 0))
          .takeWhile(_.nonEmpty)
        Applicant.initialize(head, summaries(5), summaries(6))
        val conf = new PickConfig(configs)

        val data = _data.filter(r => ExcelUtil.getString(r, 0).nonEmpty)
        if (students.length != data.length)
          sys.error(s"${students.length} != ${data.length}")

        // making sessions
        val origApplicants = students.map { case (s, _) =>
          val r = data
            .find(r =>
              ExcelUtil.getString(r, 0) == s.name && ExcelUtil
                .getString(r, 1) == s.university
            )
            .get
          Applicant(s, r)
        }
        val applicants =
          origApplicants.filterNot(a => a.coding == "하" && a.cooperation == "하")
        val byAccept = applicants.groupBy(_.accept)
        val accepts = byAccept("O")
        val intermediates = byAccept("?")
        val (mintermediates, nintermediates) = intermediates.partition(_.motiv)

        val iter = 10
        val sessionss = for (i <- 1 to iter) yield {
          val sessions = List.fill(numOfSessions)(new Session(conf, size))
          // val sessions = List(new Session(conf, 20), new Session(conf, 24))
          val rand = new Random((seed + i).##)

          register(rand.shuffle(accepts), sessions)
          register(rand.shuffle(mintermediates), sessions)
          register(rand.shuffle(nintermediates), sessions)

          sessions
        }
        def univVar(sessions: List[Session]): Double = {
          val univss = sessions.map(
            _.getApplicants.map(_.university).filterNot(_ == "KAIST")
          )
          val univSet = univss.flatten.toSet
          univSet
            .map(name => variance(univss.map(univs => univs.count(_ == name))))
            .sum
        }
        val sessions = sessionss.minBy(univVar)(Ordering.Double.TotalOrdering)

        // making groups
        val groups = sessions.zipWithIndex.map { case (s, i) =>
          val groups =
            List.fill(2)(new Session(conf, s.getApplicants.length / 2))
          register(s.accepted, groups)
          (i + 1) -> (groups.zipWithIndex.map { case (g, j) =>
            j + 1 -> g
          }.toMap)
        }.toMap

        // not accepted
        val allAccepted = sessions.flatMap(_.accepted)
        val notAccepted =
          (accepts ++ intermediates).filterNot(allAccepted.contains(_))

        // writing to xlsx
        val file = pickDir.getAbsolutePath + File.separator + "sessions.xlsx"
        ExcelUtil.writeWorkbook(
          file,
          wb => {
            ExcelUtil.writeSheet(
              wb,
              "참가자",
              sheet => {
                val st = ExcelUtil.createStyle(wb, 230, 230, 230)
                for (i <- groups.keys.toList.sorted) {
                  val m = groups(i)
                  for (j <- m.keys.toList.sorted) {
                    val g = m(j)
                    sheet.write(List(s"${i}분반", s"${j}그룹"), st)
                    for (
                      s <- g.accepted.sortBy(a => (a.university, a.ent, a.name))
                    ) {
                      sheet.write(s.info)
                    }
                  }
                }
              }
            )
            ExcelUtil.writeSheet(
              wb,
              "예비 후보",
              sheet => {
                val waits = MMap[Applicant, Int]()
                def updateWaits(a: Applicant, sc: Int): Unit =
                  waits.get(a) match {
                    case Some(n) => waits.update(a, n + sc)
                    case None    => waits += (a -> sc)
                  }
                sessions.foreach(s =>
                  for (a <- s.accepted)
                    s.withoutDo(a) {
                      notAccepted
                        .map(aa => (aa, s.scoreIncrease(aa)))
                        .groupBy(_._2)
                        .toList
                        .sortBy(-_._1)
                        .head
                        ._2
                        .foreach(p => updateWaits(p._1, 1))
                    }
                )
                waits.toList.groupBy(_._2).toList.sortBy(-_._1).foreach {
                  case (_, l) =>
                    val (s, score) :: _ = l
                    sheet.write(s.info :+ score.toString)
                }
              }
            )
            ExcelUtil.writeSheet(
              wb,
              "불합격",
              sheet => notAccepted.foreach(s => sheet.write(s.info))
            )
          }
        )

        // uploading to Google Drive
        val id = GoogleDriveUtil.uploadExcel(
          service,
          parent,
          "CS496: 20xx 여름/겨울 그룹 편성(전체)",
          new File(file)
        )
        Process(s"open https://docs.google.com/spreadsheets/d/$id").!

        // statistics
        Json.toJson(
          ("전체" -> Statistics.create(allAccepted)) +:
            sessions
              .map(s => Statistics.create(s.accepted))
              .zipWithIndex
              .map { case (t, i) =>
                s"${i + 1}분반" -> t
              } :+
            ("합격률" -> (
              List("대학", "지원", "합격", "비율") +:
                intermediates.groupBy(_.university).toList.sortBy(_._1).map {
                  case (univ, l) =>
                    val total = l.length
                    val accepted = l.count(allAccepted.contains)
                    val ratio = accepted.toDouble / total
                    List(
                      univ,
                      total.toString,
                      accepted.toString,
                      f"$ratio%1.3f"
                    )
                }
            ))
        )
      }

    case r @ GET(Path("/delete")) =>
      run {
        def deleteAll(name: String) =
          service.files.list
            .setQ(s"name = \'$name\'")
            .execute()
            .getFiles
            .asScala
            .foreach(f => {
              service.files.delete(f.getId).execute()
              println(s"${f.getId} deleted")
            })
        deleteAll("CS496: 20xx 여름/겨울 그룹 편성(전체)")
        deleteAll("CS496: 20xx 여름/겨울 지원서.pdf")
        deleteAll("CS496: 20xx 여름/겨울 지원서(공유).pdf")
      }

    case _ => success(NotFound)
  }

  def getId(id: Seq[String]): String =
    decode(id.mkString, "UTF-8").split("/").maxBy(_.length)

  def getRows(name: String): List[Row] = {
    val workbook = WorkbookFactory.create(new File(name))
    val res = workbook.getSheetAt(0).iterator.asScala.toList
    workbook.close()
    res
  }

  def makeStudents() = {
    val head :: data = getRows(afile)
    val phead :: pdata = getRows(pfile)
    summaries = getRows(sfile)
    Student.initialize(head, phead, pdata, summaries)
    students = data.flatMap(Student.create).zipWithIndex.map { case (s, i) =>
      (s, i + 1)
    }
  }

  def printTex(share: Boolean): (String, String) = {
    val photoFiles =
      FileUtils
        .iterateFiles(photoDir, null, false)
        .asScala
        .map(s => {
          val n = s.getName
          nameWithoutExtension(n).toInt -> s.getAbsolutePath
        })
        .toMap
    val texContent =
      students
        .sortBy(_._1.university)
        .map { case (s, i) =>
          Tex.mkChapter(s, photoFiles(i), i, share, summaries)
        }
        .mkString(
          Tex.start("KoPubWorld돋움체_Pro"),
          "\n\n",
          Tex.end
        )

    val ns = if (share) "share" else "internal"
    val name = s"applicants-$ns"
    val texFile = texDir.getAbsolutePath + File.separator + name + ".tex"
    val pdfFile = texDir.getAbsolutePath + File.separator + name + ".pdf"
    val texWriter = new PrintWriter(new File(texFile))
    texWriter.print(texContent)
    texWriter.close()

    (name, pdfFile)
  }

  def buildTex(name: String, pdf: String) = {
    Process(s"xelatex $name", texDir).!
    Process(s"xelatex $name", texDir).!
    Process(s"xelatex $name", texDir).!
    Process(s"open $pdf").!
  }

  def register(a: Seq[Applicant], s: List[Session]): Unit = {
    val addable = s.filter(_.canAdd)
    if (addable.isEmpty || a.isEmpty) return

    val min = addable.minBy(_.score)
    val scores =
      for (applicant <- a) yield (applicant, min.scoreIncrease(applicant))
    val grouped = scores.groupBy(_._2)
    val maxStudents = grouped.maxBy(_._1)._2.map(_._1)
    val univs = maxStudents.map(_.university).toSet
    val univMap = s
      .flatMap(_.getApplicants)
      .map(_.university)
      .groupBy(x => x)
      .map { case (univ, l) => univ -> l.length }
    val sortedUnivs = univs
      .map(u => u -> univMap.getOrElse(u, 0))
      .toList
      .sortBy(_._2)
    val max = sortedUnivs
      .map(_._1)
      .flatMap(u => maxStudents.find(_.university == u))
      .head

    min.addApplicant(max)
    register(a.filterNot(_ == max), s)
  }

  def variance(l: Seq[Int]): Double = {
    val ave = l.sum.toDouble / l.length
    l.map(v => {
      val d = ave - v
      d * d
    }).sum / l.length
  }

  def run(x: => Unit) =
    try {
      x
      success(
        ResponseHeader("Content-Type", List("application/json")) ~>
          ResponseString("{\"success\": true}")
      )
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        success(
          ResponseHeader("Content-Type", List("application/json")) ~>
            ResponseString("{\"success\": false}")
        )
    }

  def runJson(v: => JsValue) =
    try {
      success(
        ResponseHeader("Content-Type", List("application/json")) ~>
          ResponseString(s"""{"success": true, "result": ${v.toString}}""")
      )
    } catch {
      case e: Throwable =>
        e.printStackTrace()
        success(
          ResponseHeader("Content-Type", List("application/json")) ~>
            ResponseString("{\"success\": false}")
        )
    }

  private def nameWithoutExtension(name: String): String =
    name.substring(0, name.lastIndexOf("."))
}
