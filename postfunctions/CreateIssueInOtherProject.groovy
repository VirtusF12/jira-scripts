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

    /*
        HAPI создание задачи от имени atl-service-r00
    */
    def userService = Users.getByName("atl-service-r00")
    try {
        Users.runAs(userService) {
            Issues.create("AZ","Task") {
                setSummary("test")
                setAssignee(user)
                setDescription("")
            }
        }
    } catch (Exception ex) {
        log.error(ex)
    }

} else {
    log.error("""
        issue.key = ${issue.key}
        Задача не была создана в проекте AZ 
        Поле “Руководитель проекта ЕАС ОПС“ = ${user}
    """)
}
