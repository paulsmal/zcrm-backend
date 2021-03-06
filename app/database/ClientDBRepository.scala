package database

import models.Employee

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import models.{Client, PagedResult}
import play.api.Logger
import utils.converters.ClientConverter._
import utils.converters.ContactProfileConverter._


object ClientDBRepository {
  import database.gen.current.dao.dbConfig.driver.api._
  import database.gen.current.dao._

  def createClient(client: Client): Future[Client] = {
    for{
        profile <- upsertProfile(client.contactProfile.asEntity())
        client  <- insertClient(client.copy(contactProfile = profile.asProfile).asClientEntity)
        inserted<- getClientWithProfileById(client.id.get)
    }yield(inserted.asClient)
  }

  def updateClient(client: Client): Future[Client] = {
    for{
        profile <- upsertProfile(client.contactProfile.asEntity())
        client  <- updateClientEntity(client.copy(contactProfile = profile.asProfile).asClientEntity)
        updated <- getClientWithProfileById(client.id.get)
    }yield(updated.asClient)
  }

  def deleteClient(clientId: Int): Future[Client] = {
    deleteClientById(clientId)
          .map(deleted => deleted.asClient)
  }

  def getClientById(id: Int): Future[Client] = {
    getClientWithProfileById(id).map(client => client.asClient)
  }

  def getClientsByCompanyId(companyId: Int): Future[List[Client]] = {
    getAllClientsWithContactProfileByCompanyId(companyId).map(list => list.map(_.asClient))
  } 
}
