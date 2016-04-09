package database

import models.Employee

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import models.Company

object CompanyDBRepository {
  import database.gen.current.dao.dbConfig.driver.api._
  import database.gen.current.dao._

  def saveCompany(company: Company): Future[Company] = {
    import utils.converters.CompanyConverter.{CompanyToEntity, EntitiesToCompany}
    import utils.converters.ContactProfileConverter.ContactProfileToEntity
    import database.tables.ContactProfileEntity

    //TODO: should be transactionally
    (for {
        profile <- upsertProfile(company.contactProfile.fold(ContactProfileEntity())(_.asEntity()))
        company <- upsertCompany(company.asEntity(profile.id.get))
    } yield (company, profile)).map(_.asCompany)
  }


}