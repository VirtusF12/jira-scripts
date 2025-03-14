import com.atlassian.jira.component.ComponentAccessor
import com.onresolve.jira.groovy.user.FieldBehaviours
import static com.atlassian.jira.issue.IssueFieldConstants.*
import groovy.transform.BaseScript
import com.onresolve.jira.groovy.user.FieldBehaviours
import groovy.transform.BaseScript

@BaseScript FieldBehaviours fieldBehaviours

final String fieldName = "Бюджет проекта"

def currentUser = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()
def assignee = getFieldById(ASSIGNEE)
def reporter = getFieldById(REPORTER)

if ( currentUser.getUsername().equals(assignee.getValue()) ||
        currentUser.getUsername().equals(reporter.getValue())
) {
    getFieldByName(fieldName).setReadOnly(false)
} else {
    getFieldByName(fieldName).setReadOnly(true)
}
