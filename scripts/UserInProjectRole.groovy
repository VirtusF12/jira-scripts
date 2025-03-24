import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.project.Project
import com.atlassian.jira.project.ProjectManager
import com.atlassian.jira.security.roles.ProjectRole
import com.atlassian.jira.security.roles.ProjectRoleManager
import com.atlassian.jira.user.ApplicationUser

String username = "username"
String projectKey = "RI"

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