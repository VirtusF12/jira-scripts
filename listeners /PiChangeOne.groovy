import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.MutableIssue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.index.IssueIndexingService
import com.atlassian.jira.user.util.UserManager

// Issue Update

IssueManager issueManager = CA.getIssueManager()
CustomFieldManager customFieldManager = CA.getCustomFieldManager()
UserManager userManager = CA.getUserManager()
IssueIndexingService issueIndexingService = CA.getComponent(IssueIndexingService)

def changeItem = event.changeLog.getRelated("ChildChangeItem")
MutableIssue issue = issueManager.getIssueObject(event.getIssue().id) as MutableIssue

if (changeItem['field'].first() == "PI   (Profitability Index, индекс рентабельности инвестиций)") {

    (issue as MutableIssue).update {
        setCustomFieldValue(33611, "Нет")
    }

    issueIndexingService.reIndex(issue)
}
