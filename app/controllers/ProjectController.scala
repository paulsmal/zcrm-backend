package controllers

import javax.inject._
import play.api._
import play.api.mvc._

import models._
import utils.ExpectedFormat._
import controllers.session.InsufficientRightsException
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import database.ProjectDBRepository 
import play.api.libs.json.Json
import scala.util.{Failure, Success, Try}
import scala.concurrent.Future

@Singleton
class ProjectController @Inject() extends CRMController {
  import utils.JSFormat.projectFrmt

  //TODO: add permissions check
  def newProject(companyId: Int) = CRMActionAsync[Project](expectedProjectFormat) { rq => 
    // if(rq.header.belongsToCompany(companyId)){
       ProjectDBRepository.createProject(rq.body, companyId).map( project => Json.toJson(project))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }


  //TODO: add permissions check
  def updateProject(companyId: Int, projectId: Int) = CRMActionAsync[Project](expectedProjectFormat){ rq =>
    // if(rq.header.belongsToCompany(companyId)){
      ProjectDBRepository.updateProject(rq.body.copy(id = Some(projectId)), companyId).map( project => Json.toJson(project))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }


  //TODO: add permissions check
  def getProject(companyId: Int, projectId: Int) = CRMActionAsync { rq =>
    // if(rq.header.belongsToCompany(companyId)){
      ProjectDBRepository.getProjectById(projectId).map( project => Json.toJson(project))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }

  //TODO: add permissions check
  def getAllProjects(companyId: Int) = CRMActionAsync { rq =>
    // if(rq.header.belongsToCompany(companyId)){
      ProjectDBRepository.getProjectsByCompanyId(companyId).map( project => Json.toJson(project))
    // }else{ Future{Failure(new InsufficientRightsException())} }
  }
  
  def deleteProjectById(companyId: Int, projectId:Int) = CRMActionAsync{rq =>
      ProjectDBRepository.deleteProject(projectId).map(deletedProject => Json.toJson(deletedProject))
  }

  def searchAllProjectsByName(companyId: Int, pageSize: Option[Int], pageNr: Option[Int], searchTerm: Option[String]) = CRMActionAsync{rq =>
    import utils.JSFormat._
    if (pageNr.nonEmpty || pageSize.nonEmpty || searchTerm.nonEmpty) {
      val psize = pageSize.getOrElse(10)
      val pnr = pageNr.getOrElse(1)
      ProjectDBRepository.searchProjectByName(companyId, psize, pnr, searchTerm).map(page => Json.toJson(page))
    } else { ProjectDBRepository.getProjectsByCompanyId(companyId).map( projects => Json.toJson(projects)) }
  }

  import utils.JSFormat.clientFrmt
  case class ProjectClients( projectId: Int, clients: List[Client])
  implicit val  projectClientFrmt       = Json.format[ProjectClients]

  import scala.collection.immutable.ListMap
  val expecteProjectClientsFormat = Json.toJson(ListMap(
    "projectId"      -> "[M] (int) id of the project",
    "clients"        -> "[M] (list[client])"))

  def addClientsToProject(companyId: Int, projectId: Int) = CRMActionAsync[ProjectClients](expecteProjectClientsFormat){ rq =>
    ProjectDBRepository.addClients(rq.body.projectId, rq.body.clients).map(clients => Json.toJson(clients))
  }

  import utils.JSFormat.userFrmt
  case class ProjectMembers( projectId: Int, members: List[User])
  implicit val  projectMembersFrmt       = Json.format[ProjectMembers]

  import scala.collection.immutable.ListMap
  val expecteProjectMembersFormat = Json.toJson(ListMap(
    "projectId"        -> "[M] (int) id of the project",
    "members"         -> "[M] (list[user])"))

  def addMembersToProject(companyId: Int, projectId: Int) = CRMActionAsync[ProjectMembers](expecteProjectMembersFormat){ rq => 
    ProjectDBRepository.addMembers(rq.body.projectId, rq.body.members).map(users => Json.toJson(users))
  }

  import utils.JSFormat.teamFrmt
  case class ProjectTeamMembers( projectId: Int, teams: List[Team])
  implicit val  projectTeamMembersFrmt       = Json.format[ProjectTeamMembers]

  import scala.collection.immutable.ListMap
  val expecteProjectTeamMembersFormat = Json.toJson(ListMap(
    "projectId"      -> "[M] (int) id of the project",
    "teams"         -> "[M] (list[team])"))

  def addTeamMembersToProject(companyId: Int, projectId: Int) = CRMActionAsync[ProjectTeamMembers](expecteProjectTeamMembersFormat){ rq => 
    ProjectDBRepository.addTeams(rq.body.projectId, rq.body.teams).map(teams => Json.toJson(teams))
  }

 
}
