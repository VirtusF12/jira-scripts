import com.atlassian.confluence.security.SpacePermission
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.security.SpacePermission
import com.atlassian.confluence.spaces.SpaceManager
import com.atlassian.sal.api.component.ComponentLocator
import com.atlassian.confluence.security.Permission
import com.atlassian.confluence.security.PermissionManager
import com.atlassian.confluence.user.ConfluenceUser
import com.atlassian.confluence.user.UserAccessor
import com.atlassian.user.GroupManager

def userAccessor = ComponentLocator.getComponent(UserAccessor)
def groupManager = ComponentLocator.getComponent(GroupManager)
def spaceManager = ComponentLocator.getComponent(SpaceManager)
def spacePermissionManager = ComponentLocator.getComponent(PermissionManager)
def group = groupManager.getGroup('TEC-All-Users')
// log.warn(userAccessor.getMembers(group).asList().size())
def listUsers = userAccessor.getMembers(group).asList()
def outputFilePath = "/var/atlassian/application-data/confluence/export/tech_all_users_perm_4.csv"

FileWriter csvfile = new FileWriter(outputFilePath)
csvfile.write('Space name, Username, User Full Name, Edit, View, Export, Admin\n')
listUsers.each { user ->
    // ConfluenceUser user = userAccessor.getUserByName(username.toString())
    log.warn(user.getFullName())
    spaceManager.allSpaces.each { space ->
        def isEdit = spacePermissionManager.hasPermission(user, Permission.EDIT, space)
        def isExport = spacePermissionManager.hasPermission(user, Permission.EXPORT, space)
        def isView = spacePermissionManager.hasPermission(user, Permission.VIEW, space)
        def isAdmin = spacePermissionManager.hasPermission(user, Permission.ADMINISTER, space)
        if( isEdit || isView || isAdmin || isExport){
            String row = """"${space.getName()}", ${user.getName()}, ${user.getFullName()}, ${isEdit}, ${isView}, ${isExport}, ${isAdmin}\n"""
            log.error(row)
            csvfile.write(row)
        }
    }
}
csvfile.close()