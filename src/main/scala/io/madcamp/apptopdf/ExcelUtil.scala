package io.madcamp.apptopdf

import org.apache.poi.ss.usermodel.{Row, CellType, Cell}
import scala.jdk.CollectionConverters._

object ExcelUtil {

  def getStrings(row: Row): List[String] =
    row.iterator.asScala.toList.map(getString)

  def getString(c: Cell): String = {
    if (c == null) ""
    else (c.getCellType match {
      case CellType.NUMERIC =>
        c.getNumericCellValue.toLong.toString
      case CellType.FORMULA =>
        c.getCellFormula
      case _ => c.getStringCellValue
    }).trim
  }

  def getString(row: Row, col: Int): String = getString(row.getCell(col))

}
