object Tex {
  val bodyStart =
"""%!TEX encoding = UTF-8 Unicode
\documentclass[a4paper,11pt]{book}
\usepackage[margin=0.5in]{geometry}
\usepackage[T1]{fontenc}
\usepackage[utf8]{inputenc}
\usepackage[cjk]{kotex}
\usepackage[english]{babel}
\usepackage{graphicx}
\usepackage{amsmath}
\usepackage{hyperref}
\usepackage{array}
\usepackage{textcomp}
\hypersetup{
    colorlinks,
    citecolor=black,
    filecolor=black,
    linkcolor=black,
    urlcolor=black
}
\usepackage{sectsty,fancyhdr}
\usepackage{titlesec}
\titlespacing*{\subsection} {0pt}{1.25ex plus 1ex minus .2ex}{0.5ex plus .2ex}
\renewcommand\familydefault{\sfdefault}
\chapterfont{\LARGE\bfseries}
\usepackage{titlesec}
\titleformat{\chapter}
  {\normalfont\LARGE\bfseries}{\thechapter}{1em}{}
\titlespacing*{\chapter}{0pt}{3.5ex plus 1ex minus .2ex}{2.3ex plus .2ex}
\pagestyle{fancy}
\fancyhf{}
\lhead{\leftmark}
\rhead{Page \thepage}
\renewcommand{\headrulewidth}{1.0pt}
\renewcommand{\footrulewidth}{1.0pt} 
\lfoot{\leftmark}
\rfoot{Page \thepage}
\makeatletter
\renewcommand{\chaptermark}[1]{%
  \markboth{\ifnum \c@secnumdepth>\z@
      \thechapter\hskip 1em\relax
    \fi #1}{}}
\makeatother
\newcolumntype{C}[1]{>{\centering\arraybackslash}p{#1}}
\begin{document}
\setcounter{tocdepth}{1}
\tableofcontents
\newpage
"""
  val bodyEnd =
"""\end{document}"""
  def entryStart(
    name: String,
    photo0: Option[String],
//    photo1: Option[String],
    sex: String,
    birth: String,
    ent: String,
    sem: String,
    mil: String,
    last: String,
    univ: String,
    high: String,
    major: String,
    double: String,
    phone: String,
    email: String
  ): String = {
  val photo0t = photo0 match {
    case Some(n) => 
s"""\\graphicspath{{./photo0/}}
\\includegraphics[width=0.5\\textwidth,height=0.25\\textheight,keepaspectratio]{$n}
"""
    case None => ""
  }
//  val photo1t = photo1 match {
//    case Some(n) => 
//s"""\\graphicspath{{./photo1/}}
//\\includegraphics[width=0.5\\textwidth,height=0.25\\textheight,keepaspectratio]{$n}
//"""
//    case None => ""
//  }
s"""\\chapter{$name}
$photo0t\\[
\\begin{tabular}{|C{0.13\\textwidth}|C{0.32\\textwidth}||C{0.13\\textwidth}|C{0.32\\textwidth}|} \\hline
\\text{{\\bf 성별}} & \\text{$sex} & \\text{{\\bf 생년월일}} & \\text{$birth} \\\\ \\hline
\\text{{\\bf 학번}} & \\text{$ent} & \\text{{\\bf 학기}} & \\text{$sem} \\\\ \\hline
\\text{{\\bf 병역}} & \\text{$mil} & \\text{{\\bf 졸업/휴학}} & \\text{$last} \\\\ \\hline
\\text{{\\bf 대학교}} & \\text{$univ} & \\text{{\\bf 고교}} & \\text{$high} \\\\ \\hline
\\text{{\\bf 주전공}} & \\text{$major} & \\text{{\\bf 복수/부전공}} & \\text{($double)} \\\\ \\hline
\\text{{\\bf 전화번호}} & \\text{\\texttt{$phone}} & \\text{{\\bf 이메일}} & \\text{\\texttt{$email}} \\\\ \\hline
\\end{tabular}
\\]
"""
}
  val entryEnd = ""
  def subsection(p: (String, String)) = p match { case (des, ans) =>
s"""\\subsection*{- $des}
$ans
"""
  }
}
