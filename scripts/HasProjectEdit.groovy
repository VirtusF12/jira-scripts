import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser

import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.security.PermissionManager
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.issue.Issue

import com.atlassian.jira.permission.ProjectPermissions


def getListRolesUser(String username, String projectKey) {

    def projectRoleManager = CA.getComponent(ProjectRoleManager)
    def projectManager = CA.getProjectManager()
    ApplicationUser user = Users.getByName(username)
    Project project = projectManager.getProjectObjByKey(projectKey)
    Collection<ProjectRole> userRoles = projectRoleManager.getProjectRoles(user, project)

    log.error("Роли пользователя ${user.name} в проекте ${project.name}:")
    userRoles.each { role ->
        log.error("- ${role.name}")
    }

    return userRoles.collect { it.name }
}

String issueKey = "RI-3177"
String username = "Natalia.Kenikh" // "Sofiya.Cheremkhina"
String projectKey = "RI"
log.error(getListRolesUser(username,projectKey))

def issueService = ComponentAccessor.getComponent(IssueService)
def permissionManager = ComponentAccessor.getComponent(PermissionManager)
def userManager = ComponentAccessor.getUserManager()

ApplicationUser user = Users.getByName(username)
def issue = Issues.getByKey(issueKey)
def project = CA.getProjectManager().getProjectByCurrentKey(projectKey)
log.error(project.permissionScheme.getPermissions(ProjectPermissions.EDIT_ISSUES))

def hasProjectEdit(Issue issue, Project project, ApplicationUser user) {

    def hasProjectEdit = project.permissionScheme.getPermissions(ProjectPermissions.EDIT_ISSUES).findAll {
        !(it.holder.getParameter().getOrNull() == '12600')
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
                userRoles.findAll { it.id != 12600 }.collect { it.id }.any { it.toString().equals(idRole) }
                break

            case "GROUP":
                def group = it.holder.getParameter().getOrNull().toString()
                CA.getGroupManager().isUserInGroup(user, group)
                break

            case "USER_CUSTOM_FIELD":
                def cf = it.holder.getParameter().getOrNull()
                log.error(cf)
                if (cf != null) {
                    def usersCf = issue.getCustomFieldValue(cf.toString().split("_")[1] as Long) as ApplicationUser[]
                    if (usersCf != null) {
                        usersCf.any { it.name == user.name }
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

log.error("hasProjectEdit = ${hasProjectEdit(issue, project, user)}")

// def hasProjectEdit = project.permissionScheme.getPermissions(ProjectPermissions.EDIT_ISSUES).each {
//     log.error(it.holder.getType().toString() + " : " + it.holder.getParameter().toString())
//     log.error(it.holder.getParameter().getOrNull() == '12600')
//  }

// def hasProjectEdit = project.permissionScheme.getPermissions(ProjectPermissions.EDIT_ISSUES).findAll {
//     !(it.holder.getParameter().getOrNull() == '12600')
// }
// log.error(hasProjectEdit)

// hasProjectEdit.each {
//     log.error(it.holder.getType().toString() + " : " + it.holder.getParameter().toString())
//     // log.error(it.holder.getParameter().getOrNull() == '12600')
//  }

