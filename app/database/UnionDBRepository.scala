package database

import models.Employee

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import models.{Union, PagedResult}
import play.api.Logger
import utils.converters.UnionConverter._


object UnionDBRepository {
  import database.gen.current.dao.dbConfig.driver.api._
  import database.gen.current.dao._

  def createUnion(union: Union, companyId: Int): Future[Union] = {
    insertUnion(union.asUnionEntity(companyId))
          .map(inserted => inserted.asUnion)
  }

  def updateUnion(union: Union, companyId: Int): Future[Union] = {
    updateUnionEntity(union.asUnionEntity(companyId))
          .map(updated => updated.asUnion)
  }

  def deleteUnion(unionId: Int): Future[Union] = {
    softDeleteUnionById(unionId)
          .map(deleted => deleted.asUnion)
  }

  def getUnionById(id: Int): Future[Union] = {
    getUnionEntityById(id).map(union => union.asUnion)
  }

  def getUnionsByCompanyId(companyId: Int): Future[List[Union]] = {
    getUnionEntitiesByCompanyId(companyId).map(list => list.map(_.asUnion))
  } 

  def searchUnionByName(companyId: Int, pageSize: Int, pageNr: Int, searchTerm: Option[String]): Future[PagedResult[Union]] = {
    searchUnionEntitiesByName(companyId, pageSize, pageNr, searchTerm).map{dbPage =>
        PagedResult[Union](pageSize = dbPage.pageSize,
                             pageNr = dbPage.pageNr,
                             totalCount = dbPage.totalCount,
                             data = dbPage.data.map(_.asUnion))}
  }
}
