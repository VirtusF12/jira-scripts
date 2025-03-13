import com.atlassian.jira.user.ApplicationUser

if (!(issue.getProjectObject().key in ["EORM"])) return

def user = issue.getCustomFieldValue(26502L) as ApplicationUser
String summary = "АЗ к [${issue.key}] [${issue.summary}]"

if (user != null) {
    Issues.create("AZ","Task") {
        setSummary(summary)
        setAssignee(user)
        setDescription("")
    }
} else {
    log.error("""
        issue.key = ${issue.key}
        Задача не была создана в проекте AZ 
        Поле “Руководитель проекта ЕАС ОПС“ = ${user}
    """)
}
