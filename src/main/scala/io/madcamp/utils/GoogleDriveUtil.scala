package io.madcamp.utils

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.{Drive, DriveScopes}
import com.google.api.services.drive.model.{File => GFile}
import com.google.api.client.http.FileContent

import java.io.{FileInputStream, FileOutputStream, File}

import scala.jdk.CollectionConverters._

object GoogleDriveUtil {
  private val appName = "Madcamp Applications to PDF"
  private val jsonFactory = JacksonFactory.getDefaultInstance
  private val scopes = List(DriveScopes.DRIVE).asJava
  private val xlsxType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
  private val gsheetType = "application/vnd.google-apps.spreadsheet"

  def service(credentialsFile: String): Drive = {
    val transport = GoogleNetHttpTransport.newTrustedTransport
    val credential = GoogleCredential
      .fromStream(new FileInputStream(credentialsFile))
      .createScoped(scopes)
    new Drive.Builder(transport, jsonFactory, credential)
      .setApplicationName(appName)
      .build()
  }

  def downloadExcel(service: Drive, id: String, name: String): Unit = {
    val out = new FileOutputStream(name)
    service.files.export(id, xlsxType).executeMediaAndDownloadTo(out)
    out.close()
    println(s"$name has been downloaded")
  }

  def downloadPhoto(service: Drive, id: String, i: Int, dir: File): Unit = {
    try {
      val info = service.files.get(id)
      val name = info.execute.getName
      val ext = extension(name)
      val path = s"${dir.getAbsolutePath}${File.separator}$i$ext"
      val out = new FileOutputStream(path)
      info.executeMediaAndDownloadTo(out)
      out.flush()
      out.close()
      println(s"$name has been downloaded")
    } catch {
      case e: Exception =>
        println("failed... try again")
        downloadPhoto(service, id, i, dir)
    }
  }

  def findFiles(service: Drive, id: String): List[GFile] = {
    service.files.list
      .setQ(s"\'$id\' in parents and mimeType = \'$gsheetType\'")
      .execute()
      .getFiles.asScala.toList
  }

  def uploadExcel(service: Drive, parent: String, name: String, file: File): String = {
    val meta = new GFile;
    meta.setName(name);
    meta.setParents(List(parent).asJava)
    meta.setMimeType(gsheetType)
    val content = new FileContent(xlsxType, file)
    val res = service.files.create(meta, content).setFields("id").execute()
    res.getId
  }

  private def extension(name: String): String =
    name.substring(name.lastIndexOf("."))
}
