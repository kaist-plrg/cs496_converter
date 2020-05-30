package io.madcamp.utils

object Statistics {
  def create(applicants: List[Applicant]): List[List[String]] = {
    def aux(s: String, f: Applicant => String): List[List[String]] =
      List(s, "") ::
        applicants.groupBy(f).toList.sortBy(_._1).map{
          case (k, v) => List(k, v.size.toString)
        }

    def append(l1: List[List[String]], l2: List[List[String]]): List[List[String]] =
      (for (i <- 0 until Math.max(l1.size, l2.size))
        yield (
          if (i < l1.size) {
            if (i < l2.size)
              l1(i) ++ l2(i)
            else
              l1(i) ++ List.fill(l2.head.length)("")
          } else
            List.fill(l1.head.length)("") ++ l2(i)
        )
      ).toList

    List(
      aux("생년", _.birth./(10000).toString),
      aux("성별", a => if (a.isMale) "남" else "여"),
      aux("대학", _.university),
      aux("병역", a => if (a.isMilitary) "O" else "X"),
      aux("재수", a => if (a.isRepeat) "O" else "X"),
      aux("해외대학", a => if (a.isAbroad) "O" else "X"),
      aux("코딩", _.coding),
      aux("협력", _.cooperation)
    ).reduce(append(_, _))
  }
}
