package io.madcamp.apptopdf

import scala.collection.mutable.ListBuffer

class Session(conf: PickConfig, size: Int) {

  private val applicants = new ListBuffer[Applicant]()
  def getApplicants = applicants.toList

  def score: Int = conf(applicants.toList, size)

  def scoreIncrease(applicant: Applicant): Int = {
    val old = score
    addApplicant(applicant)
    val sco = score
    applicants.remove(applicants.size - 1)
    sco - old
  }

  def withoutDo(applicant: Applicant)(f: => Unit): Unit = {
    applicants -= applicant
    assert(applicants.size == size - 1)
    f
    applicants += applicant
  }

  def addApplicant(applicant: Applicant) = applicants += applicant
  def accepted: List[Applicant] = applicants.toList

  def canAdd: Boolean = applicants.size < size
  def in(a: Applicant): Boolean = applicants.contains(a)

  def toResult: String = applicants.sortBy(a => (a.university, a.ent, a.name)).map(_.toString).mkString("\n")

  override def toString: String = {
    def aux(s: String, f: Applicant => Any): List[String] = (s + ",") :: applicants.groupBy(f(_)).map { case (k, v) => k + "," + v.size }.toList.sorted
    def append(l1: List[String], l2: List[String]): List[String] =
      (for (i <- 0 until Math.max(l1.size, l2.size))
        yield (if (i < l1.size) (if (i < l2.size) l1(i) + "," + l2(i) else l1(i) + ",,") else ",," + l2(i))).toList
    List(aux("birth", _.birth / 10000),
      aux("sex", a => if (a.isMale) "male" else "female"),
      aux("military?", _.isMilitary),
      aux("repeat?", _.isRepeat),
      aux("abroad?", _.isAbroad),
      aux("kaist?", _.isKaist),
      aux("coding", _.coding),
      aux("cooperation", _.cooperation),
      aux("diversity?", _.motiv)).reduce(append(_, _)).mkString("\n")
  }
}
