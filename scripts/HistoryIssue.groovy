import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.Issue

def issue = Issues.getByKey("DID-637")
// (Рассмотрение ИП)

def logHistoryStatus = CA.getChangeHistoryManager().getChangeItemsForField(issue, "status")
logHistoryStatus.each {
    log.error("date: (${it.created}) fromString: (${it.fromString}) -> toString: (${it.toString})")
}

// 05/03/2025 16:44