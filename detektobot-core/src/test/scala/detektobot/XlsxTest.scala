package detektobot

import java.io.FileOutputStream

import detektobot.adt.FoundCode
import org.apache.poi.ss.usermodel.{BorderStyle, HorizontalAlignment}
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class XlsxTest extends AnyFunSuite with Matchers with ScalaCheckPropertyChecks {

  val blocks = List(
    FoundCode(1, "aaa", "Doo", "Joo", "20.02.2020"),
    FoundCode(2, "bbb", "Boo", "Foo", "21.02.2020"),
    FoundCode(3, "ccc", "Doo", "Goo", "22.02.2020"),
    FoundCode(4, "ddd", "Joo", "Aoo", "23.02.2020"),
  )

  val packs = List(
    FoundCode(1, "aaa", "Doo", "Joo", "20.02.2020"),
    FoundCode(2, "bbb", "Boo", "Foo", "21.02.2020"),
    FoundCode(3, "ccc", "Doo", "Goo", "22.02.2020"),
    FoundCode(4, "ddd", "Joo", "Aoo", "23.02.2020"),
  )

  def createSheet(title: String, items: List[FoundCode], workbook: XSSFWorkbook) = {
    val font = workbook.createFont()
    font.setFontName("Arial")
    font.setFontHeightInPoints(12)
    font.setBold(true)

    val hStyle = workbook.createCellStyle()
    hStyle.setFont(font)
    hStyle.setAlignment(HorizontalAlignment.CENTER)
    hStyle.setBorderTop(BorderStyle.THIN)
    hStyle.setBorderLeft(BorderStyle.THIN)
    hStyle.setBorderRight(BorderStyle.THIN)
    hStyle.setBorderBottom(BorderStyle.THIN)

    val cStyle = workbook.createCellStyle()
    cStyle.setWrapText(true)
    cStyle.setBorderTop(BorderStyle.THIN)
    cStyle.setBorderLeft(BorderStyle.THIN)
    cStyle.setBorderRight(BorderStyle.THIN)
    cStyle.setBorderBottom(BorderStyle.THIN)

    val sheet = workbook.createSheet(title)
    sheet.setColumnWidth(0, 256 * 50)
    sheet.setColumnWidth(1, 256 * 10)
    sheet.setColumnWidth(2, 256 * 20)

    val header = sheet.createRow(0)

    List("Код", "Нашёл", "Дата").zipWithIndex.foreach { case (t, idx) =>
      val hcell = header.createCell(idx)
      hcell.setCellValue(t)
      hcell.setCellStyle(hStyle)
    }

    items.zipWithIndex.foreach { case (item, idx) =>
      val row = sheet.createRow(idx + 1)

      val codeCell = row.createCell(0)
      codeCell.setCellValue(item.code)
      codeCell.setCellStyle(cStyle)

      val foundByCell = row.createCell(1)
      foundByCell.setCellValue(item.firstName + " " + item.lastName)
      foundByCell.setCellStyle(cStyle)

      val foundAtCell = row.createCell(2)
      foundAtCell.setCellValue(item.foundAt)
      foundAtCell.setCellStyle(cStyle)
    }

  }

  def createWorkbook(blocks: List[FoundCode], packs: List[FoundCode], file: String): Unit = {
    val workbook = new XSSFWorkbook()
    createSheet("Блоки", blocks, workbook)
    createSheet("Коробки", packs, workbook)
    val out = new FileOutputStream(file)
    workbook.write(out)
    workbook.close()
  }

  test("XLSX Creation") {
    createWorkbook(blocks, packs, "/tmp/report.xlsx")
  }

}
