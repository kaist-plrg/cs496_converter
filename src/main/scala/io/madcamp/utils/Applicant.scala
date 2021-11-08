package io.madcamp.utils

import org.apache.poi.ss.usermodel.Row
import scala.jdk.CollectionConverters._

case class Applicant(student: Student, row: Row) {

  require(student.name == ExcelUtil.getString(row, Applicant.nameIndex))

  val name: String = student.name
  val phone: String = student.tableMap(Applicant.colNames(0))
  val mail: String = student.tableMap(Applicant.colNames(1))
  val university: String = student.tableMap(Applicant.colNames(2))
  val isKaist: Boolean = university == "KAIST"
  val isAbroad: Boolean = Applicant.abroadUnivs contains university
  val ent: String = student.tableMap(Applicant.colNames(3))
  val birth: Int = student.tableMap(Applicant.colNames(4)).toInt
  val isMale: Boolean = student.tableMap(Applicant.colNames(5)) == "남자"
  val isMilitary: Boolean = student.tableMap(Applicant.colNames(6)) == "병역필"
  val major: String = student.tableMap(Applicant.colNames(7))
  val isRepeat: Boolean = !student.parMap(Applicant.colNames(8)).contains("없음")

  val coding: String = ExcelUtil.getString(row, Applicant.evalIndices(0))
  val cooperation: String = ExcelUtil.getString(row, Applicant.evalIndices(1))
  val motiv: Boolean = ExcelUtil.getString(row, Applicant.evalIndices(2)) == "O"
  val accept: String = ExcelUtil.getString(row, Applicant.evalIndices(3))
  val etc: String = ExcelUtil.getString(row, Applicant.evalIndices(4))

  override def toString: String = {
    val g = if (isMale) "남" else "여"
    val ma = major.replace(',', ' ')
    val mi = if (isMilitary) "병역필" else "-"
    val r = if (isRepeat) "재수" else "-"
    val a = if (isAbroad) "해외" else "국내"
    s"${name},${university},${g},${ent}학번,${ma},${mail},${phone},${birth},${mi},${r},${a},${coding},${cooperation},${motiv},${accept},${etc}"
  }

  def info: List[String] =
    List(
      name,
      university,
      if (isMale) "남" else "여",
      s"${ent}학번",
      major.replace(',', ' '),
      mail,
      phone,
      birth.toString,
      if (isMilitary) "병역필" else "-",
      if (isRepeat) "재수" else "-",
      if (isAbroad) "해외" else "국내",
      coding,
      cooperation,
      if (motiv) "O" else "X",
      accept,
      etc
    )
}

object Applicant {
  val abroadUnivs = Set(
    "The University of Melbourne",
    "University of Queensland",
    "University of Waterloo",
    "University of Toronto St.George",
    "University of Cambridge"
  )

  var nameIndex: Int = -1
  var colNames: List[String] = null
  var evalIndices: List[Int] = null

  def initialize(row: Row, cols1: Row, cols2: Row): Unit = {
    colNames = cols1.iterator.asScala.toList.map(ExcelUtil.getString)
    val headers = row.iterator.asScala.toList.map(ExcelUtil.getString)
    nameIndex = headers.indexWhere(_.contains("이름"))
    val colNames2 = cols2.iterator.asScala.toList
      .map(ExcelUtil.getString)
      .takeWhile(_.nonEmpty)
    evalIndices = colNames2.map(headers.indexOf)
    evalIndices.zipWithIndex.filter { case (i, ind) => i == -1 }.foreach {
      case (_, ind) => sys.error(s"${colNames2(ind)} not found")
    }
  }

  val headerTitles = List(
    "이름",
    "학교",
    "성별",
    "학번",
    "전공",
    "이메일",
    "전화번호",
    "생년월일",
    "병역",
    "재수",
    "대학",
    "코딩",
    "팀웍",
    "다양성",
    "합격",
    "비고"
  )
}
