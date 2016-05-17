package database


import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import play.api.Logger
import models._


object PositionRepository {
  import database.gen.current.dao.dbConfig.driver.api._
  import database.gen.current.dao._
  
	def savePosition(position: Position, companyId: Int): Future[Position] = {
  import utils.converters.PositionConverter._
    insertPosition(position.asPositionEntity(companyId)).map(savedPositionEntt => savedPositionEntt.asPosition)
	}

	def changePosition(position: Position, companyId: Int): Future[Position] = {
  import utils.converters.PositionConverter._
		updatePosition(position.asPositionEntity(companyId)).map(updated => updated.asPosition)
	}


  

}
