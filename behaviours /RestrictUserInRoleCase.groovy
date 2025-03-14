import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem
import com.atlassian.jira.project.Project
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.project.ProjectManager

/*
    Ограничение редактирование полей по ролям
*/
def currentUser = CA.getJiraAuthenticationContext().getLoggedInUser()
def keyProject = issueContext.getProjectObject().getKey()

if ( !(keyProject in ["RI", "PRODF", "RPBL"]) ) return

boolean isUserInProjectRole(ApplicationUser user, String role, Project project) {

    def projectRoleManager = CA.getComponent(ProjectRoleManager)

    try {

        return projectRoleManager.
                isUserInProjectRole(
                        user,
                        projectRoleManager?.getProjectRole(role),
                        project)
    } catch (Exception ex) {

        return false
    }
}

def setReadonlyCustomField(FieldScreenLayoutItem item, String idCustomField, boolean isReadonly) {

    if (item.getFieldId().equals(idCustomField)) {
        if (isReadonly) {
            getFieldById(item.getFieldId()).setReadOnly(false)
        } else {
            getFieldById(item.getFieldId()).setReadOnly(true)
        }
    }
}

def project = CA.getProjectManager().getProjectByCurrentKey(keyProject)

getFieldScreen().getTabs().each { tab ->

    tab.getFieldScreenLayoutItems().each { fsli ->

        log.error("fieldId = ${fsli.getFieldId()}")

        setReadonlyCustomField(fsli, "customfield_33602",  // Влияние на опыт клиента
                isUserInProjectRole(currentUser, "Administrators", project)
                        ||
                        isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project))

        setReadonlyCustomField(fsli, "customfield_33603",  // Влияние на опыт сотрудника
                isUserInProjectRole(currentUser, "Administrators", project)
                        ||
                        isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project))

        setReadonlyCustomField(fsli, "customfield_33604", // Стратегическая значимость проекта
                isUserInProjectRole(currentUser, "Administrators", project)
                        ||
                        isUserInProjectRole(currentUser, "Куратор страт. Проектов", project))

        setReadonlyCustomField(fsli, "customfield_33606", // Работы начаты
                isUserInProjectRole(currentUser, "Administrators", project))
    }
}

