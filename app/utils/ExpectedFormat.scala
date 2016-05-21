package utils

import play.api.libs.json.Json
import scala.collection.immutable.ListMap

object ExpectedFormat {

  val expectedContactProfileFormat = Json.toJson(ListMap(
    "id"                -> "[O] (int) id of the profile",
    "firstname"         -> "[O] (string)",
    "lastname"          -> "[O] (string)",
    "email"             -> "[O] (string)",
    "address"           -> "[O] (string)",
    "city"              -> "[O] (string)",
    "zipCode"           -> "[O] (string)",
    "phoneNumberMobile" -> "[O] (string)",
    "phoneNumberHome"   -> "[O] (string)",
    "phoneNumberWork"   -> "[O] (string)"
  ))

  val expectedInviteEmployeeFormat = Json.toJson(ListMap(
      "username"                -> Json.toJson("[M] (string) The desired username, normally email"),
      "baseUrl"                 -> Json.toJson("[M] (string) The baseUrl onto which to append the token and user id"),
      "contactProfile"          -> expectedContactProfileFormat,
      "employeeLevel"           -> Json.toJson("[O] (int) The user level, defaults to employee (9999)"),
      "employeeType"            -> Json.toJson("[O] (string) The type of this employee, if not set, no type is set")
  ))

  val expectedEmployeeFormat = Json.toJson(ListMap(
      "id"                      -> Json.toJson("[O] (int) Employee Id"),
      "user"                    -> expectedUserFormat,
      "companyId"               -> Json.toJson("[M] (int) Company Id for user"),
      "employeeType"            -> Json.toJson("[O] (string) "),
      "employeeLevel"           -> Json.toJson("[M] (int) Level of employee")
    ))

  val expectedLoginRqFormat = Json.toJson(ListMap(
      "username" -> "[M] (string) Username",
      "password" -> "[M] (string) Password"
  ))

  val expectedSendEmailRqFormat = Json.toJson(ListMap(
    "email" -> "[M] (string) The email to send the signup link to",
    "url"   -> "[M] (string) The url to use in the link"))


  val expectedActivateUserRqFormat = Json.toJson(ListMap(
    "token"           -> Json.toJson("[M] (string) The token used received in the activation email"),
    "password"        -> Json.toJson("[M] (string) The desired password"),
    "email"           -> Json.toJson("[M] (string) The email used to register"),
    "companyName"     -> Json.toJson("[M] (string) The desired company name"),
    "vatId"           -> Json.toJson("[M] (string) A valid vatId(organisationsnmr in sweden)"),
    "contactProfile"  -> Json.toJson(ListMap(
        "firstname"         -> "[O] (string) Person firstname",
        "lastname"          -> "[O] (string) Person lastname",
        "address"           -> "[O] (string) address",
        "city"              -> "[O] (string) city",
        "zipCode"           -> "[O] (string) numeric zipCode",
        "phoneNumberHome"   -> "[O] (string) numeric, space and '-'",
        "phoneNumberMobile" -> "[O] (string) numeric, space and '-'",
        "phoneNumberWork"   -> "[O] (string) numeric, space and '-'"))))



  val expectedUserFormat = Json.toJson(ListMap(
    "id" -> Json.toJson("[M](int) user id"),
    "username" -> Json.toJson("[M](string) username"),
    "userLevel" -> Json.toJson("[M](int) user level"),
    "contactProfile" -> expectedContactProfileFormat
  ))

  val expectedSetPasswordUsingToken = Json.toJson(
    ListMap("password" -> "[M](string) The password to set",
            "token" -> "[M](string) The token used to validate")
  )

  val expectedMailToSendFormat = Json.toJson(ListMap(
      "subject" -> "[O] (string) subject ",
      "body" -> "[O] (string) body: HTML",
      "to" -> "[M] (List[string]) TO:<email>"
  ))

  val expectedExtMailIdFormat = Json.toJson(ListMap(
    "id" -> "[M](string) Exchange unique Id"
  ))

  val expectedTeamFormat = Json.toJson(ListMap(
    "companyId"      -> Json.toJson("[M](int) company ID"),
    "name"           -> Json.toJson("[M](string) Team name"),
    "description"    -> Json.toJson("[O](string) Team description")
  ))

  val expectedUnionFormat = Json.toJson(ListMap(
    "companyId"      -> Json.toJson("[M](int) company ID"),
    "name"           -> Json.toJson("[M](string) Union name"),
    "description"    -> Json.toJson("[O](string) Union description")
  ))

  val expectedShiftFormat = Json.toJson(ListMap(
    "companyId"      -> Json.toJson("[M](int) company ID"),
    "name"           -> Json.toJson("[M](string) Shift name")
  ))

  val expectedDepartmentFormat = Json.toJson(ListMap(
    "companyId"      -> Json.toJson("[M](int) company ID"),
    "name"           -> Json.toJson("[M](string) Department name")
  ))

  val expectedTaskFormat =  Json.toJson(ListMap(
    "companyId"      -> Json.toJson("[M](int) company ID"),
    "createdByUser"  ->  expectedUserFormat,
    "assignedToUser" ->  expectedUserFormat,
    "title"          -> Json.toJson("[M](string) Task title"),
    "description"    -> Json.toJson("[O](string) Task description"),
    "status"         -> Json.toJson("[O](string) One of the task statuses: NEW|OPEN|POSTPONED|RESOLVED"),
    "attachedMails"  -> Json.toJson(List(ListMap(
        "Id"         -> "[M](string) Exchange mail ID",
        "subject"    -> "[M](string) Mail subject",
        "fromEmail"  -> "[M](string) Mail sender email"))),
    "dueDate"        -> Json.toJson("[O](string) Task to due date") 
  ))

  val expectedExchangeMailFormat = Json.toJson(ListMap(
    "extId" -> Json.toJson("[M](string) Exchange UniqueID"),
    "conversationExtId" -> Json.toJson("[M](string) Exchange Conversation ID"),
    "mailboxId" -> Json.toJson("[M](int) Mailbox ID"),
    "sender" -> Json.toJson("[M](string) Sender"),
    "receivedBy" -> Json.toJson("[M](string) Received by"),
    "ccRecipients" -> Json.toJson("[O](string) Copies divided by , "),
    "bccRecipients" -> Json.toJson("[O](string) Blind carbon copies divided by , "),
    "subject" -> Json.toJson("[M](string) Mail subject"),
    "body" -> Json.toJson("[M](string) Mail body"),
    "importance" -> Json.toJson("[O](string) Importance"),
    "attachments" -> Json.toJson("[O](string) Attachments"),
    "size" -> Json.toJson("[O](int) Mail size"),
     "received" -> Json.toJson("[M](timestamp) Mail received at")
  ))

  val expectedMailboxFormat = Json.toJson(ListMap(
  "userId"         -> Json.toJson("[M](int) User id"),
  "server"         -> Json.toJson("[M](string) Exchange server address(full)"),
  "login"          -> Json.toJson("[M](string) Exchange mailbox login formatted like username@domain"),
  "password"       -> Json.toJson("[M](string) Exchange mailbox password")
  ))

  val expectedPositionFormat = Json.toJson(ListMap(
  "id"                      -> Json.toJson("[O] (int) Position Id"),
  "companyId"               -> Json.toJson("[M] (int) Company Id"),
  "name"                    -> Json.toJson("[M] (string) name ")
  ))

  val expectedDelegateFormat = Json.toJson(ListMap(
    "companyId" -> Json.toJson("[M] (int) Company Id"),
    "name"      -> Json.toJson("[M] (string) Delegate name")
  ))

  val expectedDelegateGroupFormat = Json.toJson(ListMap(
    "delegateId"    -> Json.toJson("[M] (int) Delegate Id"),
    "userId"    -> Json.toJson("[M] (int) User Id"),
    "startDate" -> Json.toJson("[O] (TS) start date"),
    "endDate"   -> Json.toJson("[O] (TS) end date ")
  ))

}

