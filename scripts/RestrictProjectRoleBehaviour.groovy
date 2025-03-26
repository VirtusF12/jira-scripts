import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.screen.FieldScreenLayoutItem
import com.atlassian.jira.project.Project
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.permission.ProjectPermissions

def keyProject = issueContext.getProjectObject().getKey()

if ( !(keyProject in ["RI", "PRODF", "RPBL"]) ) return

def hasProjectEdit(Issue issue, Project project, ApplicationUser user) {

    def hasProjectEdit = project.permissionScheme.getPermissions(ProjectPermissions.EDIT_ISSUES).findAll {
        !(it.holder.getParameter().getOrNull() in ['12600','12601'])
    }.any {
        def type = it.holder.getType().toString()
        log.error("type = ${type}")
        switch(type) {

            case "REPORTER":
                issue.reporter?.name == user.name
                break

            case "PROJECT_LEAD":
                it.holder.getParameter().getOrNull()
                project.projectLead?.name == user.name
                break

            case "PROJECT_ROLE":
                def idRole = it.holder.getParameter().getOrNull().toString()
                def projectRoleManager = CA.getComponent(ProjectRoleManager)
                Collection<ProjectRole> userRoles = projectRoleManager.getProjectRoles(user, project)
                userRoles.findAll { !(it.id in [12600,12601]) }.collect { it.id }.any { it.toString().equals(idRole) }
                break

            case "GROUP":
                def group = it.holder.getParameter().getOrNull().toString()
                CA.getGroupManager().isUserInGroup(user, group)
                break

            case "USER_CUSTOM_FIELD":
                def cf = it.holder.getParameter().getOrNull()
                log.error(cf)
                if (cf != null) {
                    try {
                        def usersCf = issue.getCustomFieldValue(cf.toString().split("_")[1] as Long) as ApplicationUser[]
                        if (usersCf != null) {
                            usersCf.any { it.name == user.name }
                        }
                    } catch (Exception e) {
                        def userCf = issue.getCustomFieldValue(cf.toString().split("_")[1] as Long) as ApplicationUser
                        if (userCf != null) {
                            userCf.name == user.name
                        }
                    }
                }
                break

            case "ASSIGNEE":
                issue.assignee?.name == user.name
                break

            case "USER":
                def userkey = it.holder.getParameter().getOrNull()
                if(userkey != null) {
                    def userKey = Users.getByKey(userkey.toString())
                    userKey?.name == user.name
                }
                break

            default:
                false
        }
    }

    return hasProjectEdit
}

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

        if (isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project)
                &&
                !hasProjectEdit(underlyingIssue, project, currentUser)
        ) {
            if (fsli.getFieldId() in ["customfield_33602", "customfield_33603"]) { // Влияние на опыт клиента, Влияние на опыт сотрудника
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            }
        }
        if ( hasProjectEdit(underlyingIssue, project, currentUser) &&
                !(isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project) || isUserInProjectRole(currentUser, "Administrators", project) )

        ) {
            if (fsli.getFieldId() in ["customfield_33602", "customfield_33603"]) { // Влияние на опыт клиента, Влияние на опыт сотрудника
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        if (isUserInProjectRole(currentUser, "Куратор страт. Проектов", project)
                &&
                !hasProjectEdit(underlyingIssue, project, currentUser)
        ) {
            if (fsli.getFieldId() in ["customfield_33604"]) { // Стратегическая значимость проекта
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            }
        }
        if (isUserInProjectRole(currentUser, "Куратор страт. Проектов", project)
                &&
                hasProjectEdit(underlyingIssue, project, currentUser)
        ) {
            if (fsli.getFieldId() in ["customfield_33602", "customfield_33603"]) {
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if ((isUserInProjectRole(currentUser, "Куратор страт. Проектов", project) && isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project) )
                &&
                !hasProjectEdit(underlyingIssue, project, currentUser)
        ) {
            if (fsli.getFieldId() in ["customfield_33604", "customfield_33602", "customfield_33603"]) {
                getFieldById(fsli.getFieldId()).setReadOnly(false)
            } else {
                getFieldById(fsli.getFieldId()).setReadOnly(true)
            }
        }

        if ((isUserInProjectRole(currentUser, "Куратор страт. Проектов", project) && isUserInProjectRole(currentUser, "Сотрудник отдела клиентского опыта", project) )
                &&
                hasProjectEdit(underlyingIssue, project, currentUser)
        ) {
            getFieldById(fsli.getFieldId()).setReadOnly(false)
        }
    }
}