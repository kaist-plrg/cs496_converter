package io.madcamp.utils

import org.apache.poi.ss.usermodel.Row
import scala.jdk.CollectionConverters._

case class Student(
  name: String,
  photo: String,
  tableInfo: List[(String, String)],
  parInfo: List[(String, String)]
) {
  val tableMap = tableInfo.toMap
  val parMap = parInfo.toMap
}

object Student {

  private[this] var nameIndex = -1
  private[this] var photoIndex = -1
  private[this] var tableInfo: List[(String, Int)] = null
  private[this] var parInfo: List[(String, Int)] = null
  private[this] var prevMap: Map[(String, String), String] = null

  def create(row: Row): Option[Student] = {
    val name = ExcelUtil.getString(row, nameIndex)
    if (name.nonEmpty) {
      val photo = ExcelUtil.getString(row, photoIndex).substring(prefixLen)
      val table =
        tableInfo.map{ case (s, i) => (s, ExcelUtil.getString(row, i)) }
      val birthday = table.find{ case (f, _) => f == "생년월일" }.get._2
      val prev = prevMap.get((name, birthday)).getOrElse("해당 없음")
      val par = ("이전 지원 여부" -> prev) +:
        parInfo.map{ case (s, i) => (s, ExcelUtil.getString(row, i)) }
      Some(Student(name, photo, table, par))
    } else None
  }

  def initialize(
    row: Row, prow: Row, pdata: List[Row], summaries: List[Row]
  ): Unit = {
    val tableEntries = ExcelUtil.getStrings(summaries(0))
    val parEntries = ExcelUtil.getStrings(summaries(2))

    val colNames: List[String] = row.iterator.asScala.map(c => {
      val s = ExcelUtil.getString(c)
      s.replaceAll("\\d+\\. ", "").replaceAll("\\d-", "")
    }).toList.takeWhile(_.nonEmpty)

    nameIndex = colNames.indexWhere(_.contains("이름"))
    photoIndex = colNames.indexWhere(_.contains("사진"))

    tableInfo = tableEntries.map(s => s -> colNames.indexOf(s))
    tableInfo.filter{ case (_, i) => i == -1 }.foreach{
      case (s, _) => sys.error(s"$s not found")
    }

    parInfo = parEntries.map(s => s -> colNames.indexOf(s))
    parInfo.filter{ case (_, i) => i == -1 }.foreach{
      case (s, _) => sys.error(s"$s not found")
    }

    val pNames = prow.iterator.asScala.toList.map(ExcelUtil.getString)
    val nIndex = pNames.indexWhere(_.contains("이름"))
    val bIndex = pNames.indexWhere(_.contains("생년"))
    val pIndex = pNames.indexWhere(_.contains("지원년도"))
    prevMap = pdata.map(r => (
      (ExcelUtil.getString(r, nIndex), ExcelUtil.getString(r, bIndex)),
      ExcelUtil.getString(r, pIndex)
    )).groupBy(_._1).map{
      case (k, l) => k -> l.map(_._2).mkString(", ")
    }.toMap
  }

  val prefixLen = "https://drive.google.com/open?id=".length
}
