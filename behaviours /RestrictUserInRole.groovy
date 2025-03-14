import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem
import com.atlassian.jira.project.Project
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.project.ProjectManager

/*
    Для поля «Верификация ДИД» настроить ограничение заполнения, в рамках которого данное
    поле может заполнять пользователь с ролью Администратора или Исполнитель, указанный в задаче DID
*/

def currentUser = CA.getJiraAuthenticationContext().getLoggedInUser()
def keyProject = issueContext.getProjectObject().getKey()

if ( !(keyProject in ["DID"]) ) return

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

        setReadonlyCustomField(fsli, "customfield_33611", // Верификация ДИД
                isUserInProjectRole(currentUser, "Administrators", project)
                        ||
                        (currentUser.getUsername() in [getUnderlyingIssue()?.getAssignee()?.getUsername(), getFieldById("assignee").value.toString()]))
    }
}

