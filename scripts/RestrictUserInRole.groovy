import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem
import com.atlassian.jira.project.Project
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser

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

def currentUser = CA.getJiraAuthenticationContext().getLoggedInUser()
log.error("currentUser = ${currentUser.username}")
def project = CA.getProjectManager().getProjectByCurrentKey(keyProject)
def userRoles =  CA.getComponent(ProjectRoleManager).getProjectRoles(currentUser, project)
userRoles.each { role ->
    log.error("role.name = ${role.name}")
}



log.error("Сотрудник отдела клиентского опыта = ${isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project)}")
log.error("!Administrators = ${!isUserInProjectRole(currentUser, "Administrators", project)}")

getFieldScreen().getTabs().each { tab ->

    tab.getFieldScreenLayoutItems().each { fsli ->


        // Роль «Сотрудник отдела клиентского опыта» может вносить изменения в поля «Влияние на опыт клиента», «Влияние на опыт сотрудника»
        if (isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project)
                &&
                !isUserInProjectRole(currentUser, "Administrators", project)
        ) {

            if (fsli.getFieldId() in ["customfield_33602", "customfield_33603"]) { // Влияние на опыт клиента, Влияние на опыт сотрудника
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            }
        }

        // Роль «Куратор страт. Проектов» может вносить изменения в поле «Стратегическая значимость проекта»
        if (isUserInProjectRole(currentUser, "Куратор страт. Проектов", project)
                &&
                !isUserInProjectRole(currentUser, "Administrators", project)
        ) {

            if (fsli.getFieldId() in ["customfield_33604"]) { // Стратегическая значимость проекта
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            }
        }

    }
}