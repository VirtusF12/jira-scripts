import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager

def cfm = CA.getCustomFieldManager()
def fieldTypeOfActivity = getFieldById("customfield_32902") // "Вид деятельности"
fieldTypeOfActivity.setRequired(true)
def fieldTypeOfService = getFieldById("customfield_32903") // "Вид услуги"
fieldTypeOfService.setRequired(true)
def fieldTypeOfWork = getFieldById("customfield_32904") // "Вид работы"
def epicLink = getFieldByName("Epic Link")
def issueTypeNameContext = getIssueContext()?.getIssueType()?.getName()
def issueTypeNameUnderlyingIssue = underlyingIssue?.getIssueType()?.name

String getKeyAsset(Issue issue, CustomField cf) {

    def valueAsset = issue?.getCustomFieldValue(cf).toString() // [Формирование ТКП (SC-598461)]
    def size = valueAsset.replace('[','').replace(']','').split(" ").size() // 3
    def keyAsset = valueAsset.replace('[','').replace(']','').split(" ")[size-1].replace('(','').replace(')','')

    return keyAsset // SC-598433
}

if (!issueTypeNameContext.equals("Epic")) {

    if (epicLink.value) {

        def keyEpic = epicLink.value.toString().split(":")[1]
        def cfTypeOfActivity = cfm.getCustomFieldObject(32902L)
        def issueEpic = Issues.getByKey(keyEpic)
        fieldTypeOfActivity.setFormValue(getKeyAsset(issueEpic, cfTypeOfActivity))
        fieldTypeOfActivity.setReadOnly(true)
    }
}

if (issueTypeNameContext in ["Sub-task"]) {

    def parent = getFieldById("parentIssueId")
    Long parentIssueId = parent.getFormValue() as Long
    IssueManager issueManager = CA.getIssueManager()
    Issue parentIssue = issueManager.getIssueObject(parentIssueId)

    def cfTypeOfActivity = cfm.getCustomFieldObject(32902L)
    fieldTypeOfActivity.setFormValue(getKeyAsset(parentIssue, cfTypeOfActivity))

    def cfTypeOfService = cfm.getCustomFieldObject(32903L)
    fieldTypeOfService.setFormValue(getKeyAsset(parentIssue, cfTypeOfService))

    def cfTypeOfWork = cfm.getCustomFieldObject(32904L)
    def valueAssetTypeOfWork = parentIssue?.getCustomFieldValue(cfTypeOfWork)
    log.error("valueAssetTypeOfWork = ${valueAssetTypeOfWork}")

    if (valueAssetTypeOfWork == null) {
        fieldTypeOfWork.setRequired(true)
    }

    if (valueAssetTypeOfWork != null) {
        fieldTypeOfWork.setFormValue(getKeyAsset(parentIssue, cfTypeOfWork))
        fieldTypeOfWork.setReadOnly(true)
    }
}

if (issueTypeNameUnderlyingIssue in ["Sub-task"]) {

    def parent = getFieldById("parentIssueId")
    Long parentIssueId = parent.getFormValue() as Long
    IssueManager issueManager = CA.getIssueManager()
    Issue parentIssue = issueManager.getIssueObject(parentIssueId)

    def cfTypeOfWork = cfm.getCustomFieldObject(32904L)
    def valueAssetTypeOfWork = parentIssue?.getCustomFieldValue(cfTypeOfWork)

    if (valueAssetTypeOfWork == null) {
        fieldTypeOfWork.setRequired(true)
    }

    if (valueAssetTypeOfWork != null) {

        fieldTypeOfWork.setFormValue(getKeyAsset(parentIssue, cfTypeOfWork))
        fieldTypeOfWork.setReadOnly(true)
    }
}