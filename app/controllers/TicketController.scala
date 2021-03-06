package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import models._
import utils.ExpectedFormat._
import controllers.session.InsufficientRightsException
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import database.{TicketDBRepository, TicketActionDBRepository,ExchangeODSMailDBRepository,ExchangeSavedMailDBRepository,TicketAttachedMailDBRepository, FileDBRepository, TicketAttachedFileDBRepository, UserDBRepository}
import play.api.libs.json.Json
import scala.util.{Failure, Success, Try}
import scala.concurrent.Future
import java.text.DecimalFormat

@Singleton
class TicketController @Inject() extends CRMController {
  import utils.JSFormat.ticketFrmt
  import utils.JSFormat.ticketActionFrmt

  //TODO: add permissions check
  def newTicket(companyId: Int, projectId: Int) = CRMActionAsync[Ticket](expectedTicketFormat) { rq => 
    // if(rq.header.belongsToCompany(companyId)){
       TicketDBRepository.createTicket(rq.body, companyId, projectId).map(ticket => Json.toJson(ticket))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

  //TODO: add permissions check
  def addCommentToTicket(companyId: Int, ticketId: Int) = CRMActionAsync[TicketAction](expectedTicketActionFormat){ rq =>
    // if(rq.header.belongsToCompany(companyId)){
      TicketActionDBRepository.createAction(rq.body.copy(ticketId = ticketId, actionType = ActionType.COMMENT), companyId).map(ticketAction => Json.toJson(ticketAction))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

  //TODO: add permissions check
  def attachMailToTicket(companyId: Int, ticketId: Int, mailId: Int) = CRMActionAsync{ _ =>
    // if(rq.header.belongsToCompany(companyId)){
    import utils.JSFormat.exchangeMailFrmt
    
    for {
        userId       <- ExchangeODSMailDBRepository.getUserIdByODSMailId(mailId)
        odsMail      <- ExchangeODSMailDBRepository.getODSMailById(mailId)
        mail         <- ExchangeSavedMailDBRepository.insertSavedMail(odsMail)
        user         <- UserDBRepository.getUserByUserId(userId)
        action       <- TicketActionDBRepository.createAction(TicketAction(ticketId = ticketId, user = user, actionType = ActionType.MAIL), companyId)
        attachedMail <- TicketAttachedMailDBRepository.createAttachedMailAction(TicketActionAttachedMail(actionId = action.id.get, mailId = mail.id.get ))
        updatedOds   <- ExchangeODSMailDBRepository.updateODSMail(
            odsMail.copy(subject = Some(odsMail.subject.getOrElse("") + " [Ticket "+(new DecimalFormat("#000000")).format(ticketId)+"]")))
    } yield JS(Json.toJson(mail))
  }

 def detachMailFromTicket(companyId: Int, ticketId: Int, actionId: Int, mailId: Int) = CRMActionAsync{rq =>
   import utils.JSFormat.ticketActionMailFrmt
   for{
          deletedAttachedMail <- TicketAttachedMailDBRepository.deleteAttachedMailAction(actionId, mailId)
          deletedAction       <- TicketActionDBRepository.deleteAction(deletedAttachedMail.actionId)
   }yield Json.toJson(deletedAttachedMail)
  }

  //TODO: add permissions check
  //TODO: Failure safe
  def attachFileToTicket(companyId: Int, ticketId: Int, fileId: Int) = CRMActionAsync{ rq =>
    // if(rq.header.belongsToCompany(companyId)){
    import utils.JSFormat.ticketActionFileFrmt
    for{
          file         <- FileDBRepository.getFileById(fileId)
          user         <- UserDBRepository.getUserByUserId(file.userId)
          action       <- TicketActionDBRepository.createAction(TicketAction(ticketId = ticketId, user = user, actionType = ActionType.FILE), companyId)
          attachedFile <- TicketAttachedFileDBRepository.createAttachedFileAction(TicketActionAttachedFile(actionId = action.id.get, fileId = file.id.get ))
      } yield Json.toJson(attachedFile)    
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

 def detachFileFromTicket(companyId: Int, ticketId: Int, actionId: Int, fileId:Int) = CRMActionAsync{rq =>
   import utils.JSFormat.ticketActionFileFrmt
   for{
          deletedAttachedFile <- TicketAttachedFileDBRepository.deleteAttachedFileAction(actionId, fileId)
          deletedAction       <- TicketActionDBRepository.deleteAction(deletedAttachedFile.actionId)
   }yield Json.toJson(deletedAttachedFile)
  }



  //TODO: add permissions check
  def updateTicket(companyId: Int, ticketId: Int) = CRMActionAsync[Ticket](expectedTicketFormat){ rq =>
    // if(rq.header.belongsToCompany(companyId)){
      TicketDBRepository.updateTicket(rq.body.copy(id = Some(ticketId.toString)), companyId).map( ticket => Json.toJson(ticket))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

  import utils.JSFormat.projectFrmt
  def updateProject(companyId: Int, ticketId: Int) = CRMActionAsync[Project](expectedProjectFormat){rq =>
    for{
        ticket   <- TicketDBRepository.getTicketById(ticketId)
        updated  <- TicketDBRepository.updateTicket(ticket.copy(project = Some(rq.body)), companyId)
    } yield Json.toJson(updated)
  }


  //TODO: add permissions check
  def getTicket(companyId: Int, ticketId: Int) = CRMActionAsync { rq =>
    // if(rq.header.belongsToCompany(companyId)){
      TicketDBRepository.getTicketById(ticketId).map( ticket => Json.toJson(ticket))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

  //TODO: add permissions check
  def getAllTickets(companyId: Int) = CRMActionAsync { rq =>
    // if(rq.header.belongsToCompany(companyId)){
      TicketDBRepository.getTicketsByCompanyId(companyId).map( ticket => Json.toJson(ticket))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

   def getAllTicketsByCreatedByUser(companyId:Int, userId: Int) = CRMActionAsync { rq =>
    // if(rq.header.belongsToCompany(companyId)){
      TicketDBRepository.getTicketsByCreatedByUserId(userId).map( ticket => Json.toJson(ticket))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }
  
  def deleteTicketById(companyId: Int, ticketId:Int) = CRMActionAsync{rq =>
      TicketDBRepository.deleteTicket(ticketId).map(deletedTicket => Json.toJson(deletedTicket))
  }

  /*
   * priority - low, med, high, asap
   * field - id, subj, proj, date, ddln, prt
   */

  def searchAllTicketsByName(
      companyId: Int, 
      projectIds: List[Int], 
      createdByUserIds: List[Int],
      requestedByUserIds: List[Int], 
      assignedToUserIds: List[Int],
      assignedToTeamIds: List[Int], 
      pageSize: Option[Int], 
      pageNr: Option[Int], 
      searchTerm: Option[String],
      priority: Option[Int],
      sort: Option[String],
      order: Option[String]) = CRMActionAsync { rq =>
    import utils.JSFormat._

    TicketDBRepository.searchTicketByName(
        companyId, 
        projectIds,
        createdByUserIds,
        requestedByUserIds,
        assignedToUserIds,
        assignedToTeamIds,
        pageSize.getOrElse(10),
        pageNr.getOrElse(1),
        searchTerm,
        priority,
        sort.getOrElse("id"),
        order.getOrElse("desc")).map(page => Json.toJson(page))

//
//    if (pageNr.nonEmpty || pageSize.nonEmpty || searchTerm.nonEmpty) {
//      TicketDBRepository.searchTicketByName(
//          companyId, 
//          projectIds,
//          createdByUserIds,
//          requestedByUserIds,
//          assignedToUserIds,
//          assignedToTeamIds,
//          pageSize.getOrElse(10),
//          pageNr.getOrElse(1),
//          searchTerm,
//          priority).map(page => Json.toJson(page))
//    } else {
//      TicketDBRepository.getTicketsByCompanyId(companyId).map(tickets => Json.toJson(tickets))
//    }
  }

  def getAllActionWithPagination(companyId: Int, ticketId: Int, actionTypes: List[Int], pageSize: Option[Int], pageNr: Option[Int], searchTerm: Option[String]) = CRMActionAsync{rq =>
    import utils.JSFormat._
    if (pageNr.nonEmpty || pageSize.nonEmpty || searchTerm.nonEmpty) {
      val psize = pageSize.getOrElse(10)
      val pnr = pageNr.getOrElse(1)
      TicketActionDBRepository.getActionWithPagination(ticketId, actionTypes, psize, pnr).map(page => Json.toJson(page))
    } else { TicketActionDBRepository.getActionsByTicketId(ticketId, actionTypes).map( tickets => Json.toJson(tickets)) }
  }

//  def getAllAggregatedTicketWithPagination(companyId: Int, createdByUserIds: List[Int], assignedToUserIds: List[Int], assignedToTeamIds: List[Int], pageSize: Option[Int], pageNr: Option[Int]) = CRMActionAsync{rq =>
//    import utils.JSFormat._
//    TicketDBRepository.getAllAggregatedTickets(companyId, createdByUserIds, assignedToUserIds, assignedToTeamIds, pageSize, pageNr)
//                       .map(page => Json.toJson(page))
//  }
//
//  def getAggregadTicket(companyId: Int, ticketId: Int) = CRMActionAsync{rq =>
//     import utils.JSFormat._
//     TicketDBRepository.getAggregatedTicketById(ticketId).map(aggTicket => Json.toJson(aggTicket))
//  }
//
//    import utils.JSFormat._
//  def updateAggregatedTicket(companyId: Int, ticketId: Int) = CRMActionAsync[AggregatedTicket](expectedAggregatedTicketFormat){ rq  =>
//    TicketDBRepository.updateAggregatedTicket(rq.body).map(updated => Json.toJson(updated))
//  }

  import utils.JSFormat.userFrmt
  case class TicketMembers( ticketId: Int, members: List[User])
  implicit val  ticketMembersFrmt       = Json.format[TicketMembers]

  import scala.collection.immutable.ListMap
  val expecteTicketMembersFormat = Json.toJson(ListMap(
    "ticketId"        -> "[M] (int) id of the ticket",
    "members"         -> "[M] (list[user])"))

  def addMembersToTicket(companyId: Int, ticketId: Int) = CRMActionAsync[TicketMembers](expecteTicketMembersFormat){ rq => 
    TicketDBRepository.addMembers(rq.body.ticketId, rq.body.members).map(users => Json.toJson(users))
  }

  import utils.JSFormat.teamFrmt
  case class TicketTeamMembers( ticketId: Int, teams: List[Team])
  implicit val  ticketTeamMembersFrmt       = Json.format[TicketTeamMembers]

  import scala.collection.immutable.ListMap
  val expecteTicketTeamMembersFormat = Json.toJson(ListMap(
    "ticketId"      -> "[M] (int) id of the ticket",
    "teams"         -> "[M] (list[team])"))

  def addTeamMembersToTicket(companyId: Int, ticketId: Int) = CRMActionAsync[TicketTeamMembers](expecteTicketTeamMembersFormat){ rq => 
    TicketDBRepository.addTeams(rq.body.ticketId, rq.body.teams).map(teams => Json.toJson(teams))
  }

  import utils.JSFormat.clientFrmt
  case class TicketClients( ticketId: Int, clients: List[Client])
  implicit val  ticketClientFrmt       = Json.format[TicketClients]

  import scala.collection.immutable.ListMap
  val expecteTicketClientsFormat = Json.toJson(ListMap(
    "ticketId"      -> "[M] (int) id of the ticket",
    "clients"       -> "[M] (list[client])"))

  def addClientsToTicket(companyId: Int, ticketId: Int) = CRMActionAsync[TicketClients](expecteTicketClientsFormat){ rq =>
    TicketDBRepository.addClients(rq.body.ticketId, rq.body.clients).map(clients => Json.toJson(clients))
  }

  case class updateStatus(id:Int, status: Int)
  implicit val updateStatusFrmt = Json.format[updateStatus]
  val expectedUpdateStatusFormat = Json.toJson(ListMap(
    "id"        -> "[M] (int) id of the client",
    "status"    -> "[M] (int) Status"))
  def updateStatusById(companyId: Int, ticketId: Int) = CRMActionAsync[updateStatus](expectedUpdateStatusFormat){rq =>
    TicketDBRepository.getTicketById(rq.body.id).flatMap(ticket =>
       TicketDBRepository.updateTicket(ticket.copy(status = rq.body.status), companyId).map(res => Json.toJson(res)))
  }

  case class updatePriority(id:Int, priority: Int)
  implicit val updatePriorityFrmt = Json.format[updatePriority]
  val expectedUpdatePriorityFormat = Json.toJson(ListMap(
    "id"        -> "[M] (int) id of the client",
    "priority"    -> "[M] (int) priority"))
  def updatePriorityById(companyId: Int, ticketId: Int) = CRMActionAsync[updatePriority](expectedUpdatePriorityFormat){rq =>
    TicketDBRepository.getTicketById(rq.body.id).flatMap(ticket =>
       TicketDBRepository.updateTicket(ticket.copy(priority = rq.body.priority), companyId).map(res => Json.toJson(res)))
  }

  import utils.JSFormat.userFrmt
  case class TicketRequesters( ticketId: Int, requesters: List[User])
  implicit val  ticketRequestersFrmt       = Json.format[TicketRequesters]

  import scala.collection.immutable.ListMap
  val expecteTicketRequestersFormat = Json.toJson(ListMap(
    "ticketId"        -> "[M] (int) id of the ticket",
    "requesters"      -> "[M] (list[user])"))

  def addRequestersToTicket(companyId: Int, ticketId: Int) = CRMActionAsync[TicketRequesters](expecteTicketRequestersFormat){ rq => 
    TicketDBRepository.addRequesters(rq.body.ticketId, rq.body.requesters).map(users => Json.toJson(users))
  }
 
}
