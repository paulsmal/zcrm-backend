package database.tables

import java.sql.Timestamp
import models.{UserLevels, RowStatus}
import slick.model.ForeignKeyAction.{Cascade, Restrict}
import scala.util.{Success, Failure, Try}
import exceptions.UsernameAlreadyExistException
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import org.mindrot.jbcrypt.BCrypt
import slick.profile.SqlProfile.ColumnOption.Nullable

case class UserEntity(
  id: Option[Int] = None,
  username: String,
  userLevel: Int,
  profileId: Int,
  recordStatus: Int = RowStatus.ACTIVE,
  updatedAt: Timestamp = new Timestamp(System.currentTimeMillis()))

case class PasswordEntity(
  userId: Int,
  password: String,
  editedAt: Timestamp)

trait UserDBComponent extends DBComponent {
    this: DBComponent 
    with  ContactProfileDBComponent =>

  import dbConfig.driver.api._

  val users = TableQuery[UserTable]
  val passwords = TableQuery[PasswordTable]
  
  class UserTable(tag: Tag) extends Table[UserEntity](tag, "tbl_user") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userLevel = column[Int]("user_level", O.Default(UserLevels.USER))
    def username = column[String]("username", O.SqlType("VARCHAR(255)"))
    def profileId = column[Int]("contact_profile_id")    
    def recordStatus = column[Int]("record_status", O.Default(RowStatus.ACTIVE))
    def updatedAt = column[Timestamp]("updated_at", Nullable)

    def fkContactProfile= foreignKey("fk_user_contact_profile", profileId, contactProfiles)(_.id)
    def idxUsername = index("username_uniq", username, unique = true)

    def * = (id.?, username, userLevel, profileId, recordStatus, updatedAt) <>
      (UserEntity.tupled, UserEntity.unapply)
  }

  class PasswordTable(tag: Tag) extends Table[PasswordEntity](tag, "tbl_passwords") {
    def userId = column[Int]("user_id")
    def password = column[String]("password", O.SqlType("VARCHAR(255)"))
    def editedAt = column[Timestamp]("edited_at")

    def fkUserId = foreignKey("fk_password_user_id", userId, users)(_.id, onUpdate = Restrict, onDelete = Cascade) 

    def * = (userId, password, editedAt) <> (PasswordEntity.tupled, PasswordEntity.unapply)
  }

  //JOINs 
  def usersWithProfile = users join contactProfiles on (_.profileId === _.id)

  //CRUD UserEntity
  def insertUser(user: UserEntity): Future[UserEntity] = {
    db.run(users.filter(_.username === user.username).result.head)
        .recoverWith{ case ex =>
           db.run(((users returning users.map(_.id) into ((user,id) => user.copy(id=Some(id)))) += user))
        }
  }

  def getUserById(id: Int): Future[UserEntity] = {
    db.run(users.filter(_.id === id).result.head)
  }

  def getUserByUserUsername(username: String): Future[UserEntity] = {
    db.run(users.filter(_.username === username ).result.head)
  }

  def updateUser(user: UserEntity): Future[UserEntity] = {
      db.run(users.filter(_.id === user.id).update(user))
        .map( num => user)
  }

  def softDeleteById(userId: Int): Future[UserEntity] = {
    getUserById(userId).flatMap(res =>
        updateUser(res.copy(recordStatus = RowStatus.DELETED, 
                  updatedAt = new Timestamp(System.currentTimeMillis()))))

  } 

  def softDeleteByUserName(userName: String): Future[UserEntity] = {
    getUserByUserUsername(userName).flatMap(res =>
        updateUser(res.copy(recordStatus = RowStatus.DELETED, 
                  updatedAt = new Timestamp(System.currentTimeMillis()))))

  }

  //CRUD PasswordEntity
  private def insertPassword(user: UserEntity, password: String): Future[PasswordEntity] = {
    val hashedPwd = BCrypt.hashpw(password, BCrypt.gensalt())
    val pwdEntt = PasswordEntity(user.id.get, hashedPwd, new Timestamp(System.currentTimeMillis()))
    db.run(passwords += pwdEntt).map( num => pwdEntt)
  }


  
  //USER FILTERS 
  def upsertUser(user: UserEntity): Future[UserEntity] = {
    if(user.id.isDefined) {
      updateUser(user)
    } else {
      insertUser(user)
    }
  }

  def getUserWithProfileByUserId(id: Int): Future[(UserEntity, ContactProfileEntity)] = {
    db.run(usersWithProfile.filter(_._1.id === id).result.head)
  }

  def checkUserStatusById(userId: Int): Future[Boolean] = {
     getUserById(userId).map(user =>
       user.recordStatus == RowStatus.ACTIVE)
  }

  def checkUserStatusByUserName(userName: String): Future[Boolean] = {
    getUserByUserUsername(userName).map(user =>
      user.recordStatus == RowStatus.ACTIVE)
  }

  def isUserExists(userName: String): Future[Boolean] = {
    getUserByUserUsername(userName).map(userEntt => true)
     .recover{ case ex => false } 
  }


  //PWD FILTERS 
  def setUserPassword(userId: Int, password: String): Future[UserEntity] = {
    getUserById(userId).flatMap( user =>
           insertPassword(user, password)
             .map( pwd => user)
               .recover{ case ex => user })
  }

  def getPasswordByUsername(username: String): Future[PasswordEntity] = {
    getUserByUserUsername(username).flatMap( user =>
        db.run(passwords.filter(_.userId === user.id.get).sortBy(_.editedAt.desc).result.head))
  }

  def checkPassword(username: String, rawPassword: String): Future[Boolean] = {
    getPasswordByUsername(username).map( hash =>
                                 BCrypt.checkpw(rawPassword, hash.password))
  }
}