import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.issue.history.ChangeItemBean
import org.apache.commons.httpclient.methods.*
import com.atlassian.jira.bc.project.component.*
import groovyx.net.http.Method.*
import com.atlassian.jira.mail.Email
import com.atlassian.jira.mail.settings.MailSettings
import com.atlassian.mail.MailException
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.plugin.util.ContextClassLoaderSwitchingUtil
import java.time.*
import com.atlassian.jira.issue.Issue
import java.sql.Timestamp

String sendEmail(String emailAddr, String subject, String body) {

    def mailSettings = CA.getComponent(MailSettings)
    if (mailSettings?.send()?.disabled) {
        return 'Your outgoing mail server has been disabled'
    }
    def mailServer = CA.mailServerManager.defaultSMTPMailServer
    if (!mailServer) {
        log.error('Your mail server Object is Null, make sure to set the SMTP Mail Server Settings Correctly on your Server')
        return 'Failed to Send Mail. No SMTP Mail Server Defined'
    }
    def email = new Email(emailAddr)
    email.setMimeType('text/html')
    email.setSubject(subject)
    email.setBody(body)
    try {
        ContextClassLoaderSwitchingUtil.runInContext(SMTPMailServer.classLoader) {
            mailServer.send(email)
        }
        log.error('Mail sent')
        'Success'
    } catch (MailException e) {
        log.error("Send mail failed with error: ${e.message}")
        'Failed to Send Mail, Check Logs for error'
    }
}

def calculateWorkingTime(Timestamp startTime, Timestamp endTime) {

    def WORKDAY_START_HOUR = 9
    def WORKDAY_END_HOUR = 18
    def totalWorkingTime = 0L
    def calendar = Calendar.getInstance()
    calendar.timeInMillis = startTime.time

    while (calendar.timeInMillis < endTime.time) {
        def dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
            def startOfWorkDay = Calendar.getInstance()
            startOfWorkDay.timeInMillis = calendar.timeInMillis
            startOfWorkDay.set(Calendar.HOUR_OF_DAY, WORKDAY_START_HOUR)
            startOfWorkDay.set(Calendar.MINUTE, 0)
            startOfWorkDay.set(Calendar.SECOND, 0)

            def endOfWorkDay = Calendar.getInstance()
            endOfWorkDay.timeInMillis = calendar.timeInMillis
            endOfWorkDay.set(Calendar.HOUR_OF_DAY, WORKDAY_END_HOUR)
            endOfWorkDay.set(Calendar.MINUTE, 0)
            endOfWorkDay.set(Calendar.SECOND, 0)

            def dayStart = Math.max(calendar.timeInMillis, startOfWorkDay.timeInMillis)
            def dayEnd = Math.min(endTime.time, endOfWorkDay.timeInMillis)

            if (dayEnd > dayStart) {
                totalWorkingTime += (dayEnd - dayStart)
            }
        }

        calendar.add(Calendar.DAY_OF_MONTH, 1) // Переход к следующему дню
        calendar.set(Calendar.HOUR_OF_DAY, WORKDAY_START_HOUR)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
    }

    return totalWorkingTime
}

Double getWorkHoursInStatus(Issue issue, String nameStatus) {

    def changeHistoryManager = CA.getChangeHistoryManager()
    def changeItems = changeHistoryManager.getChangeItemsForField(issue, "status")

    def statusDuration = [:]
    def previousTime = issue.getCreated()
    def previousStatus = issue.getStatus().name

    changeItems.each { ChangeItemBean item ->

        def currentTime = item.getCreated()
        def currentStatus = item.getToString()
        def duration = calculateWorkingTime(previousTime, currentTime)
        statusDuration[previousStatus] = (statusDuration[previousStatus] ?: 0) + duration
        previousTime = currentTime
        previousStatus = currentStatus
    }

    def currentTime = new Timestamp(System.currentTimeMillis())
    def duration = calculateWorkingTime(previousTime, currentTime)
    statusDuration[previousStatus] = (statusDuration[previousStatus] ?: 0) + duration

    Double result = 0.0
    statusDuration.each { key, value ->

        def hours = (value as Double / (1000 * 60 * 60)).round(2) as Double
        // log.error("Статус: ${key}, Время нахождения: ${hours.round(2)} часов")
        if (key.toString() == nameStatus) {
            result = hours
        }
    }

    return result
}

Issues.search("project = NEWB2B AND issuetype = \"Рассчитать ФЭМ\"").collect().each { i ->

    Issue issue = i as Issue
    log.error("""
    issue.key = ${issue.key}
    issue.status = ${issue.status.name}
    """)

    if (getWorkHoursInStatus(issue,"Запрос параметров") > 40.0) {
        def email = "Glebova.G@russianpost.ru"
        def summary = "На ${issue.key} нет ответа от вашей группы сотрудников."
        def body = "<p>На <a href=\"https://jira.russianpost.ru/browse/${issue.key}\">${issue.key}</a> нет ответа от вашей группы сотрудников. Просим вас принять ответные меры и предоставить ответ по запросу, чтобы избежать задержек.</p>"
        sendEmail(email, summary, body)
    }

    if (getWorkHoursInStatus(issue,"Уточнение у автора") > 14.0) {
        issue.transition('Закрыть (2)')
        final boolean dispatchEvent = true
        final String commentBody = "Задача закрыта автоматически из-за отсутствия ответа в установленные сроки"
        def author = CA.jiraAuthenticationContext.loggedInUser
        CA.commentManager.create(issue, author, commentBody, dispatchEvent)
    }

    if (getWorkHoursInStatus(issue,"ФЭМ готова") > 5.0) {
        issue.transition('Закрыть (2)')
    }
}