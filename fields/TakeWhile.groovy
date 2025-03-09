import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.changehistory.ChangeHistory
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.issue.history.ChangeItemBean

if (!(issue.getProjectObject().getKey().equals("ROI") && issue.getIssueType().name.equals("Epic"))) return

try {

    def changeHistoryManager = CA.getChangeHistoryManager()
    def cfDate = CA.getCustomFieldManager().getCustomFieldObject("customfield_28565")
    def dateFromValue = issue.getCustomFieldValue(cfDate)

    boolean isExistValueHistory =
            changeHistoryManager.getChangeHistories(issue)
                    .reverse()
                    .takeWhile {
                        !it.getChangeItemBeans().any { item ->
                            item.field.equals("status") && item.getFromString().equals("Отменено") && item.getToString().equals("Черновик")
                        }
                    }.any { l ->
                l.getChangeItemBeans().any { el ->
                    el.field.equals("Предоставление оценки со стороны ПТ")
                }
            }

    log.error("isExistValueHistory = ${isExistValueHistory}")
    log.error("status Черновик = ${issue.getStatus().getName().equals("Черновик")}")
    log.error(" (dateFromValue == null) && (isExistValueHistory == true) = ${(dateFromValue == null) && (isExistValueHistory == true)}")
    log.error("(dateFromValue == null) && (isExistValueHistory == false) = ${(dateFromValue == null) && (isExistValueHistory == false)}")

    if (issue.getStatus().getName().equals("Черновик")) {
        return null
    }


    if ( (dateFromValue == null) && (isExistValueHistory == true) ) {
        return null
    }

    if ( (isExistValueHistory == true) ) {
        return dateFromValue
    }

    if ( (dateFromValue == null) && (isExistValueHistory == false) ) {

        def created = changeHistoryManager?.getChangeItemsForField(issue, "status").size() > 0 ?
                changeHistoryManager?.getChangeItemsForField(issue, "status")?.
                        findAll { it?.toString == "Согласование с Заказчиком"  }?.last()?.created : null

        log.error("created = ${created}")

        return (created as java.util.Date)

    } else {

        return dateFromValue
    }

} catch (Exception ex) {
    return
}