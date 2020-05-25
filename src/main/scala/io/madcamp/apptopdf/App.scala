package io.madcamp.apptopdf

import org.apache.poi.ss.usermodel.{WorkbookFactory, Row}
import org.apache.commons.io.FileUtils
import java.io.{File, PrintWriter}
import scala.jdk.CollectionConverters._
import scala.collection.parallel.CollectionConverters._
import scala.sys.process.Process

object App {

  def main(args: Array[String]): Unit = args match {
    case Array(opt, config) =>
      val conf = Config.fromFile(config)

      val dir = new File(conf.out)
      val sheetDir = new File(conf.out + File.separator + "sheets")
      val photoDir = new File(conf.out + File.separator + "photos")
      val texDir = new File(conf.out + File.separator + "tex")
      if (!dir.exists) dir.mkdir()
      if (!sheetDir.exists) sheetDir.mkdir()
      if (!photoDir.exists) photoDir.mkdir()
      if (!texDir.exists) texDir.mkdir()

      val service = GoogleDriveUtil.service(conf.credentialFile)
      def getRows(id: String, name: String): List[Row] = {
        val file = new File(name)
        opt match {
          case "-d" =>
            FileUtils.cleanDirectory(sheetDir)
            GoogleDriveUtil.downloadExcel(service, id, name)
          case "-c" | "-n" =>
          case _ => sys.error("wrong option")
        }
        val workbook = WorkbookFactory.create(file)
        val res = workbook.getSheetAt(0).iterator.asScala.toList
        workbook.close()
        res
      }

      val afile = sheetDir.getAbsolutePath + File.separator + "applicants.xlsx"
      val pfile = sheetDir.getAbsolutePath + File.separator + "prevs.xlsx"
      val sfile = sheetDir.getAbsolutePath + File.separator + "summaries.xlsx"
      val head :: data = getRows(conf.applicationFileId, afile)
      val phead :: pdata = getRows(conf.previousFileId, pfile)
      val summaries = getRows(conf.summaryFileId, sfile)
      Student.initialize(head, phead, pdata, summaries)
      val students = data.flatMap(Student.create).zipWithIndex.map{
        case (s, i) => (s, i + 1)
      }

      opt match {
        case "-d" =>
          if (photoDir.exists) FileUtils.cleanDirectory(photoDir)
          students.par.foreach{
            case (s, i) =>
              GoogleDriveUtil.downloadPhoto(service, s.photo, i, photoDir)
          }
        case "-c" =>
          val existings = FileUtils.iterateFiles(photoDir, null, false)
            .asScala
            .toList
            .map(s => nameWithoutExtension(s.getName).toInt)
            .toSet
          students.filter{ case (_, i) => !existings(i) }.par.foreach{
            case (s, i) =>
              GoogleDriveUtil.downloadPhoto(service, s.photo, i, photoDir)
          }
        case "-n" =>
        case _ => sys.error("wrong option")
      }

      val photoFiles =
        FileUtils.iterateFiles(photoDir, null, false).asScala.map(
          s => {
            val n = s.getName
            nameWithoutExtension(n).toInt -> s.getAbsolutePath
          }
        ).toMap
      FileUtils.cleanDirectory(texDir)

      def printTex(share: Boolean): Unit = {
        val texContent =
          students.map{
            case (s, i) => Tex.mkChapter(s, photoFiles(i), i, share, summaries)
          }.mkString(
            Tex.start(conf.typeface), "\n\n", Tex.end
          )

        val ns = if (share) "share" else "internal"
        val name = s"applicants-$ns.tex"
        val texFile = texDir.getAbsolutePath + File.separator + name
        val texWriter = new PrintWriter(new File(texFile))
        texWriter.print(texContent)
        texWriter.close()

        Process(s"xelatex $name", texDir).!
        Process(s"xelatex $name", texDir).!
        Process(s"xelatex $name", texDir).!
      }
      printTex(false)
      printTex(true)

    case _ =>
      println("Usage:")
      println("  run -d|-c|-n [conf]")
  }

  private def nameWithoutExtension(name: String): String =
    name.substring(0, name.lastIndexOf("."))

}
