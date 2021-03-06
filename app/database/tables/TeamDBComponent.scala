package database.tables

import java.sql.Timestamp
import models.RowStatus
import slick.model.ForeignKeyAction._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.profile.SqlProfile.ColumnOption.Nullable
import database.PagedDBResult

case class TeamEntity(
  id: Option[Int] = None,
  companyId: Int,
  name: String,
  description: Option[String] = None,
  recordStatus: Int = RowStatus.ACTIVE,
  createdAt: Timestamp = new Timestamp(System.currentTimeMillis()),
  updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()))

case class TeamGroupEntity(
  teamId: Int,
  userId: Int,
  startDate: Option[Timestamp] = None,
  endDate: Option[Timestamp] = None)


trait TeamDBComponent extends DBComponent {
    this: DBComponent 
    with CompanyDBComponent
    with EmployeeDBComponent
    with UserDBComponent => 

  import dbConfig.driver.api._

  val teams = TableQuery[TeamTable]
  val teamGroups = TableQuery[TeamGroupTable]
  
  class TeamTable(tag: Tag) extends Table[TeamEntity](tag, "tbl_team") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def companyId = column[Int]("company_id")
    def name = column[String]("name", O.SqlType("VARCHAR(255)"))
    def description = column[String]("description", Nullable, O.SqlType("VARCHAR(255)"))
    def recordStatus = column[Int]("record_status", O.Default(RowStatus.ACTIVE))
    def createdAt = column[Timestamp]("created_at", Nullable)
    def updatedAt = column[Timestamp]("updated_at", Nullable)

    def fkCompanyId = foreignKey("fk_team_company", companyId, companies)(_.id)
    def UqTeamName = index("uq_team_name_per_company", (name,companyId), unique = true)

    def * = (id.?, companyId, name, description.?, recordStatus, createdAt, updatedAt)<>(TeamEntity.tupled, TeamEntity.unapply)
  }

  class TeamGroupTable(tag: Tag) extends Table[TeamGroupEntity](tag, "tbl_group_team"){
    def teamId = column[Int]("team_id")
    def userId = column[Int]("user_id")
    def startDate = column[Timestamp]("start_date", Nullable)
    def endDate = column[Timestamp]("end_date", Nullable)

    def fkTeamId = foreignKey("fk_team_id", teamId, teams)(_.id, onUpdate = Restrict, onDelete = Cascade)
    def fkUserId = foreignKey("fk_user_id", userId, users)(_.id, onUpdate = Restrict, onDelete = Cascade)

    def * = (teamId, userId, startDate.?, endDate.?)<>(TeamGroupEntity.tupled, TeamGroupEntity.unapply)
  }

  def groupWithTeams = teamGroups join teams on ( _.teamId === _.id)
  // (TeamGroupEntity, (EmployeeEntity, (UserEntity, ContactProfile)))
  def teamWithEmployee = teamGroups join employeesWithUsersWihtProfile on ( _.userId === _._1.userId)

  def teamQry(companyId: Int) = {
    teams.filter(t =>(t.companyId === companyId && 
                      t.recordStatus === RowStatus.ACTIVE))
  }
  //CRUD TeamEntity
  def insertTeam(team: TeamEntity): Future[TeamEntity] = {
      db.run((teams returning teams.map(_.id) into ((team,id) => team.copy(id=Some(id)))) += team)
  }

  def getTeamEntityById(id: Int): Future[TeamEntity] = {
    db.run(teams.filter(t =>(t.id === id && 
                             t.recordStatus === RowStatus.ACTIVE)).result.head)
  }

  def updateTeamEntity(team: TeamEntity): Future[TeamEntity] = {
      db.run(teams.filter(t =>(t.id === team.id && 
                                t.recordStatus === RowStatus.ACTIVE)).update(team)
                  .map{num => if(num != 0) team 
                              else throw new Exception("Can't update team, is it deleted?")})
  }

  def softDeleteTeamById(id: Int): Future[TeamEntity] = {
    getTeamEntityById(id).flatMap(res =>
        updateTeamEntity(res.copy(recordStatus = RowStatus.DELETED, 
                            updatedAt = new Timestamp(System.currentTimeMillis()))))
  } 

  //CRUD TeamGroup 
  def insertTeamGroup(teamGroup: TeamGroupEntity): Future[TeamGroupEntity] = {
      //db.run((teamGroups returning teamGroups.map(_.teamId) into ((teamGroup,id) => teamGroup.copy(teamId=id))) += teamGroup)
      db.run(teamGroups += teamGroup).map( res => teamGroup)
  }

  //NOTE: pointless
  /* 
  def getTeamGroupByEntity(tg: TeamGroupEntity): Future[TeamGroupEntity] = {
    db.run(teamGroups.filter(t => t.userId === tg.userId && t.teamId === tg.teamId).result.head) 
  }
  */

  def deleteTeamGroup(teamGroup: TeamGroupEntity): Future[TeamGroupEntity] = {
    db.run(teamGroups.filter( t => ( t.teamId === teamGroup.teamId &&
                                t.userId === teamGroup.userId)).delete)
    Future(teamGroup)
  }
  
  def getTeamGroupEntityByUserId(userId: Int): Future[TeamGroupEntity] = {
    db.run(teamGroups.filter(_.userId === userId).result.head) 
  }

  def deleteTeamGroupByUserId(userId: Int): Future[TeamGroupEntity] = {
    val lastDeleted = getTeamGroupEntityByUserId(userId)
    db.run(teamGroups.filter(_.userId === userId).delete)
    lastDeleted
  }

  def deleteTeamGroupByTeamId(teamId: Int): Future[String] = {
    //val lastDeleted = getTeamGroupEntityByUserId(userId)
    db.run(teamGroups.filter(_.teamId === teamId).delete)
    Future("Ok")
  }

  def deleteUserFromTeamGroup(teamId:Int, userId: Int): Future[TeamGroupEntity] = {
    val deleted = db.run(teamGroups.filter(t => (t.userId === userId && 
                                                 t.teamId === teamId)).result.head)
    db.run(teamGroups.filter(t => (t.userId === userId && 
                                   t.teamId === teamId)).delete)
    deleted
  }

  //FILTERS
  def insertTeamGroups(teamGroups: List[TeamGroupEntity]): Future[List[TeamGroupEntity]] = {
    Future.sequence(teamGroups.map( t =>  insertTeamGroup(t)))
  }

  def getTeamEntitiesByCompanyId(companyId: Int): Future[List[TeamEntity]] = {
    db.run(teams.filter(t => (t.companyId === companyId && 
                              t.recordStatus === RowStatus.ACTIVE)).result).map(_.toList)
  }

  def getTeamEntitiesByUserId(userId: Int): Future[List[(TeamGroupEntity, TeamEntity)]] = {
    db.run(groupWithTeams.filter( _._1.userId === userId ).result).map(_.toList)
  }

  def getTeamEmployeesByTeamId(teamId: Int): Future[List[(TeamGroupEntity, (EmployeeEntity, (UserEntity, ContactProfileEntity)))]] = {
    db.run(teamWithEmployee.filter( _._1.teamId === teamId ).result).map(_.toList)
  }

  def deleteTeamGroups(teamGroups: List[TeamGroupEntity]): Future[List[TeamGroupEntity]] = {
    Future.sequence(teamGroups.map( t =>  deleteTeamGroup(t)))
    Future(teamGroups)
  }

  def searchTeamEntitiesByName(companyId: Int, pageSize: Int, pageNr: Int, searchTerm: Option[String] = None): Future[PagedDBResult[TeamEntity]] = {
    val baseQry = searchTerm.map { st =>
        val s = "%" + st + "%"
        teamQry(companyId).filter{_.name.like(s)}
      }.getOrElse(teamQry(companyId))  

    val pageRes = baseQry
      .sortBy(_.name.asc)
      .drop(pageSize * (pageNr - 1))
      .take(pageSize)

    db.run(pageRes.result).flatMap( teamList => 
        db.run(baseQry.length.result).map( totalCount => 
         PagedDBResult(
            pageSize = pageSize,
            pageNr = pageNr,
            totalCount = totalCount,
            data = teamList)
          )
        )
  }

  def getTeamsCountByCompanyId(companyId: Int): Future[Int] = {
   db.run(teams.filter(t => (t.companyId === companyId && 
                             t.recordStatus === RowStatus.ACTIVE)).length.result)
  }

}

