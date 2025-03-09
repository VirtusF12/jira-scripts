import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.user.ApplicationUser
import com.onresolve.scriptrunner.runner.rest.common.CustomEndpointDelegate
import groovy.transform.BaseScript
import javax.ws.rs.core.MultivaluedMap
import javax.ws.rs.core.Response
import groovy.json.JsonSlurper
import groovy.json.*
import groovy.json.*
import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.link.IssueLink

/*
    nginx:
    location = /rest/tempo-timesheets/4/worklogs/ {

                if ($request_method = POST) {
                        return 307 https://$server_name/rest/scriptrunner/latest/custom/validateTempoRequest;
                }
        }
*/

def baseUrl = CA.getApplicationProperties().getString("jira.baseurl")
final allowedGroups = ["jira-administrators"," jira-users"]

def sendOriginRequest(String url, String data) {

    def request = new URL(url).openConnection()
    def user_pass = "username:password"
    def auth = user_pass.bytes.encodeBase64().toString()

    request.setRequestMethod("POST")
    request.setDoOutput(true)
    request.setRequestProperty("Content-Type", "application/json")
    request.setRequestProperty("Authorization", "Basic $auth")
    request.getOutputStream().write(data.getBytes("UTF-8"))

    def rc = null
    def text = null
    def stream = null
    def result = null
    def slurper = new JsonSlurper()

    try {
        rc = request.getResponseCode()
        stream = request.getErrorStream() ?: request.getInputStream()
        text = stream.getText()
        result = slurper.parseText(text)
        log.error("exc ===> ${result}")
    } catch(Exception exc) {
        log.error("exc ===> ${exc}")
        throw exc
    }

    return result
}

/*
    Запрет списания: issue -> epic -> epic ROI (status)
*/
def checkValidTaskEpicEpic(Issue issue, String projectKey, String status, CustomFieldManager cfm) {

    def issueEpic = null
    def isEpicROI = false

    try {
        issueEpic = Issues.getByKey(issue.getCustomFieldValue(cfm?.getCustomFieldObjectsByName('Epic Link')[0]).toString())
    } catch(Exception e) {
        issueEpic = null
    }

    if (issueEpic != null) {
        List<IssueLink> links = CA.getIssueLinkManager().getInwardLinks(issueEpic.getId())
        isEpicROI = links.any { it.getSourceObject().getProjectObject().getKey().equals(projectKey) && it.getSourceObject().getStatus().getName().equals(status) }
    }

    if (issueEpic != null && isEpicROI) return true

    return false
}


boolean isValidRequest(ApplicationUser user, Issue issue) {

    /*
        Прописать все кейсы запрета на отправку оригинального запроса в Tempo
        Confluence: https://confluence.tools.ad.ru/pages/viewpage.action?pageId=695799541
    */

    def cfm = CA.getCustomFieldManager()

    if ( checkValidTaskEpicEpic(issue, "ROI", "Новая", cfm) ) {

        return false
    }

    return true
}

@BaseScript CustomEndpointDelegate delegate

validateTempoRequest(httpMethod: "POST", groups: allowedGroups) { MultivaluedMap queryParams, String body, HttpServletRequest request ->

    def object = new JsonSlurper().parseText(body)
    log.error("worker: ${object.worker}")
    log.error("originTaskId: ${object.originTaskId}")

    def userWorker = CA.getUserManager().getUserByKey(object.worker) as ApplicationUser
    log.error("userWorker = ${userWorker.username}")
    def issueWorkLog = CA.getIssueManager().getIssueObject(Long.parseLong(object.originTaskId)) as Issue
    log.error("issueWorkLog.key = ${issueWorkLog.key}")

    if ( isValidRequest(userWorker, issueWorkLog) ) {

        /* Отправить оригинальный запрос в Tempo */
        def result = sendOriginRequest("${baseUrl}/rest/tempo-timesheets/4/worklogs", body)
        log.error("Отправка запроса...")

        return Response.ok(JsonOutput.toJson(result)).build()

    } else {
        return Response.status(400).build()
    }
}