import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.link.RemoteIssueLinkBuilder
import com.atlassian.jira.issue.link.RemoteIssueLink
import com.atlassian.jira.issue.link.RemoteIssueLinkManager

def title = "Основные правила работы с инициативами"
def url = "https://confluence.tools.russianpost.ru/pages/viewpage.action?pageId=582782856"

if ( issue.getIssueType().getName() in ["Initiative", "Инициатива"] ) {
    RemoteIssueLink link = new RemoteIssueLinkBuilder().issueId(issue.getId()).url(url).title(title).build();
    CA.getComponent(RemoteIssueLinkManager.class).createRemoteIssueLink(link, CA.getJiraAuthenticationContext().getLoggedInUser());
}
