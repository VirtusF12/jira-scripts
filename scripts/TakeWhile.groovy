// https://community.atlassian.com/t5/Jira-Service-Management/Scriptrunner-Set-insight-custom-field-value-based-on-issue/qaq-p/2065241

import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.changehistory.ChangeHistory
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager
import com.atlassian.jira.issue.history.ChangeItemBean

if (!(issue.getProjectObject().getKey().equals("ROI") && issue.getIssueType().name.equals("Epic"))) return

try {

    // ROI-1612, ROI-2428
    def cfDate = CA.getCustomFieldManager().getCustomFieldObject("customfield_28565")
    def dateFromValue = issue.getCustomFieldValue(cfDate)

    /// flag
    def changeHistoryManager = CA.getChangeHistoryManager()
    // def oldCustomFieldHisotry = changeHistoryManager?.getChangeItemsForField(issue, "Предоставление оценки со стороны ПТ").sort { it.getCreated() }
    // def dateFromValueHistory = oldCustomFieldHisotry.size() > 0 ? oldCustomFieldHisotry.last().to : null

    boolean isExistValueHistory =
            changeHistoryManager.getChangeHistories(issue)
                    .reverse()
                    .takeWhile {
                        !it.getChangeItemBeans().any { item ->
                            item.field.equals("status") && item.getFromString().equals("Отменено") && item.getToString().equals("Черновик")
                        }
                    }.any { l ->
                l.getChangeItemBeans().any { el ->
                    el.field.equals("Направление на оценку")
                }
            }
    ///

    if (issue.getStatus().getName().equals("Черновик")) {
        return null
    }

    /* B: ATL-4305 */
    // def property = issue.entityProperties
    // def flag = property.getBoolean("flag") as boolean

    if ( (dateFromValue == null) && (isExistValueHistory == true) ) { // (dateFromValueHistory != null)
        return null
    }

    if ( (isExistValueHistory == true) ) { // (dateFromValueHistory != null)
        return dateFromValue
    }
    /* E: ATL-4305 */

    if ( (dateFromValue == null) && (isExistValueHistory == false) ) { // (dateFromValueHistory == null)

        // def changeHistoryManager = CA.getChangeHistoryManager()
        def created = changeHistoryManager?.getChangeItemsForField(issue, "status").size() > 0 ?
                changeHistoryManager?.getChangeItemsForField(issue, "status")?.
                        findAll { it?.toString == "Согласование с Заказчиком"  }?.last()?.created : null

        return (created as java.util.Date)

    } else {

        return dateFromValue
    }

} catch (Exception ex) {
    return
}