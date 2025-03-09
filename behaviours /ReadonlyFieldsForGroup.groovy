import com.atlassian.jira.component.ComponentAccessor as CA

def currentUser = CA.getJiraAuthenticationContext().getLoggedInUser()
def restrictedGroup = "R00-JIRA-PT-MNGRS"

def editableFields = ["customfield_28521", "customfield_28522"]

def groupManager = CA.groupManager

if (groupManager.isUserInGroup(currentUser,restrictedGroup)) {

    getFieldScreen().getTabs().each { tab ->

        log.error("tab.name = ${tab.name}")

        tab.getFieldScreenLayoutItems().each { lsla ->
            log.error("fieldId = ${lsla.getFieldId()}")

            if ( editableFields.contains(lsla.getFieldId()) ) {
                getFieldById(lsla.getFieldId()).setReadOnly(false)
            } else {
                getFieldById(lsla.getFieldId()).setReadOnly(true)
            }
        }
    }
}