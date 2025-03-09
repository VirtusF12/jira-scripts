import com.atlassian.jira.issue.Issue

//  PI * 1 * Вес ДИД

if (issue.getInwardLinks().size() > 0) {

    def result = 0
    def listIssueDID = Issues.search("issue in linkedIssues(\"${issue.key}\",\"дочерние\") and project = DID and issuetype = \"Инвестиционный проект\"").collect()

    if (
            (listIssueDID.size() >= 3)
    ) {
        return "Ошибка связей с ДИД"
    }

    if (
            (listIssueDID.size() > 0)
                    &&
                    (listIssueDID.findAll() { !((it as Issue).getStatus().name in ["Отменено"]) }.size() == 2)
    ) {
        return "Ошибка связей с ДИД"
    }

    if (
            (listIssueDID.size() > 0)
                    &&
                    (listIssueDID.findAll() { !((it as Issue).getStatus().name in ["Отменено"]) }.size() == 1)
    ) {

        def linkIssue = listIssueDID.findAll() { !((it as Issue).getStatus().name.equals("Отменено")) }.collect().first() as Issue
        log.error("linkIssue = ${linkIssue.key}")

        def PI = linkIssue.getCustomFieldValue(33607L) != null ?
                linkIssue.getCustomFieldValue(33607L) : 0
        log.error("PI = ${PI}")

        def weigthDID = linkIssue.getCustomFieldValue(33611L) != null ?
                (linkIssue.getCustomFieldValue(33611L).toString().equals("Да") ? 1.2 : 0.2) : 0
        log.error("weigthDID = ${weigthDID}")

        result = PI * 1 * weigthDID
        return "${result}"

    } else {
        return "Нет данных"
    }

} else {

    return "Нет данных"
}
