package database.tables

import java.sql.Timestamp
import slick.profile.SqlProfile.ColumnOption.Nullable
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.model.ForeignKeyAction.{Cascade, SetNull, Restrict}
import models._
import database.PagedDBResult
import play.api.Logger
import slick.lifted.{Ordered, ColumnOrdered}
import slick.ast.Ordering

case class ExchangeODSMailEntity(
    id: Option[Int] = None,
    mailboxId: Int,
    extId: String,
    conversationExtId: String,
    sender: String,
    receivedBy: String,
    ccRecipients: String,
    bccRecipients: String,
    subject: String,
    body: String,
    importance: String,
    attachments: String,
    size: Int,
    received: Timestamp)

trait ExchangeODSMailDBComponent extends DBComponent {
 this: DBComponent
 with MailboxDBComponent=>

  import dbConfig.driver.api._

  val ods_mails = TableQuery[ExchangeMailTable]

  class ExchangeMailTable(tag: Tag) extends Table[ExchangeODSMailEntity](tag, "tbl_ods_mail") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def mailboxId = column[Int]("mailbox_id")
    def extId = column[String]("ext_id", O.SqlType("VARCHAR(255)"))
    def conversationExtId = column[String]("conversation_ext_id", O.SqlType("VARCHAR(255)"))
    def sender = column[String]("sender", O.SqlType("VARCHAR(255)"))
    def receivedBy = column[String]("received_by", O.SqlType("VARCHAR(255)"))
    def ccRecipients = column[String]("cc_recipients", O.SqlType("VARCHAR(255)"))
    def bccRecipients = column[String]("bcc_recipients", O.SqlType("VARCHAR(255)"))
    def subject = column[String]("subject", O.SqlType("VARCHAR(255)"))
    def body = column[String]("body")
    def importance = column[String]("importance", O.SqlType("VARCHAR(255)"))
    def attachments = column[String]("attachments", O.SqlType("VARCHAR(255)"))
    def size = column[Int]("size")
    def received = column[Timestamp]("received")

    def mailboxIdFk = foreignKey("fk_ods_mailbox_id", mailboxId, mailboxes)(_.id.get)

    def * = (id.?, mailboxId, extId, conversationExtId, sender, receivedBy, ccRecipients, bccRecipients,
             subject, body, importance, attachments, size, received) <> (ExchangeODSMailEntity.tupled, ExchangeODSMailEntity.unapply)

    def UqExtId = index("unique_ods_ext_id", extId, unique = true)
  }

  def odsQry(mailboxId: Int) = {
    ods_mails.filter(_.mailboxId === mailboxId)
  }

   def insertODSMailEntity(mail: ExchangeODSMailEntity): Future[ExchangeODSMailEntity] = {
       db.run((ods_mails returning ods_mails.map(_.id)
                     into ((mail,id) => mail.copy(id=Some(id)))) += mail)
   }
   
  def insertTheEmails(emails: Iterable[ExchangeODSMailEntity]): Future[Option[Int]] = {
    db.run(ods_mails ++= emails)
  }

  def getODSMailEntityById(id: Int): Future[ExchangeODSMailEntity] = {
    db.run(ods_mails.filter(_.id === id).result.head)
  }

  def getODSMailEntityByExtId(extId: String): Future[ExchangeODSMailEntity] = {
      db.run(ods_mails.filter(_.extId === extId).result.head)
  }

  def getODSMailEntitiesByConversationId(converstionId: String): Future[List[ExchangeODSMailEntity]] = {
      db.run(ods_mails.filter(_.conversationExtId === converstionId).result).map(_.toList)
  }

  def getODSMailEntitiesByMailboxId(mailboxId: Int): Future[List[ExchangeODSMailEntity]] = {
    db.run(ods_mails.filter(_.mailboxId === mailboxId).result).map(_.toList)
  }

  def updateODSMailEntity(mail: ExchangeODSMailEntity): Future[ExchangeODSMailEntity] = {
      db.run(ods_mails.filter(_.id === mail.id).update(mail))
                    .map( num => mail)
  }

  def deleteODSMailEntityById(id: Int): Future[ExchangeODSMailEntity] = {
      val deleted = getODSMailEntityById(id)
      db.run(ods_mails.filter(_.id === id).delete)
      deleted
  }

  def deleteODSMailEntityByExtId(extId: String): Future[ExchangeODSMailEntity] = {
    val deleted = getODSMailEntityByExtId(extId)
    db.run(ods_mails.filter(_.extId === extId).delete)
    deleted
  }

  //FILTERS

  def searchODSMailEntitiesByMailboxId(
      mailboxId: Int,
      pageSize: Int,
      pageNr: Int,
      searchTerm: Option[String] = None): Future[PagedDBResult[ExchangeODSMailEntity]] = {
    val baseQry = searchTerm.map(st => odsQry(mailboxId).filter(_.subject.like("%" + st + "%")))
      .getOrElse(odsQry(mailboxId))
    
    db.run(baseQry.result).flatMap { mailsList =>
      val mails = mailsList.sortWith((first, second) => first.received.after(second.received))
        .drop(pageSize * (pageNr - 1))
        .take(pageSize)
        
      db.run(baseQry.length.result).map(totalCount => PagedDBResult(pageSize, pageNr, totalCount, mails))
    }
  }
}