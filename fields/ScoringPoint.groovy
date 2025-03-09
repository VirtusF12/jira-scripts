import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.CustomFieldManager
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.fields.CustomField
import com.atlassian.plugin.PluginAccessor
import com.atlassian.jira.user.ApplicationUser

/*
    Скор.балл = PI * 1 * Вес ДИД + Балл Рег.риск * 1 * 0,4 + (Балл R *0,5 + Балл CE * 0,5 ) * 0,3 + Балл S * 1 * 0,3
*/

Double calculatePointR(Double pointCE) {

    /*
    CustomFieldManager customFieldManager = CA.getOSGiComponentInstanceOfType(CustomFieldManager.class)
    CustomField tgngCustomField = customFieldManager.getCustomFieldObjectsByName("Охват").get(0)
    Long tgngCustomFieldId = tgngCustomField.getIdAsLong()
    ApplicationUser user = CA.getJiraAuthenticationContext().getLoggedInUser()

    PluginAccessor pluginAccessor = CA.getPluginAccessor()
    Class apiServiceClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridService")
    def gridService = CA.getOSGiComponentInstanceOfType(apiServiceClass)

    Class gridRowClass = pluginAccessor.getClassLoader().findClass("com.idalko.tgng.jira.server.api.GridRow")

    def fieldData = gridService.readFieldData(issue.getId(), tgngCustomFieldId, user, null)
    def gridRows = fieldData.getRows()

    def count = 0

    for (row in gridRows) {

        def columns = row.getColumns()

        def jname = columns?.get("jname") as String
        log.error("jname = (${jname})")

        def jcount = columns?.get("jcount") as String
        log.error("jcount = (${jcount})")

        if (jcount != null && !jcount?.isEmpty()) {
            count += jcount as Double
        }
    }
    */

    def count = issue.getCustomFieldValue(33800L) != null ? issue.getCustomFieldValue(33800L) : 0  // Охват: Кол-во операции
    log.error("count = ${count}")

    /*
        0	                        0       +
        ⩽ 5 тыс.	                1,5     +
        > 5 тыс. ⩽ 10 тыс.	        3       +
        > 10 тыс. ⩽ 50 тыс.	        4,5     +
        > 50 тыс. ⩽ 100 тыс.	    6       +
        > 100 тыс. ⩽ 500 тыс.	    7,5     +
        > 500 тыс. ⩽ 1 млн	        9       +
        > 1 млн ⩽ 5 млн	            10,5    +
        > 5 млн ⩽ 10 млн	        12      +
        > 10 млн ⩽ 25 млн	        14
        > 25 млн ⩽ 50 млн	        16
        > 50 млн ⩽ 75 млн	        18
        > 75 млн ⩽ 100 млн	        20
        > 100 млн ⩽ 200 млн	        22
        > 200 млн ⩽ 300 млн	        24
        > 300 млн ⩽ 400 млн	        26
        > 400 млн ⩽ 500 млн	        28
        > 500 млн	                30
    */

    def valueOptionClient = issue.getCustomFieldValue(33602L) as String
    log.error("valueOptionClient = ${valueOptionClient}")
    def valueOptionStaff = issue.getCustomFieldValue(33603L) as String
    log.error("valueOptionStaff = ${valueOptionStaff}")

    if (
            (pointCE >= -15 && pointCE <= 0)
                    &&
                    (valueOptionClient != null && valueOptionStaff != null)
    ) {
        return 0
    }

    if (count == 0) {
        return 0
    }

    if (count <= 5000) {
        return 1.5
    }

    if (count > 5000 && count <= 10000) {
        return 3
    }

    if (count > 10000 && count <= 50000) {
        return 4.5
    }

    if (count > 50000 && count <= 100000) {
        return 6
    }

    if (count > 100000 && count <= 500000) {
        return 7.5
    }

    if (count > 500000 && count <= 1000000) {
        return 9
    }

    if (count > 1000000 && count <= 5000000) {
        return 10.5
    }

    if (count > 5000000 && count <= 10000000) {
        return 12
    }

    if (count > 10000000 && count <= 25000000) {
        return 14
    }

    if (count > 25000000 && count <= 50000000) {
        return 16
    }

    if (count > 50000000 && count <= 75000000) {
        return 18
    }

    if (count > 75000000 && count <= 100000000) {
        return 20
    }

    if (count > 100000000 && count <= 200000000) {
        return 22
    }

    if (count > 200000000 && count <= 300000000) {
        return 24
    }

    if (count > 300000000 && count <= 400000000) {
        return 26
    }

    if (count > 400000000 && count <= 500000000) {
        return 28
    }

    if (count > 500000000) {
        return 30
    }

    return 0
}

Double calculatePointCE() {

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

    return pointCE
}

Double calculatePointReg() {

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

    return pointReg
}

Double calculatePointS() {

    return issue.getCustomFieldValue(33604L) != null ?
            (issue.getCustomFieldValue(33604L).toString().equals("Да") ? 30 : 0) : 0

}

def scoringPoint = 0
def PI = 0
def weigthDID = 0
def pointReg = 0
def pointR = 0
def pointCE = 0
def pointS = 0

if (issue.getInwardLinks().size() > 0) {

    def listIssueDID = Issues.search("issue in linkedIssues(\"${issue.key}\",\"дочерние\") and project = DID and issuetype = \"Инвестиционный проект\"").collect()

    if (
            (listIssueDID.size() >= 3)
    ) {
        log.error("----- Ошибка (инициатива с дочерними задачами DID не валиден) listIssueDID.size() >= 3 == ${listIssueDID.size() >= 3} -----")

        pointReg = calculatePointReg()
        log.error("---- pointReg = ${pointReg}")

        pointCE = calculatePointCE()
        log.error("---- pointCE = ${pointCE}")

        pointR = calculatePointR(pointCE)
        log.error("---- pointR = ${pointR}")

        pointS = calculatePointS()
        log.error("---- pointS = ${pointS}")

        scoringPoint = PI * 1 * weigthDID + pointReg * 1 * 0.4 + (pointR * 0.5 + pointCE * 0.5 ) * 0.3 + pointS * 1 * 0.3
        log.error("PI(${PI}) * 1 * weigthDID(${weigthDID}) + pointReg(${pointReg}) * 1 * 0.4 + (pointR(${pointR}) * 0.5 + pointCE(${pointCE}) * 0.5 ) * 0.3 + pointS(${pointS}) * 1 * 0.3 = ${scoringPoint}")

        return scoringPoint
    }

    if (
            (listIssueDID.size() > 0)
                    &&
                    (listIssueDID.findAll() { !((it as Issue).getStatus().name in ["Отменено"]) }.size() == 1)
    ) {
        log.error("----- Расчет (инициатива с дочерними задачами DID валиден) -----")

        def linkIssue = listIssueDID.findAll() { !((it as Issue).getStatus().name.equals("Отменено")) }.collect().first() as Issue // listIssueDID.first() as Issue
        log.error("linkIssue = ${linkIssue.key}")

        PI = linkIssue.getCustomFieldValue(33607L) != null ?
                linkIssue.getCustomFieldValue(33607L) : 0
        log.error("---- PI = ${PI}")

        weigthDID = linkIssue.getCustomFieldValue(33611L) != null ?
                (linkIssue.getCustomFieldValue(33611L).toString().equals("Да") ? 1.2 : 0.2) : 0
        log.error("---- weigthDID = ${weigthDID}")

        pointReg = calculatePointReg()
        log.error("---- pointReg = ${pointReg}")

        pointCE = calculatePointCE()
        log.error("---- pointCE = ${pointCE}")

        pointR = calculatePointR(pointCE)
        log.error("---- pointR = ${pointR}")

        pointS = calculatePointS()
        log.error("---- pointS = ${pointS}")

        scoringPoint = PI * 1 * weigthDID + pointReg * 1 * 0.4 + (pointR * 0.5 + pointCE * 0.5 ) * 0.3 + pointS * 1 * 0.3
        log.error("PI(${PI}) * 1 * weigthDID(${weigthDID}) + pointReg(${pointReg}) * 1 * 0.4 + (pointR(${pointR}) * 0.5 + pointCE(${pointCE}) * 0.5 ) * 0.3 + pointS(${pointS}) * 1 * 0.3 = ${scoringPoint}")

        return scoringPoint

    } else {

        log.error("----- Ошибка (инициатива с дочерними задачами DID не валиден) -----")

        pointReg = calculatePointReg()
        log.error("---- pointReg = ${pointReg}")

        pointCE = calculatePointCE()
        log.error("---- pointCE = ${pointCE}")

        pointR = calculatePointR(pointCE)
        log.error("---- pointR = ${pointR}")

        pointS = calculatePointS()
        log.error("---- pointS = ${pointS}")

        scoringPoint = PI * 1 * weigthDID + pointReg * 1 * 0.4 + (pointR * 0.5 + pointCE * 0.5 ) * 0.3 + pointS * 1 * 0.3
        log.error("PI(${PI}) * 1 * weigthDID(${weigthDID}) + pointReg(${pointReg}) * 1 * 0.4 + (pointR(${pointR}) * 0.5 + pointCE(${pointCE}) * 0.5 ) * 0.3 + pointS(${pointS}) * 1 * 0.3 = ${scoringPoint}")

        return scoringPoint
    }

} else {

    log.error("--------- Инициатива без дочерних задач (DID) ---------")
    pointReg = calculatePointReg()
    log.error("---- pointReg = ${pointReg}")

    pointCE = calculatePointCE()
    log.error("---- pointCE = ${pointCE}")

    pointR = calculatePointR(pointCE)
    log.error("---- pointR = ${pointR}")

    pointS = calculatePointS()
    log.error("---- pointS = ${pointS}")

    scoringPoint =  PI * 1 * weigthDID + pointReg * 1 * 0.4 + (pointR * 0.5 + pointCE * 0.5 ) * 0.3 + pointS * 1 * 0.3
    log.error("PI(${PI}) * 1 * weigthDID(${weigthDID}) + pointReg(${pointReg}) * 1 * 0.4 + (pointR(${pointR}) * 0.5 + pointCE(${pointCE}) * 0.5 ) * 0.3 + pointS(${pointS}) * 1 * 0.3 = ${scoringPoint}")

    return scoringPoint
}