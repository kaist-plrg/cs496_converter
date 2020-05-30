package io.madcamp.apptopdf

import org.apache.poi.ss.usermodel.{Row, CellType, Cell}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.{File, FileOutputStream}
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
    }).trim.filterNot(_.isControl)
  }

  def getString(row: Row, col: Int): String = getString(row.getCell(col))

  def writeWorkbook(path: String, func: XSSFWorkbook => Unit): Unit = {
    val wb = new XSSFWorkbook
    func(wb)
    val out = new FileOutputStream(new File(path))
    wb.write(out)
    wb.close()
    out.close()
  }

}
