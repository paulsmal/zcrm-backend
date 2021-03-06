package database.tables

import models.{UserLevels, RowStatus}
import slick.model.ForeignKeyAction._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.profile.SqlProfile.ColumnOption.Nullable
import database.PagedDBResult
import utils.DBComponentWithSlickQueryOps

import play.api.Logger


case class EmployeeEntity(
                           id: Option[Int],
                           companyId:  Int,
                           userId: Int,
                           positionId: Option[Int] = None,
                           shiftId: Option[Int] = None, 
                           departmentId: Option[Int] = None, 
                           unionId: Option[Int] = None,
                           flypass: Option[String] = None, 
                           salarySystem: Option[Int] = None, 

                           // The level the user has within a company, i.e admin or normal employee
                           // 1000 - 9999  = user level
                           // 100 - 999    = Human resource
                           // 0-99         = Admin levels
                           employeeLevel: Int,
                           recordStatus: Int = RowStatus.ACTIVE)



trait EmployeeDBComponent extends DBComponentWithSlickQueryOps{
  this: DBComponent 
    with UserDBComponent
    with ContactProfileDBComponent
    with DepartmentDBComponent
    with PositionDBComponent
    with ShiftDBComponent
    with UnionDBComponent
    with CompanyDBComponent
    =>

  import dbConfig.driver.api._

  val employees = TableQuery[EmployeeTable]

  class EmployeeTable(tag: Tag) extends Table[EmployeeEntity](tag, "tbl_employee") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def companyId = column[Int]("company_id")
    def userId = column[Int]("user_id")
    def positionId = column[Int]("position_id", Nullable)
    def shiftId = column[Int]("shift_id", Nullable)
    def departmentId = column[Int]("department_id", Nullable)
    def unionId = column[Int]("union_id", Nullable)
   // def employeeType = column[String]("employee_type", Nullable)
    //def comment = column[String]("comment")
    def flypass = column[String]("flypass", Nullable, O.SqlType("VARCHAR(255)"))
    def salarySystem = column[Int]("salary_system", Nullable)
    def employeeLevel = column[Int]("employee_level", O.Default(UserLevels.USER))
    def recordStatus = column[Int]("record_status", O.Default(RowStatus.ACTIVE))


    def fkEmployeeUser =
      foreignKey("fk_employee_user", userId, users)(_.id)

    def fkEmployeeCompany =
      foreignKey("fk_employee_company", companyId, companies)(_.id)

    def fkEmployeePosition = 
      foreignKey("fk_employee_position", positionId, positions)(_.id)

    def fkEmployeeShift = 
      foreignKey("fk_employee_shift", shiftId, shifts)(_.id)

    def fkEmployeeDepartment = 
      foreignKey("fk_employee_department", departmentId, departments)(_.id)

    def fkEmployeeUnion = 
      foreignKey("fk_employee_union", unionId, unions)(_.id)

    override def * =
      ( id.?, companyId, userId, positionId.?, shiftId.?, departmentId.?, unionId.?,  flypass.?, salarySystem.?, employeeLevel, recordStatus) <> (EmployeeEntity.tupled, EmployeeEntity.unapply)

  }

  //JOINS 

  //(EmployeeEntity,  (UserEntity, ContactProfileEntity))
  def employeesWithUsersWihtProfile = employees join usersWithProfile on (_.userId === _._1.id)

  //((((EmployeeEntity,  (UserEntity, ContactProfileEntity)), PositionEntity) , ShiftEntity),  DepartmentEntity), UnionEntity )
  def aggregatedEmployee = employeesWithUsersWihtProfile joinLeft
                             positions on (_._1.positionId === _.id) joinLeft
                             shifts on ( _._1._1.shiftId === _.id) joinLeft
                             departments on ( _._1._1._1.departmentId === _.id) joinLeft
                             unions on ( _._1._1._1._1.unionId === _.id) 


  //Queries 
  def employeeQry(companyId: Int, 
                  positionIds: List[Int],
                  shiftIds: List[Int],
                  departmentIds: List[Int],
                  unionIds:List[Int]) = {
    aggregatedEmployee.filter(t => (t._1._1._1._1._1.companyId === companyId  && t._1._1._1._1._1.recordStatus === RowStatus.ACTIVE) )
      .filteredBy( positionIds match { case List() => None; case list => Some(list) } )( _._1._1._1._1._1.positionId inSet _)
      .filteredBy( shiftIds match { case List() => None; case list => Some(list) } )( _._1._1._1._1._1.shiftId inSet _)
      .filteredBy( unionIds match { case List() => None; case list => Some(list) } )( _._1._1._1._1._1.unionId inSet _)
      .filteredBy( departmentIds match { case List() => None; case list => Some(list) } )( _._1._1._1._1._1.departmentId inSet _)
  }

  //CRUD EmployeeEntity
  def insertEmployee(empl: EmployeeEntity): Future[EmployeeEntity] = {
    db.run((employees returning employees.map(_.id)
                  into ((empl,id) => empl.copy(id=Some(id)))) += empl)
  }

  def getEmployeeById(id: Int): Future[EmployeeEntity] = {
    db.run(employees.filter(_.id === id).result.head)
  }

  def getEmployeeByUserId(userId: Int): Future[EmployeeEntity] = {
    db.run(employees.filter(_.userId === userId).result.head)
  }

  def updateEmployeeEntity(newEmpl: EmployeeEntity): Future[EmployeeEntity] = {
    db.run(employees.filter(_.id === newEmpl.id).update(newEmpl))
                    .map( num => newEmpl)
  }

  def softDeleteEmployeeEntityByUserId(userId: Int): Future[EmployeeEntity] = {
    getEmployeeByUserId(userId).flatMap(empl =>
          updateEmployeeEntity(empl.copy(recordStatus = RowStatus.DELETED)))
  }

  def softDeleteEmployeeEntityById(id: Int): Future[EmployeeEntity] = {
    getEmployeeById(id).flatMap(empl =>
          updateEmployeeEntity(empl.copy(recordStatus = RowStatus.DELETED)))
  }

  //EmployeeEntity filters


  def getUserIdByEmloyeeId(employeeId: Int): Future[Int] = {
    getEmployeeById(employeeId).map( e => e.userId)
  }
  
  def upsertEmployee(empl: EmployeeEntity): Future[EmployeeEntity] = {
    if(empl.id.isDefined) {updateEmployeeEntity(empl)}
    else {insertEmployee(empl)}
  }

  def getAllEmployeesWithUsersByCompanyId(companyId: Int): Future[List[(EmployeeEntity,  (UserEntity, ContactProfileEntity))]] = {
    db.run(employeesWithUsersWihtProfile.filter(t =>(t._1.companyId === companyId)).result).map(_.toList)
  }

  def getAllAggregatedEmployeesByCompanyId(companyId: Int, 
                                           positionIds: List[Int],
                                           shiftIds: List[Int],
                                           departmentIds: List[Int],
                                           unionIds:List[Int]
                                          // delegateIds: List[Int],
                                          // teamIds: List[Int]
                                           )
   : Future[List[(((((EmployeeEntity,  (UserEntity, ContactProfileEntity)), Option[PositionEntity]) , Option[ShiftEntity]),  Option[DepartmentEntity]), Option[UnionEntity])]] = {
    val baseQry = employeeQry(companyId,
                              positionIds,
                              shiftIds,
                              departmentIds,
                              unionIds)
    db.run(baseQry.sortBy(_._1._1._1._1._2._2.lastname.asc).result).map(_.toList)
  }

  def searchAllAggregatedEmployeesByCompanyId(companyId: Int,
                                              positionIds: List[Int],
                                              shiftIds: List[Int],
                                              departmentIds: List[Int],
                                              unionIds:List[Int],
                                             // delegateIds: List[Int],
                                             // teamIds: List[Int],
                                              pageSize: Int, 
                                              pageNr: Int, 
                                              searchTerm: Option[String] = None)
   : Future[PagedDBResult[(((((EmployeeEntity,  (UserEntity, ContactProfileEntity)), Option[PositionEntity]) , Option[ShiftEntity]),  Option[DepartmentEntity]), Option[UnionEntity])]] = {

    val baseQry = searchTerm.map { st =>
        val s = "%" + st + "%"
        employeeQry(companyId,
                    positionIds,
                    shiftIds,
                    departmentIds,
                    unionIds).filter { tup =>
          tup._1._1._1._1._2._1.username.like(s) ||
          tup._1._1._1._1._2._2.firstname.like(s) ||
          tup._1._1._1._1._2._2.lastname.like(s)
        }
      }.getOrElse(employeeQry(companyId,
                               positionIds,
                               shiftIds,
                               departmentIds,
                               unionIds)) // if search term is empty, do not filter
     
    val pageRes = baseQry
      .sortBy(_._1._1._1._1._2._2.lastname.asc)
      .drop(pageSize * (pageNr - 1))
      .take(pageSize)

    db.run(pageRes.result).flatMap( empList => 
        db.run(baseQry.length.result).map( totalCount => 
         PagedDBResult(
            pageSize = pageSize,
            pageNr = pageNr,
            totalCount = totalCount,
            data = empList)
          )
        )
  }

  def searchEmployeesWithUserWithContactProfileForTypeahead(companyId: Int, searchTerm: Option[String] = None): Future[List[(EmployeeEntity,  (UserEntity, ContactProfileEntity))]] = {
    def qry(cmpId: Int) = employeesWithUsersWihtProfile.filter(t =>(t._1.companyId === cmpId))
    val baseQry = searchTerm.map { st =>
        val s = "%" + st + "%"
        qry(companyId).filter{t => t._2._2.firstname.like(s) || 
                                   t._2._2.lastname.like(s) ||
                                   t._2._2.email.like(s)}
        .sortBy(_._1.id.asc)
      }.getOrElse(qry(companyId).sortBy(_._1.id.asc))  
    db.run(baseQry.result).map(_.toList)
  }

  def getEmployeeWithUserById(employeeId: Int): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    db.run(employeesWithUsersWihtProfile.filter(t =>(t._1.id === employeeId)).result.head)
  }

  def getEmployeeWithUserByUserId(userId: Int): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    db.run(employeesWithUsersWihtProfile.filter(t =>(t._1.userId === userId)).result.head)
  }

  def updateEmployeeWithUser(emplEntt: EmployeeEntity): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    updateEmployeeEntity(emplEntt).flatMap(updatedEmpl =>
        getEmployeeWithUserById(updatedEmpl.id.get))
  }

  def updateEmployeeEntityPosition(employeeId: Int, positionId: Option[Int]): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    getEmployeeById(employeeId).flatMap(empl =>
          updateEmployeeWithUser(empl.copy(positionId = positionId)))

  }

  def updateEmployeeEntityShift(employeeId: Int, shiftId: Option[Int]): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    getEmployeeById(employeeId).flatMap(empl =>
          updateEmployeeWithUser(empl.copy(shiftId = shiftId)))
  }

  def updateEmployeeEntityDepartment(employeeId: Int, departmentId: Option[Int]): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    getEmployeeById(employeeId).flatMap(empl =>
          updateEmployeeWithUser(empl.copy(departmentId = departmentId)))
  }

  def updateEmployeeEntityUnion(employeeId: Int, unionId: Option[Int]): Future[(EmployeeEntity,  (UserEntity, ContactProfileEntity))] = {
    getEmployeeById(employeeId).flatMap(empl =>
          updateEmployeeWithUser(empl.copy(unionId = unionId)))
  }
  
  def getEmployeeCountByCompanyId(companyId: Int): Future[Int] = {
   db.run(employees.filter(_.companyId === companyId).length.result)
  }
}
