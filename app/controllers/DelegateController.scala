package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import utils.ExpectedFormat._
import database.DelegateDBRepository
import models.Delegate

class DelegateController @Inject() extends CRMController {
  import utils.JSFormat.delegateFrmt

  def getDelegate(companyId: Int, delegateId: Int) = CRMActionAsync { rq =>
    DelegateDBRepository.getDelegateById(delegateId).map(Json.toJson(_))
  }

  def newDelegate(companyId: Int) = CRMActionAsync[Delegate](expectedDelegateFormat) { rq =>
    DelegateDBRepository.createDelegate(rq.body, companyId).map(delegate => Json.toJson(delegate))
  }

  def updateDelegate(companyId: Int, delegateId: Int) = CRMActionAsync[Delegate](expectedDelegateFormat) { rq =>
    DelegateDBRepository.updateDelegate(rq.body.copy(id = Some(delegateId)), companyId).map(Json.toJson(_))
  }

  def deleteDelegateById(companyId: Int, delegateId: Int) = CRMActionAsync { rq =>
    DelegateDBRepository.deleteDelegate(delegateId).map(Json.toJson(_))
  }

  def getAllDelegates(companyId: Int) = CRMActionAsync { rq =>
    DelegateDBRepository.getDelegatesByCompanyId(companyId).map(Json.toJson(_))
  }


}
