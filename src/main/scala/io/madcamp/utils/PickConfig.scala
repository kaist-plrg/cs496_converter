package io.madcamp.utils

class PickConfig(lines: List[String]) {

  private def parse(s: String): List[Int] = s.split("-").toList.map(_.toInt)
  private def diff(v: Int, vl: Int, vh: Int) = {
    val d = if (v < vl) vl - v else if (v > vh) v - vh else 0
    -d * d
  }

  def apply(a: List[Applicant], size: Int): Int = {
    def ratio2Num(s: Int): Int = s * size / 100
    def aux0[T](m: Map[T, List[Applicant]], k: T) = m.get(k) match {
      case None => 0
      case Some(s) => s.size
    }
    def aux(t: String, f: String, func: Applicant => Boolean): List[(Int, Int, Int)] =
      (parse(t) ++ parse(f)).map(ratio2Num(_)) match {
        case List(tl, th, fl, fh) => {
          val d = a.groupBy(func)
          val (t, f) = (aux0(d, true), aux0(d, false))
          (t, tl, th) :: (f, fl, fh) :: Nil
        }
      }

    val res = (lines match {
      case List(
        year, young, midOld, old, male, female, mil, notMil,
        kaist, notKaist, repeat, notRepeat, lowCode, midCode, highCode, superCode,
        lowCoop, midCoop, highCoop, abroad, notAbroad
      ) =>

        (parse(year) ++ (parse(young) ++ parse(midOld) ++ parse(old)).map(ratio2Num(_)) match {
          case List(yearl, yearh, yl, yh, ml, mh, ol, oh) => {
            val d = a.groupBy(ap => (ap.birth / 10000) match {
              case x if x > yearh => 'y'
              case x if x < yearl => 'o'
              case _ => 'm'
            })
            val (y, m, o) = (aux0(d, 'y'), aux0(d, 'm'), aux0(d, 'o'))
            (y, yl, yh) :: (m, ml, mh) :: (o, yl, oh) :: Nil
          }
        }) ++
        aux(male, female, _.isMale) ++
        aux(mil, notMil, _.isMilitary) ++
        aux(kaist, notKaist, _.isKaist) ++
        aux(repeat, notRepeat, _.isRepeat) ++
        aux(abroad, notAbroad, _.isAbroad) ++
        ((parse(lowCode) ++ parse(midCode) ++ parse(highCode) ++ parse(superCode)).map(ratio2Num(_)) match {
          case List(ll, lh, ml, mh, hl, hh, sl, sh) => {
            val d = a.groupBy(_.coding)
            val (l, m, h, s) = (aux0(d, "하"), aux0(d, "중"), aux0(d, "상"), aux0(d, "최상"))
            (l, ll, lh) :: (m, ml, mh) :: (h, hl, hh) :: (s, sl, sh) :: Nil
          }
        }) ++
        ((parse(lowCoop) ++ parse(midCoop) ++ parse(highCoop)).map(ratio2Num(_)) match {
          case List(ll, lh, ml, mh, hl, hh) => {
            val d = a.groupBy(_.cooperation)
            val (l, m, h) = (aux0(d, "하"), aux0(d, "중"), aux0(d, "상"))
            (l, ll, lh) :: (m, ml, mh) :: (h, hl, hh) :: Nil
          }
        })
    }).map { case (v, vl, vh) => diff(v, vl, vh) }.sum + a.filter(_.motiv).size
    val max = (a.filterNot(_.isKaist).groupBy(_.university).map{ case (_, as) => as.length }.toSet + 0).max
    if (max > 3) (Int.MinValue  / 4) else res
  }
}
