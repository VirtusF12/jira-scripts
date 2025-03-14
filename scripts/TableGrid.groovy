import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.plugin.PluginAccessor
import com.atlassian.jira.user.ApplicationUser

IssueManager issueManager = ComponentAccessor.getOSGiComponentInstanceOfType(IssueManager.class)
Issue issue = issueManager.getIssueObject("RI-2987")
Long issueId = issue.getId()

CustomFieldManager customFieldManager = ComponentAccessor.getOSGiComponentInstanceOfType(CustomFieldManager.class)
CustomField tgngCustomField = customFieldManager.getCustomFieldObjectsByName("Этапы").get(0)
Long tgngCustomFieldId = tgngCustomField.getIdAsLong()
ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser()

PluginAccessor pluginAccessor = ComponentAccessor.getPluginAccessor()
Class apiServiceClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridService")
def gridService = ComponentAccessor.getOSGiComponentInstanceOfType(apiServiceClass)

Class gridRowClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridRow")

def fieldData = gridService.readFieldData(issueId, tgngCustomFieldId, user, null)
def gridRows = fieldData.getRows()

def updates = new ArrayList()

for (row in gridRows) {

    def columns = row.getColumns()

    def jname = columns.get("jname")
    def jdate1 = columns.get("jdate1")

    if (columns.get("jname") == "Оценка БФТ") {

        def update = gridRowClass.newInstance()
        update.setRowId(row.getRowId())
        // We need to specify only the columns which are being updated
        update.setColumns(["jdate1": "12/01/2qwwq5"])
        updates.add(update)
    }

    // if (assignee != null && assignee.get("key") == user.getKey() && columns.get("jstatus") == "In Review") {
    // def update = gridRowClass.newInstance()
    // update.setRowId(row.getRowId())
    // // We need to specify only the columns which are being updated
    // update.setColumns(["jstatus": "Closed"])
    // updates.add(update)
    // }
}
gridService.updateRows(issueId, tgngCustomFieldId, user, updates)





import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.plugin.PluginAccessor
import com.atlassian.jira.user.ApplicationUser

IssueManager issueManager = CA.getOSGiComponentInstanceOfType(IssueManager.class)
Issue issue = issueManager.getIssueObject("RI-3177")
Long issueId = issue.getId()

CustomFieldManager customFieldManager = CA.getOSGiComponentInstanceOfType(CustomFieldManager.class)
CustomField tgngCustomField = customFieldManager.getCustomFieldObjectsByName("Охват").get(0)
Long tgngCustomFieldId = tgngCustomField.getIdAsLong()
ApplicationUser user = CA.getJiraAuthenticationContext().getLoggedInUser()

PluginAccessor pluginAccessor = CA.getPluginAccessor()
Class apiServiceClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridService")
def gridService = CA.getOSGiComponentInstanceOfType(apiServiceClass)

Class gridRowClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridRow")

def fieldData = gridService.readFieldData(issueId, tgngCustomFieldId, user, null)
def gridRows = fieldData.getRows()

def updates = new ArrayList()

for (row in gridRows) {

    def columns = row.getColumns()

    def jname = columns.get("jname")
    log.error("jname = ${jname}")

    def jcount = columns.get("jcount")
    log.error("jcount = ${jcount}")


    // if (columns.get("jname") == "Оценка БФТ") {

    //     def update = gridRowClass.newInstance()
    //     update.setRowId(row.getRowId())
    //     // We need to specify only the columns which are being updated
    //     update.setColumns(["jdate1": "12/01/2qwwq5"])
    //     updates.add(update)
    // }

    // if (assignee != null && assignee.get("key") == user.getKey() && columns.get("jstatus") == "In Review") {
    // def update = gridRowClass.newInstance()
    // update.setRowId(row.getRowId())
    // // We need to specify only the columns which are being updated
    // update.setColumns(["jstatus": "Closed"])
    // updates.add(update)
    // }
}
// gridService.updateRows(issueId, tgngCustomFieldId, user, updates)











import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.plugin.PluginAccessor
import com.atlassian.jira.user.ApplicationUser


// IssueManager issueManager = CA.getOSGiComponentInstanceOfType(IssueManager.class)
// Issue issue = issueManager.getIssueObject("RI-3177")
Long issueId = issue.getId()

CustomFieldManager customFieldManager = CA.getOSGiComponentInstanceOfType(CustomFieldManager.class)
CustomField tgngCustomField = customFieldManager.getCustomFieldObjectsByName("Охват").get(0)
Long tgngCustomFieldId = tgngCustomField.getIdAsLong()
ApplicationUser user = CA.getJiraAuthenticationContext().getLoggedInUser()

PluginAccessor pluginAccessor = CA.getPluginAccessor()
Class apiServiceClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridService")
def gridService = CA.getOSGiComponentInstanceOfType(apiServiceClass)

Class gridRowClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridRow")

def fieldData = gridService.readFieldData(issueId, tgngCustomFieldId, user, null)
def gridRows = fieldData.getRows()


def count = 0
def resPoint = 0

for (row in gridRows) {

    def columns = row.getColumns()

    def jname = columns.get("jname")
    log.error("jname = ${jname}")

    def jcount = columns.get("jcount")
    log.error("jcount = ${jcount}")

    if (jcount != null) {
        count += jcount as Double
    }

}


/*
        0                       0
    ⩽ 5 тыс.                    1,5
    > 5 тыс. ⩽ 10 тыс.          3
    > 10 тыс. ⩽ 50 тыс.         4,5
    > 50 тыс. ⩽ 100 тыс.        6
    > 100 тыс. ⩽ 500 тыс.       7,5
    > 500 тыс. ⩽ 1 млн          9
    >1 млн ⩽ 5 млн              10,5
    > 5 млн ⩽ 10 млн            12
    > 10 млн ⩽ 25 млн           14
    > 25 млн ⩽ 50 млн           16
    > 50 млн ⩽ 75 млн           18
    > 75 млн ⩽ 100 млн          20
    > 100 млн ⩽ 200 млн         22
    > 200 млн ⩽ 300 млн         24
    > 300 млн ⩽ 400 млн         26
    > 400 млн ⩽ 500 млн         28
    > 500 млн                   30
*/

if (count < 5000) {
    resPoint =  1.5
}

if (count > 5000 && count <= 10000) {
    resPoint = 3
}

if (count > 10000 && count <= 50000) {
    resPoint = 4.5
}

if (count > 50000 && count <= 100000) {
    resPoint = 6
}

if (count > 100000 && count <= 500000) {
    resPoint = 7.5
}

if (count > 500000 && count <= 1000000) {
    resPoint = 9
}

if (count > 1000000 && count <= 5000000) {
    resPoint = 10.5
}

if (count > 5000000 && count <= 10000000) {
    resPoint = 12
}

if (count > 10000000 && count <= 25000000) {
    resPoint = 14
}

if (count > 25000000 && count <= 50000000) {
    resPoint = 16
}

if (count > 50000000 && count <= 75000000) {
    resPoint = 18
}

if (count > 75000000 && count <= 100000000) {
    resPoint = 20
}

if (count > 100000000 && count <= 200000000) {
    resPoint = 22
}

if (count > 200000000 && count <= 300000000) {
    resPoint = 24
}

if (count > 300000000 && count <= 400000000) {
    resPoint = 26
}

if (count > 400000000 && count <= 500000000) {
    resPoint = 28
}

if (count > 500000000) {
    resPoint = 30
}

log.error("count = ${count}")

// Скор.балл = PI * 1 * Вес ДИД + Балл Рег.риск * 1 * 0,4 + (Балл R *0,5 + Балл CE * 0,5 ) * 0,3 + Балл S * 1 * 0,3

if (issue.getInwardLinks().size() > 0) {

    def scoringPoint = 0

    issue.getInwardLinks().each { inwardLink ->

        if ( (inwardLink.getIssueLinkType().getInward() in ["дочерние"])
                &&
                (inwardLink.getSourceObject().getIssueType().name.equals("Инвестиционный проект"))
        ) {

            def linkIssue = inwardLink.getSourceObject()
            log.error("linkIssue = ${linkIssue.key}")

            // 123123
            def PI = linkIssue.getCustomFieldValue(33607L) != null ?
                    linkIssue.getCustomFieldValue(33607L) : 0

            log.error("PI = ${PI}")

            def weigthDID = linkIssue.getCustomFieldValue(33611L) != null ?
                    (linkIssue.getCustomFieldValue(33611L).toString().equals("Да") ? 1.2 : 0.2) : 0
            log.error("weigthDID = ${weigthDID}")

            def pointReg = 0
            switch (issue.getCustomFieldValue(33600L).toString()) {
                case "Влияние отсутствует":
                    pointReg = 0
                    break
                case "Нереализация цифрового проекта влечет наложение совокупных штрафов в размере не более 25% от общей стоимости проекта":
                    pointReg = 10
                    break
                case "Нереализация цифрового проекта влечет наложение совокупных штрафов в размере 25% и более от общей стоимости проекта":
                    pointReg = 20
                    break
                case "Нереализация цифрового проекта влечет приостановление деятельности Общества/ невозможность оказания услуг(и) клиентам и/или контрагентам":
                    pointReg = 30
                    break
            }
            log.error("pointReg = ${pointReg}")

            // расчитать
            def pointR = 0 //linkIssue.getCustomFieldValue()
            log.error("pointR = ${pointR}")


            def pointCE = 0
            def valueOptionClient = issue.getCustomFieldValue(33602L) as String
            log.error("valueOptionClient = ${valueOptionClient}")
            def valueOptionStaff = issue.getCustomFieldValue(33603L) as String
            log.error("valueOptionStaff = ${valueOptionStaff}")
            def optionsClient = ["Положительно повлияет на путь клиента (улучшит опыт / упростит путь (повлияет на CJM))", "Влияние не определено", "Отрицательно повлияет на путь клиента (ухудшит опыт / путь усложнится (повлияет на CJM))"]
            def optionsStaff = ["Положительно повлияет на путь сотрудника (улучшит опыт / упростит путь (повлияет на CJM))", "Влияние не определено", "Отрицательно повлияет на путь сотрудника (ухудшит опыт / путь усложнится (повлияет на CJM))"]
            if ( (valueOptionClient != null) && (valueOptionStaff != null) ) {

                switch (valueOptionClient+":"+valueOptionStaff) {

                    case optionsClient[0]+":"+optionsStaff[0]:
                        pointCE = 30
                        break
                    case optionsClient[0]+":"+optionsStaff[1]:
                        pointCE = 15
                        break
                    case optionsClient[0]+":"+optionsStaff[2]:
                        pointCE = 15
                        break

                    case optionsClient[1]+":"+optionsStaff[0]:
                        pointCE = 15
                        break
                    case optionsClient[1]+":"+optionsStaff[1]:
                        pointCE = 0
                        break
                    case optionsClient[1]+":"+optionsStaff[2]:
                        pointCE = -15
                        break

                    case optionsClient[2]+":"+optionsStaff[0]:
                        pointCE = 0
                        break
                    case optionsClient[2]+":"+optionsStaff[1]:
                        pointCE = -15
                        break
                    case optionsClient[2]+":"+optionsStaff[2]:
                        pointCE = -15
                        break
                }

            }
            log.error("pointCE = ${pointCE}")

            def pointS = issue.getCustomFieldValue(33604L) != null ?
                    (issue.getCustomFieldValue(33604L).toString().equals("Да") ? 30 : 0) : 0
            log.error("pointS = ${pointS}")

            log.error("PI * 1 * weigthDID = ${PI * 1 * weigthDID}")
            log.error("pointReg * 1 * 0.4 = ${pointReg * 1 * 0.4}")
            log.error("(pointR * 0.5 + pointCE * 0.5 ) * 0.3 = ${(pointR * 0.5 + pointCE * 0.5 ) * 0.3}")
            log.error("pointS * 1 * 0.3 = ${pointS * 1 * 0.3}")

            scoringPoint = PI * 1 * weigthDID + pointReg * 1 * 0.4 + (pointR * 0.5 + pointCE * 0.5 ) * 0.3 + pointS * 1 * 0.3
        }
    }

    return scoringPoint

} else {
    return 0
}