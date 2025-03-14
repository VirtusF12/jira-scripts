// REPDOC дата приемки/ поле "Дата сдачи работ".
import java.text.SimpleDateFormat
import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.user.UserLocaleStore
import groovy.transform.BaseScript
import com.onresolve.jira.groovy.user.FieldBehaviours

@BaseScript FieldBehaviours fieldBehaviours

def currentUser = CA.getJiraAuthenticationContext().loggedInUser
def userLanguage = CA.getComponent(UserLocaleStore).getLocale(currentUser)
def sdf = new SimpleDateFormat("dd/MMM/yy", userLanguage)
log.error(userLanguage)
def currentField = getFieldById(getFieldChanged())
def duField = getFieldByName("Срок для приемки в днях")
def dateField = getFieldByName("Дата приемки")
def Integer duration = duField.getFormValue() as Integer
Date date = sdf.parse(currentField.getFormValue() as String)
// log.warn(date.plus(duration).toLocalDateTime().getDayOfWeek())
// log.warn(issue.getCustomFieldValue(field).plus(5).toLocalDateTime().getDayOfWeek())
if (date.plus(duration).toLocalDateTime().getDayOfWeek() as String == "SUNDAY") {
    duration = duration + 1
} else if (date.plus(duration).toLocalDateTime().getDayOfWeek() as String == "SATURDAY") {
    duration = duration + 2
} else {
    log.warn(duration)
}
dateField.setFormValue(date.plus(duration).format("dd/MMM/yy"))