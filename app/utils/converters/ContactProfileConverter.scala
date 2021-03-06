package utils.converters

import database.tables.ContactProfileEntity
import models.ContactProfile

object ContactProfileConverter {
  implicit class EntityToProfile(e: ContactProfileEntity) {
    def asProfile: ContactProfile = {
      ContactProfile(
        id = e.id,
        firstname = e.firstname,
        lastname = e.lastname,
        email = e.email,
        emailHome = e.emailHome,
        emailWork = e.emailWork,
        address = e.address,
        city = e.city,
        zipCode = e.zipCode,
        phoneNumberMobile = e.phoneNumberMobile,
        phoneNumberHome = e.phoneNumberHome,
        phoneNumberWork = e.phoneNumberWork,
        lastModified = e.lastModified)
    }
  }

  implicit class ContactProfileToEntity(profile: ContactProfile)  {
    def asEntity(profileId: Option[Int] = profile.id) : ContactProfileEntity = {
      ContactProfileEntity(
        id = profileId,
        firstname = profile.firstname,
        lastname = profile.lastname,
        email = profile.email,
        emailHome = profile.emailHome,
        emailWork = profile.emailWork,
        address = profile.address,
        city = profile.city,
        zipCode = profile.zipCode,
        phoneNumberMobile = profile.phoneNumberMobile,
        phoneNumberHome = profile.phoneNumberHome,
        phoneNumberWork = profile.phoneNumberWork,
        lastModified = profile.lastModified)
    }
  }
}
