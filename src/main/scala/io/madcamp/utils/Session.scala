package io.madcamp.utils

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
}
