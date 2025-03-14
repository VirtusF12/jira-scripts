/*
    Web-panel: панель справа списка согласующих (список согласующих берется из поля)
        atl.jira.view.issue.rigth.context
    Condition: условие видимости панели
        issue.getProjectObject().getKey().equals("ISOMS")
*/
import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.avatar.AvatarService
import com.atlassian.jira.config.properties.APKeys
import com.atlassian.jira.issue.Issue
import groovy.xml.MarkupBuilder

def issue = context.issue as Issue
def baseUrl = CA.applicationProperties.getString(APKeys.JIRA_BASEURL)
def avatarService = CA.getComponent(AvatarService)
def loggedInUser = CA.jiraAuthenticationContext.loggedInUser
def cfm = CA.getCustomFieldManager()
def cf = cfm.getCustomFieldObject(33003L)
def users = (issue.getCustomFieldValue(cf) as List<ApplicationUser>).collect { u -> u.getUsername() }
def entityProperties = issue.entityProperties

// for (String username : usernames) {
//     entityProperties.setBoolean(username, false)
// }

new MarkupBuilder(writer).table {

    for (String username : users) {

        ApplicationUser user = Users.getByName(username)
        def iconStatus = entityProperties.getBoolean(user.getUsername()) ? "/images/icons/emoticons/check.png" : "/images/icons/emoticons/error.png"

        tr {
            td(
                    class: 'jira-user-name user-hover jira-user-avatar jira-user-avatar-small',
                    rel: 'admin', 'id': 'project-vignette_admin',
                    style: 'margin: 1px 0px 1px 0px; height: 24px;',
                    href: "$baseUrl/secure/ViewProfile.jspa?name=$user.name"
            ) {
                span(
                        class: 'aui-avatar aui-avatar-small'
                ) {
                    span(
                            class: 'aui-avatar-inner'
                    ) {
                        img(
                                src: avatarService.getAvatarURL(user, user),
                                alt: user.name
                        )
                    }
                }
            }
            td(
                    user.displayName
            )
            td() {
                img(
                        style: 'height: 16px; width: 16px;',
                        src: iconStatus,
                        alt: ""
                )
            }
            // td() {
            //     img(
            //             style: 'height: 16px; width: 16px;',
            //             src: "/images/icons/emoticons/error.png",
            //             alt: ""
            //         )
            // }
        }
    }
}

// <img class="emoticon" src="/images/icons/emoticons/check.png" height="16" width="16" align="absmiddle" alt="" border="0">
// <img class="emoticon" src="/images/icons/emoticons/error.png" height="16" width="16" align="absmiddle" alt="" border="0">

writer.write(""" 
<script>
    \$(document).ready(function() {
        console.log('SOGL');
    });
</script>
""")

//   "AJS.\$('div.type-approval-control-field').removeClass('editable-field');"+
//   "AJS.\$('div.type-approval-control-field').find('span.aui-iconfont-edit').remove();"+
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
    Web-item: копка "Согласовать"
        jira.issue.tools
    Condition: условие видимости кнопки
        p.s. условие прописано скриптом ниже
    Action: действие при нажатии на кнопку
        /rest/scriptrunner/latest/custom/setFlagISOMS?issueKey=${issue.key}
*/

import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.user.ApplicationUser

def loggedInUser = CA.jiraAuthenticationContext.loggedInUser
def cfm = CA.getCustomFieldManager()
def cf = cfm.getCustomFieldObject(33003L)
def usernameLogged = loggedInUser.getUsername()

try {

    def users = (issue.getCustomFieldValue(cf) as List<ApplicationUser>).collect { u -> u.getUsername() }
    def entityProperties = issue.entityProperties

    if (    (issue.getProjectObject().getKey().equals("ISOMS")) &&
            (users.any { u -> u.equals(usernameLogged) }) &&
            (entityProperties.getBoolean(usernameLogged) == false)
    )  {
        return true
    } else {
        return false
    }

} catch (Exception e) {
    return false
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
    Description: после нажатия на кнопку Согласовать срабатывает Endpoint
    REST Endpoint
*/
import groovy.json.JsonBuilder
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.json.JsonOutput
import groovy.transform.BaseScript
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import javax.servlet.http.HttpServletRequest
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.bc.issue.IssueService
import com.atlassian.jira.issue.UpdateIssueRequest
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.component.ComponentAccessor as CA

@BaseScript CustomEndpointDelegate delegate

setFlagISOMS(httpMethod: "GET", groups: ["jira-administrators", "jira-users"]) { MultivaluedMap queryParams, String body, HttpServletRequest request ->

    final String baseUrl = CA.getApplicationProperties().getString("jira.baseurl")
    String issueKey = queryParams.getFirst('issueKey')
    def issue = Issues.getByKey(issueKey)
    def entityProperties = issue.entityProperties

    def loggedInUser = CA.jiraAuthenticationContext.loggedInUser
    def usernameLogged = loggedInUser.getUsername()
    log.error(usernameLogged)

    def cfm = CA.getCustomFieldManager()
    def cf = cfm.getCustomFieldObject(33003L)
    def usernames = (issue.getCustomFieldValue(cf) as List<ApplicationUser>).collect { u -> u.getUsername() }
    log.error(usernames)

    if (issue.getProjectObject().getKey().equals("ISOMS") && usernames.any { u -> u.equals(usernameLogged) } )  {

        def isSoglBefore = entityProperties.getBoolean(usernameLogged)
        log.error("isSoglBefore = ${isSoglBefore}")

        entityProperties.setBoolean(usernameLogged, true)

        def isSoglAfter = entityProperties.getBoolean(usernameLogged)
        log.error("isSoglAfter = ${isSoglAfter}")
    }

    def flag = [
            type : 'success',
            title: "Согласовано",
            close: 'auto',
            body : 'Ключ задачи <a href="/browse/'+ issueKey + '"> ' + issueKey + '</a>'
    ]

    return Response.ok(JsonOutput.toJson(flag)).build()
}
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/*
    Description: при создании задачи установить флаги состояний согласующих в false
    Post Function: на переходе (create)
*/
import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.user.ApplicationUser

def cfm = CA.getCustomFieldManager()
def cf = cfm.getCustomFieldObject(33003L)
def usernames = (issue.getCustomFieldValue(cf) as List<ApplicationUser>).collect { u -> u.getUsername() }
log.error(usernames)
def entityProperties = issue.entityProperties

for (String username : usernames) {
    entityProperties.setBoolean(username, false)
}

// log.error("properties was set...")

// def isSogl = entityProperties.getBoolean("Kovalenko_M")
// log.error(isSogl)