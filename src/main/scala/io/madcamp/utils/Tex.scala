package io.madcamp.utils

import org.apache.poi.ss.usermodel.Row

object Tex {

  def mkChapter(
      s: Student,
      photo: String,
      index: Int,
      share: Boolean,
      summaries: List[Row]
  ): String = {
    val r0 :: r1 :: r2 :: r3 :: r4 :: _ = summaries
    val tableMap = (ExcelUtil.getStrings(r0) zip ExcelUtil.getStrings(r1)).toMap
    val parMap = (ExcelUtil.getStrings(r2) zip ExcelUtil.getStrings(r3)).toMap
    val internal = ExcelUtil.getStrings(r4).toSet
    Tex.mkIntro(s.name, photo, index, share) + "\n" +
      Tex.mkTable(s.tableInfo, share, tableMap, internal) + "\n" +
      Tex.mkPar(s.parInfo, parMap)
  }

  private def mkIntro(
      name: String,
      photo: String,
      index: Int,
      share: Boolean
  ): String = {
    val n = if (!share) name else s"익명의 지원자 $index"
    s"""\\chapter{$n}
       |\\begin{center}
       |\\begin{tabular}{@{}c@{}}
       |  \\includegraphics[width=0.40\\textwidth,height=0.18\\textheight,keepaspectratio]{$photo}
       |\\end{tabular}""".stripMargin
  }

  private def mkTable(
      tableInfo: List[(String, String)],
      share: Boolean,
      tableMap: Map[String, String],
      internal: Set[String]
  ): String = {
    def tableEntry(field: String, content: String): String =
      if (content.contains("https://"))
        s"\\href{$content}{$field}"
      else
        s"\\text{${escape(content + field)}}"
    val header =
      "\\begin{tabular}{|C{0.15\\textwidth}|C{0.37\\textwidth}|} \\hline\n"
    val footer = "\\\\ \\hline\n\\end{tabular}\n\\end{center}"
    val info =
      if (!share) tableInfo
      else tableInfo.filter { case (f, _) => !internal(f) }
    val body =
      info
        .map { case (f, c) => tableEntry(tableMap.getOrElse(f, ""), c) }
        .sliding(2, 2)
        .map(l => l.mkString(" & "))
        .mkString(" \\\\ \\hline\n")
    header + body + footer
  }

  private def mkPar(
      parInfo: List[(String, String)],
      parMap: Map[String, String]
  ): String = {
    def parEntry(field: String, _content: String): String = {
      val content = if (_content.forall(_ == ' ')) "-" else _content
      s"\\noindent\n{\\bf- ${escape(field)}}\n\n\\noindent\n${escape(content)}"
    }
    parInfo
      .map { case (f, c) =>
        parEntry(parMap.getOrElse(f, f), c)
      }
      .mkString(" \\\\[1em]\n")
  }

  private def escape(raw: String): String = {
    val esc = "_$%#&{}".toSet
    val builder = new StringBuilder
    for (ch <- raw)
      if (esc(ch)) builder += '\\' += ch
      else if (ch == '~') builder += '-'
      else if (ch == '^') builder ++= "\\textsuperscript{$\\wedge$}"
      else builder += ch
    builder.toString.replaceAll("\n", "\n\n")
  }

  def start(font: String) =
    s"""%!TEX encoding = UTF-8 Unicode
\\documentclass[a4paper,12pt]{book}
\\usepackage[margin=0.5in]{geometry}
\\usepackage[T1]{fontenc}
\\usepackage[utf8]{inputenc}
\\usepackage[hangul]{xetexko}
\\usepackage[english]{babel}
\\setmainfont{$font}
\\setmainhangulfont{$font}
\\setmainhanjafont{$font}
\\setsansfont{$font}
\\setsanshangulfont{$font}
\\setsanshanjafont{$font}
\\setmonofont{$font}
\\setmonohangulfont{$font}
\\setmonohanjafont{$font}
\\linespread{1.2}
\\usepackage{graphicx}
\\usepackage{amsmath}
\\usepackage{hyperref}
\\usepackage{array}
\\usepackage{textcomp}
\\usepackage{verbatim}
\\hypersetup{
    colorlinks,
    citecolor=black,
    filecolor=black,
    linkcolor=black,
    urlcolor=black
}
\\usepackage{sectsty,fancyhdr}
\\usepackage{titlesec}
\\titleformat{\\chapter}
  {\\normalfont\\LARGE\\bfseries}{\\thechapter}{1em}{}
\\titlespacing*{\\chapter}{0em}{-3em}{0em}
\\pagestyle{fancy}
\\fancyhf{}
\\lhead{\\leftmark}
\\rhead{Page \\thepage}
\\renewcommand{\\headrulewidth}{1.0pt}
\\renewcommand{\\footrulewidth}{1.0pt}
\\lfoot{\\leftmark}
\\rfoot{Page \\thepage}
\\makeatletter
\\renewcommand{\\chaptermark}[1]{%
  \\markboth{\\ifnum \\c@secnumdepth>\\z@
      \\thechapter\\hskip 1em\\relax
    \\fi #1}{}}
\\makeatother
\\newcolumntype{C}[1]{>{\\centering\\arraybackslash}p{#1}}
\\begin{document}
\\setcounter{tocdepth}{1}
\\tableofcontents
\\newpage
\\small
"""
  val end = "\n\\end{document}"
}
