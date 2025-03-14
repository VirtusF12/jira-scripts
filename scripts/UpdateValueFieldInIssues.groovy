import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.user.ApplicationUser
import com.atlassian.jira.event.type.EventDispatchOption

def data = [
        "EASARCHI-47800:RP37402029",
        "EASARCHI-47806:RP37478140",
        "EASARCHI-47764:RP37284641",
        "EASARCHI-47802:RP37487764",
]


def cfm = CA.getCustomFieldManager()
def issueManager = CA.getIssueManager()
def cf = cfm.getCustomFieldObject("customfield_16800")
def user = CA.getUserManager().getUserByName("atl-service-r00") as ApplicationUser

data.each { item ->

    def _data = item.split(":")

    try {
        def issue = issueManager.getIssueByCurrentKey(_data[0])
        // def result = issue.getCustomFieldValue(cf) == null ? value : issue.getCustomFieldValue(cf).toString() + ", " + value
        issue.setCustomFieldValue(cf, _data[1])
        issueManager.updateIssue(user, issue, EventDispatchOption.DO_NOT_DISPATCH, false)
    } catch (Exception e) {
        log.error(_data[0])
    }
}