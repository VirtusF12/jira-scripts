import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.issue.Issue

import com.atlassian.jira.component.ComponentAccessor
import com.opensymphony.workflow.InvalidInputException
import com.atlassian.servicedesk.api.requesttype.RequestTypeService
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.atlassian.servicedesk.internal.customfields.origin.VpOrigin

import com.atlassian.jira.component.ComponentAccessor
import com.opensymphony.workflow.InvalidInputException



log.error("status.name = ${issue.getStatus().name}")
def user = Users.getLoggedInUser()
def issueManager = CA.getIssueManager()
IssueIndexingService issueIndexingService = (IssueIndexingService) CA.getComponent(IssueIndexingService.class)

if (issue.getStatus().name == "Уточнение информации"
        &&
        issue.reporter.username == user.username
) {

    def previousStatus = CA.getChangeHistoryManager().getChangeItemsForField(issue, 'status')?.last().fromString
    log.error("previousStatus = ${previousStatus}")

    if (previousStatus in ["Анализ"]) {
        issue.transition("Информация предоставлена")
        issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
        issueIndexingService.reIndex(issue)
    }

    if (previousStatus in ["Пауза"]) {
        issue.transition("На паузу")
        issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
        issueIndexingService.reIndex(issue)
    }
}


@WithPlugin("com.atlassian.servicedesk")
@PluginModule
RequestTypeService requestTypeService
def customFieldManager = ComponentAccessor.customFieldManager

def requestTypeCustomField = customFieldManager.getCustomFieldObjects(issue).findByName('Customer Request Type')
def requestTypeKey = (issue.getCustomFieldValue(requestTypeCustomField) as VpOrigin)?.requestTypeKey

def groups = ["R00-All-Users-АУП","R00-All-users-AUP-ext","jira-administrators"] as Set<String>
def groupManager = ComponentAccessor.getGroupManager()
log.error("requestTypeKey = ${requestTypeKey}")
log.error("is current user (${user.getUsername()}) in groups (${groupManager.isUserInGroups(user, groups)})")

if (!(requestTypeKey == null)) {
    if ( !groupManager.isUserInGroups(user, groups) ) {
        log.error("exception create")
        throw new InvalidInputException("Вы не состоите в одной из групп: 'R00-All-Users-АУП', 'R00-All-users-AUP-ext'")
    } else {
        log.error("create")
    }
}