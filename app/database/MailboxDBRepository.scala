package database


import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import models.Mailbox
import play.api.Logger
import utils.converters.MailboxConverter._


object MailboxDBRepository {
  import database.gen.current.dao.dbConfig.driver.api._
  import database.gen.current.dao._
  
	def saveMailbox(mailBox: Mailbox): Future[Mailbox] = {
		insertMailboxEnitity(mailBox.asMailboxEntity).map(res => res.asMailbox)
	}

	def getMailboxById(id: Int): Future[Mailbox] = {
		getMailboxEntityById(id).map(res => res.asMailbox)
	}

	def getAllMailboxesByUserId(userId: Int): Future[List[Mailbox]] = {
		getMailboxEntitiesByUserId(userId).map(list => list.map(_.asMailbox))
	}

	def updateMailbox(mailBox: Mailbox): Future[Mailbox] = {
		updateMailboxEntity(mailBox.asMailboxEntity).map(res => res.asMailbox)
	}

	def softDeleteMailboxById(id: Int): Future[Mailbox] = {
		softDeleteMailboxEntityById(id).map(res => res.asMailbox)
	}

}
