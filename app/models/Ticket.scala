package models

case class Ticket(id: Option[String] = None,
                  companyId: Int,
                  projectId: Option[Int] = None, 
                  createdByUserId: Int,
                  createdByUser: Option[User] = None,
                  //requestedByUserId: Option[Int] = None,
                  members: Option[List[User]] = None, 
                  teams: Option[List[Team]] = None,
                  clients: Option[List[Client]] = None, 
                  //requestedByEmail: Option[String] = None,
                  //assignedToUserId: Option[Int] = None,
                  //assignedToTeamId: Option[Int] = None,
                  status: Int,
                  priority: Int,
                  subject: String,
                  description: Option[String] = None)
