package io.madcamp.utils

import org.apache.poi.xssf.usermodel.{XSSFColor, DefaultIndexedColorMap}
import org.apache.poi.ss.usermodel.{Row, CellType, Cell, Sheet, CellStyle}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
import java.io.{File, FileOutputStream}
import java.awt.Color
import scala.jdk.CollectionConverters._
import scala.collection.mutable.{Map => MMap}

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

  def writeSheet(
    wb: XSSFWorkbook, name: String, func: SheetWrapper => Unit
  ): Unit = {
    val sheet = wb.createSheet(name)
    val wrapper = new SheetWrapper(sheet)
    val st = createStyle(wb, 200, 200, 200)
    wrapper.write(Applicant.headerTitles, st)
    func(wrapper)
    wrapper.setWidth(sheet)
  }

  def createStyle(wb: XSSFWorkbook, r: Int, g: Int, b: Int): CellStyle = {
    val st = wb.createCellStyle
    st.setFillPattern(SOLID_FOREGROUND);
    st.setFillForegroundColor(
      new XSSFColor(new Color(r, g, b), new DefaultIndexedColorMap))
    st
  }
}

class SheetWrapper(sheet: Sheet){

  private val map = MMap.empty[Int, Int]
  private var rowNumber = 0

  def write(r: Row, i: Int, s: String): Unit = {
    val c = r.createCell(i)
    c.setCellValue(s)
    map(i) = map.getOrElse(i, 0) max s.map(
      c => if (c < 128) 1 else 2
    ).sum
  }

  def write(r: Row, i: Int, s: String, st: CellStyle): Unit = {
    val c = r.createCell(i)
    c.setCellValue(s)
    c.setCellStyle(st)
    map(i) = map.getOrElse(i, 0) max s.map(
      c => if (c < 128) 1 else 2
    ).sum
  }

  def write(ss: List[String]): Unit = {
    val r = newRow()
    ss.zipWithIndex.foreach{
      case (s, i) => write(r, i, s)
    }
  }

  def write(ss: List[String], st: CellStyle): Unit = {
    val r = newRow()
    r.setRowStyle(st)
    ss.zipWithIndex.foreach{
      case (s, i) => write(r, i, s, st)
    }
  }

  def setWidth(sheet: Sheet): Unit =
    map.foreach{
      case (i, l) =>
        sheet.setColumnWidth(i, (l * 256 + 256) min 12800)
    }

  def newRow(): Row = {
    val row = sheet.createRow(rowNumber)
    rowNumber += 1
    row
  }
}
