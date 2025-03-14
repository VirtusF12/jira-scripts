import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.issue.history.ChangeItemBean
import com.atlassian.jira.issue.Issue
import java.sql.Timestamp

def calculateWorkingTime(Timestamp startTime, Timestamp endTime) { // Функция для расчета рабочего времени между двумя временными метками

    def WORKDAY_START_HOUR = 9
    def WORKDAY_END_HOUR = 18
    def totalWorkingTime = 0L
    def calendar = Calendar.getInstance()
    calendar.timeInMillis = startTime.time

    while (calendar.timeInMillis < endTime.time) {
        def dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) { // Исключаем выходные (суббота и воскресенье)
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

    def changeHistoryManager = ComponentAccessor.getChangeHistoryManager()
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
        log.error("Статус: ${key}, Время нахождения: ${hours.round(2)} часов")
        if (key.toString() == nameStatus) {
            result = hours
        }
    }

    return result
}

Issue issue = Issues.getByKey("ATL-4847")
log.error(getWorkHoursInStatus(issue, 'Ожидает выполнения'))