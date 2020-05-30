package io.madcamp.apptopdf

import unfiltered.filter.Plan
import unfiltered.request._
import unfiltered.response._
import unfiltered.directives._, Directives._
import org.apache.commons.io.FileUtils
import org.apache.poi.ss.usermodel.{WorkbookFactory, Row}
import java.io.{File, PrintWriter}
import java.net.URLDecoder.decode
import scala.collection.parallel.CollectionConverters._
import scala.jdk.CollectionConverters._
import scala.sys.process.Process
import scala.util.Random
import scala.io.Source
import scala.collection.mutable.{Map => MMap}

class ServerPlan extends Plan {

  val out = "out"
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

  val service = GoogleDriveUtil.service("credential.json")

  var students: List[(Student, Int)] = null
  var photoFiles: Map[Int, String] = null
  var summaries: List[Row] = null
  var sessions: List[Session] = null
  var groups: Map[Int, Map[Int, Session]] = null
  var notAccepted: List[Applicant] = null

  def intent = Directive.Intent {
    case r @ GET(Path("/")) =>
      success(
        HtmlContent ~> ResponseString(Source.fromFile("index.html").mkString)
      )
    case r @ GET(Path("/index.js")) =>
      success(
        ResponseHeader("Content-Type", List("text/javascript")) ~>
        ResponseString(Source.fromFile("index.js").mkString)
      )

    case r @ GET(Path("/downloads") & Params(params)) => run {
      val aid = getId(params("aid"))
      val pid = getId(params("pid"))
      val sid = getId(params("sid"))
      FileUtils.cleanDirectory(sheetDir)
      GoogleDriveUtil.downloadExcel(service, aid, afile)
      GoogleDriveUtil.downloadExcel(service, pid, pfile)
      GoogleDriveUtil.downloadExcel(service, sid, sfile)
    }

    case r @ GET(Path("/students")) => run {
      makeStudents()
    }

    case r @ GET(Path("/photos") & Params(params)) => run {
      FileUtils.cleanDirectory(photoDir)
      students.par.foreach{
        case (s, i) =>
          GoogleDriveUtil.downloadPhoto(service, s.photo, i, photoDir)
      }
      photoFiles =
        FileUtils.iterateFiles(photoDir, null, false).asScala.map(
          s => {
            val n = s.getName
            nameWithoutExtension(n).toInt -> s.getAbsolutePath
          }
        ).toMap
      success(Ok)
    }

    case r @ GET(Path("/tex")) => run {
      FileUtils.cleanDirectory(texDir)
      printTex(false)
      printTex(true)
    }

    case r @ GET(Path("/evals") & Params(params)) => run {
      val eid = getId(params("eid"))
      val cid = getId(params("cid"))
      GoogleDriveUtil.downloadExcel(service, eid, efile)
      GoogleDriveUtil.downloadExcel(service, cid, cfile)
    }

    case r @ GET(Path("/pick") & Params(params)) => run {
      if (summaries == null)
        makeStudents()
      val seed = params("seed").mkString
      val size = params("size").mkString.toInt
      val gsize = params("gsize").mkString.toInt
      val numOfSessions = params("num").mkString.toInt

      val head :: _data = getRows(efile)
      val configs = getRows(cfile).map(ExcelUtil.getString(_, 0))
        .takeWhile(_.nonEmpty)
      Applicant.initialize(head, summaries(5), summaries(6))
      val conf = new PickConfig(configs)

      val data = _data.filter(r => ExcelUtil.getString(r, 0).nonEmpty)
      if (students.length != data.length)
        sys.error(s"${students.length} != ${data.length}")
      val origApplicants = (students zip data).map{
        case ((s, _), r) => Applicant(s, r)
      }
      val applicants = origApplicants.filterNot(a => a.coding == "하" && a.cooperation == "하")
      val byAccept = applicants.groupBy(_.accept)
      val accepts = byAccept("O")
      val intermediates = byAccept("?")
      val (mintermediates, nintermediates) = intermediates.partition(_.motiv)

      val iter = 10
      val sessionss = for (i <- 1 to iter) yield {
        val sessions = List.fill(numOfSessions)(new Session(conf, size))
        val rand = new Random((seed + i).##)

        register(rand.shuffle(accepts), sessions)
        register(rand.shuffle(mintermediates), sessions)
        register(rand.shuffle(nintermediates), sessions)

        sessions
      }

      def univVar(sessions: List[Session]): Double = {
        val univss = sessions.map(_.getApplicants.map(_.university).filterNot(_ == "KAIST"))
        val univSet = univss.flatten.toSet
        univSet.map(name =>
          variance(univss.map(univs =>
            univs.count(_ == name)
          ))
        ).sum
      }

      sessions = sessionss.minBy(univVar)(Ordering.Double.TotalOrdering)
      groups = sessions.zipWithIndex.map{
        case (s, i) =>
          val groups = List.fill(size / gsize)(new Session(conf, gsize))
          register(s.accepted, groups)
          (i + 1) -> (groups.zipWithIndex.map{
            case (g, j) => j + 1 -> g
          }.toMap)
      }.toMap
      val allAccepted = sessions.flatMap(_.accepted)
      notAccepted = (accepts ++ intermediates).filterNot(allAccepted.contains(_))
    }

    case r @ GET(Path("/sessions")) => run {
      val file = pickDir.getAbsolutePath + File.separator + "sessions.xlsx"
      ExcelUtil.writeWorkbook(file, wb => {
        val sheet = wb.createSheet("참가자")
        def write(r: Row, i: Int, s: String): Unit = {
          val c = r.createCell(i)
          c.setCellValue(s)
        }

        val header = sheet.createRow(0)
        Applicant.headerTitles.zipWithIndex.foreach{
          case (h, i) => write(header, i, h)
        }

        var rn = 1
        for (i <- groups.keys.toList.sorted) {
          val m = groups(i)
          for (j <- m.keys.toList.sorted) {
            val g = m(j)
            val gh = sheet.createRow(rn)
            rn += 1
            write(gh, 0, s"${i}분반 / ${j}그룹")
            for (s <- g.accepted.sortBy(a => (a.university, a.ent, a.name))) {
              val r = sheet.createRow(rn)
              s.info.zipWithIndex.foreach{
                case (s, i) => write(r, i, s)
              }
              rn += 1
            }
          }
        }
      })
    }

    case r @ GET(Path("/waits")) => run {
      val file = pickDir.getAbsolutePath + File.separator + "waits.xlsx"

      val waits = MMap[Applicant, Int]()
      def updateWaits(a: Applicant, sc: Int): Unit = waits.get(a) match {
        case Some(n) => waits.update(a, n + sc)
        case None => waits += (a -> sc)
      }
      sessions.foreach(s =>
        for (a <- s.accepted)
          s.withoutDo(a){
            val sorted = (for (aa <- notAccepted) yield (aa, s.scoreIncrease(aa))).sortBy(-_._2)
            updateWaits(sorted(0)._1, 2);
            updateWaits(sorted(1)._1, 1);
          }
      )
      ExcelUtil.writeWorkbook(file, wb => {
        val sheet = wb.createSheet("대기자")
        def write(r: Row, i: Int, s: String): Unit = {
          val c = r.createCell(i)
          c.setCellValue(s)
        }

        val header = sheet.createRow(0)
        (Applicant.headerTitles :+ "점수").zipWithIndex.foreach{
          case (h, i) => write(header, i, h)
        }

        waits.toList.sortBy(-_._2).zipWithIndex.foreach{
          case ((s, score), rn) =>
            val r = sheet.createRow(rn + 1)
            (s.info :+ score.toString).zipWithIndex.foreach{
              case (s, i) => write(r, i, s)
            }
        }
      })
    }

    case r @ GET(Path("/rejects")) => run {
      val file = pickDir.getAbsolutePath + File.separator + "rejects.xlsx"
      ExcelUtil.writeWorkbook(file, wb => {
        val sheet = wb.createSheet("탈락자")
        def write(r: Row, i: Int, s: String): Unit = {
          val c = r.createCell(i)
          c.setCellValue(s)
        }

        val header = sheet.createRow(0)
        Applicant.headerTitles.zipWithIndex.foreach{
          case (h, i) => write(header, i, h)
        }

        notAccepted.zipWithIndex.foreach{
          case (s, rn) =>
            val r = sheet.createRow(rn + 1)
            s.info.zipWithIndex.foreach{
              case (s, i) => write(r, i, s)
            }
        }
      })
    }

    case r @ GET(Path("/stats")) => run {
      val file = pickDir.getAbsolutePath + File.separator + "stats.xlsx"
      ExcelUtil.writeWorkbook(file, wb => {
        val sheet = wb.createSheet("통계")
        def write(r: Row, i: Int, s: String): Unit = {
          val c = r.createCell(i)
          c.setCellValue(s)
        }

        var rn = 0
        for (i <- groups.keys.toList.sorted) {
          val m = groups(i)
          val r = sheet.createRow(rn)
          rn += 1

          write(r, 0, s"${i}분반")
          sessions(i - 1).statistics.map(_.split(",")).foreach{
            case arr =>
              val r = sheet.createRow(rn)
              rn += 1
              arr.zipWithIndex.foreach{
                case (s, i) => write(r, i, s)
              }
          }

          for (j <- m.keys.toList.sorted) {
            val r = sheet.createRow(rn)
            rn += 1
            write(r, 0, s"${i}분반 ${j}그룹")
            m(j).statistics.map(_.split(",")).foreach{
              case arr =>
                val r = sheet.createRow(rn)
                rn += 1
                arr.zipWithIndex.foreach{
                  case (s, i) => write(r, i, s)
                }
            }
          }
        }
      })
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
    students = data.flatMap(Student.create).zipWithIndex.map{
      case (s, i) => (s, i + 1)
    }
  }

  def printTex(share: Boolean): Unit = {
    val texContent =
      students.map{
        case (s, i) => Tex.mkChapter(s, photoFiles(i), i, share, summaries)
      }.mkString(
        Tex.start("KoPubWorld돋움체_Pro"), "\n\n", Tex.end
      )

    val ns = if (share) "share" else "internal"
    val name = s"applicants-$ns"
    val texFile = texDir.getAbsolutePath + File.separator + name + ".tex"
    val texWriter = new PrintWriter(new File(texFile))
    texWriter.print(texContent)
    texWriter.close()

    Process(s"xelatex $name", texDir).!
    Process(s"xelatex $name", texDir).!
    Process(s"xelatex $name", texDir).!
    Process(s"open $name.pdf", texDir).!
  }

  def register(a: Seq[Applicant], s: List[Session]): Unit = {
    val addable = s.filter(_.canAdd)
    if (addable.isEmpty || a.isEmpty) return

    val min = addable.minBy(_.score)
    val scores = for (applicant <- a) yield (applicant, min.scoreIncrease(applicant))
    val (max, _) = scores.maxBy(_._2)

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

  private def nameWithoutExtension(name: String): String =
    name.substring(0, name.lastIndexOf("."))

//     val applicantBy = applicants.groupBy(_.university)
//     val acceptedBy = allAccepted.groupBy(_.university)
//     println("===Application===")
//     println(applicantBy.map(a => s"${a._1}\t${a._2.length}").toList.sorted.mkString("\n"))
//     println("===Accepted===")
//     println(acceptedBy.map(a => s"${a._1}\t${a._2.length}").toList.sorted.mkString("\n"))
//     println("===Ratio===")
//     println(applicantBy.keys.map(k => s"$k\t${acceptedBy.getOrElse(k, Nil).length.toDouble / applicantBy(k).toList.length}").toList.sorted.mkString("\n"))
//     println()

//     val qApplicantBy = intermediates.groupBy(_.university)
//     val qAcceptedBy = allAccepted.filter(_.accept == "?").groupBy(_.university)
//     println("===?Application===")
//     println(qApplicantBy.map(a => s"${a._1}\t${a._2.length}").toList.sorted.mkString("\n"))
//     println("===Accepted===")
//     println(qAcceptedBy.map(a => s"${a._1}\t${a._2.length}").toList.sorted.mkString("\n"))
//     println("===?Ratio===")
//     println(qApplicantBy.keys.map(k => s"$k\t${qAcceptedBy.getOrElse(k, Nil).length.toDouble / qApplicantBy(k).toList.length}").toList.sorted.mkString("\n"))
}