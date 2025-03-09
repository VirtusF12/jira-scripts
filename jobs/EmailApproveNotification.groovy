
/* 
    Оповестим пользователей о необходимости согласования задач
*/

import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.mail.Email
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.jira.user.ApplicationUser
import groovy.sql.Sql
import org.ofbiz.core.entity.ConnectionFactory
import org.ofbiz.core.entity.DelegatorInterface
import com.atlassian.jira.issue.Issue
import java.sql.Connection
import com.atlassian.jira.mail.*
import com.atlassian.mail.queue.MailQueue
import com.atlassian.mail.queue.MailQueueItem
import com.atlassian.jira.mail.MailServiceQueueItemBuilder
import com.atlassian.jira.notification.NotificationRecipient

String baseUrl = CA.getApplicationProperties().getString("jira.baseurl")
def asUser = CA.userManager.getUserByName("atl-service-r00")

SMTPMailServer mailServer = CA.getMailServerManager().getDefaultSMTPMailServer()
Map issues = [:]
Map approval_map = [2:"Согласование систем", 3:"Согласование КА", 4:"Согласование ДИБ", 5:"Согласование СА"]

def delegator = (DelegatorInterface) CA.getComponent(DelegatorInterface)
String helperName = delegator.getGroupHelperName("default")

def sqlStmt = """
    select * from "AO_D195E9_ISSUE_APPROVAL" where "APPROVAL_STATUS" = 'PENDING' order by "ISSUE_ID";
"""

Connection conn = ConnectionFactory.getConnection(helperName)
Sql sql = new Sql(conn)

try {

    StringBuffer sb = new StringBuffer()
    sql.eachRow(sqlStmt) {
        Issue is = CA.getIssueManager().getIssueObject(it.ISSUE_ID)
        if(is != null && is.getProjectObject().getKey() == "IS") {
            // log.error("${it.CUSTOM_APPROVER}/${it.APPROVAL_STATUS}/${is.getKey()}/${is.getStatus().getName()}/${approval_map[it.APPROVAL_ID]}.")
            if(!issues[it.CUSTOM_APPROVER]){
                issues[it.CUSTOM_APPROVER] = []
            }
            issues[it.CUSTOM_APPROVER].add(is)
        }
    }

} finally {
    sql.close()
}

issues.each{ k, val ->

    ApplicationUser userapp =  CA.getUserManager().getUserByName(k)
    String prefix = "Уважаемый"
    if(userapp.getDisplayName().toString().endsWith("вна")){
        prefix = "Уважаемая"
    }

    String emailBody = "<br>"+prefix+" <b>"+userapp.getDisplayName().toString()+"</b>, вашего согласования требуют следующие задачи:<br><br>"
    emailBody += "<style>table.appissue {width:100%; border:1px solid black; border-collapse: collapse;} table.appissue th, table.appissue td {border:1px solid black; padding-left:10px;}</style><table class=\"appissue\" border=0>"
    emailBody += "<th>Код задачи</th><th>Статус задачи</th><th>Тема задачи</th>"
    val.toSet().each {
        emailBody += "<tr><td><a href=\"${baseUrl}/browse/${it.getKey()}\">${it.getKey()}</a></td><td>${it.getStatus().name}</td><td>${it.summary}</td></tr>"
    }
    emailBody += "</table>"

    Map<String,Object> context =[:]
    String subjectTemplatePath = "templates/email/subject/issuenotify.vm"
    String bodyTemplatePath = "templates/email/html/emailfromadmin.vm"
    String comment = "<style>table.appissue {width:100%; border:1px solid black; border-collapse: collapse;} table.appissue th, table.appissue td {border:1px solid black; padding-left:10px;}</style>"
    context.put("content", emailBody)
    context.put("author", asUser)
    context.put("subject", "Cписок задач требующих вашего согласования")
    String from = "Jiradev.Info-T00@russianpost.ru"

    MailQueueItem item = new MailServiceQueueItemBuilder(asUser, new NotificationRecipient(userapp), subjectTemplatePath, bodyTemplatePath, context).buildQueueItemUsingProjectEmailAsReplyTo()
    CA.getMailQueue().addItem(item)
}