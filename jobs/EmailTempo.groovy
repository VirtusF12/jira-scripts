// Уведомления о незаполненном периоде в Tempo для лидеров команд
import com.atlassian.jira.component.ComponentAccessor
import org.apache.commons.httpclient.Credentials
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.*
import groovy.json.JsonSlurper
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date
import com.atlassian.jira.bc.project.component.*
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
import groovyx.net.http.Method.*
import com.atlassian.jira.mail.Email
import com.atlassian.jira.mail.settings.MailSettings
import com.atlassian.mail.MailException
import com.atlassian.mail.server.SMTPMailServer
import com.atlassian.plugin.util.ContextClassLoaderSwitchingUtil
import java.util.Arrays
import java.time.*
import java.time.temporal.TemporalAdjusters
import com.atlassian.jira.bc.user.search.UserSearchService

String sendEmail(String emailAddr, String subject, String body) {

    def mailSettings = ComponentAccessor.getComponent(MailSettings)
    if (mailSettings?.send()?.disabled) {
        return 'Your outgoing mail server has been disabled'
    }
    def mailServer = ComponentAccessor.mailServerManager.defaultSMTPMailServer
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

try {

    def baseUrl = ComponentAccessor.getApplicationProperties().getString("jira.baseurl")
    def JIRA_API_URL = baseUrl + "/rest/tempo-teams/1/team"
    def JIRA_API_URL_2 = baseUrl + "/rest/tempo-teams/2/team/"
    def user_pass = "atl-service-r00:SkgykETS@Fk4"
    //def user_pass = "TempoBot:d.T+3U'o{%p_k"

    log.error("user_pass = ${user_pass}")
    def jira = new HTTPBuilder(JIRA_API_URL)
    jira.client.addRequestInterceptor(new HttpRequestInterceptor() {
        void process(HttpRequest httpRequest, HttpContext httpContext) {
            httpRequest.addHeader('Authorization', 'Basic ' + user_pass.bytes.encodeBase64().toString())
        }
    })
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
    def cal = Calendar.instance
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_WEEK, -1)
    }
    cal.add(Calendar.DAY_OF_WEEK, -1)
    while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        cal.add(Calendar.DAY_OF_WEEK, -1)
    }
    Date lastMonday = cal.time // дата предыдущей недели
    def teams = jira.get(path: JIRA_API_URL)

    def excludePrograms = ["Бэкофис"]
    teams = teams.findAll { team -> !excludePrograms.contains( team?.program ) }
    log.error("teams.size() = " + teams.size())

    for (int idx = 0; idx < teams.size(); idx++) {

        def listUser = []

        try {
            def leadEmail = ComponentAccessor.getUserManager().getUserByKey(teams[idx].lead).getEmailAddress().toString()
            def members = jira.get(path: JIRA_API_URL_2 + teams[idx].id +"/member",
                    query: [onlyActive: true]);
            //if (teams[idx].id == 25) {

            if (members.size() > 0) {

                for ( int j = 0; j < members.size(); j++) {
                    if (members[j].member.activeInJira) {
                        listUser.add(members[j].member.name)
                    }
                }

                if (listUser.size() == 0) continue

                def mapTeam = [:]
                def listTimeSpendSecondsTeam = [:]
                def listDateWeek = []
                def flag = true // def dayMonday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).getDayOfMonth()

                listUser.each {

                    def username = it
                    def listDate = []
                    def listTimeSpend = []

                    if (username != "atl-service-r00") {

                        Calendar copyCalendar = Calendar.instance
                        copyCalendar.setTime(cal.getTime())

                        for (int day = 1; day <= 7; day++) {

                            Date dateCalendar = copyCalendar.getTime();
                            String dateCalendarStr = dateCalendar.format("yyyy-MM-dd").toString()
                            if (flag) listDateWeek.add(dateCalendarStr)
                            String date_string = dateCalendarStr.split('-')[0]  + "-" + dateCalendarStr.split('-')[1] + "-" + dateCalendarStr.split('-')[2]
                            Date dateLocal = sdf.parse(date_string)

                            HttpClient client = new HttpClient();
                            username = username.contains(' ') ? username.replaceAll(" ", "%20") : username
                            def url = baseUrl + "/rest/tempo-timesheets/3/worklogs?username=${username}&dateFrom=${date_string}&dateTo=${date_string}"

                            GetMethod method = new GetMethod(url);
                            Credentials credentials = new UsernamePasswordCredentials("atl-service-r00","SkgykETS@Fk4");
                            //Credentials credentials = new UsernamePasswordCredentials("TempoBot","d.T+3U'o{%p_k");
                            client.getParams().setAuthenticationPreemptive(true);
                            client.getState().setCredentials(AuthScope.ANY, credentials);
                            client.executeMethod(method);
                            def response = method.getResponseBodyAsString()
                            def jsonSlurper = new JsonSlurper()
                            def objectJson = jsonSlurper.parseText(response)

                            if (objectJson.size() == 0) {
                                listDate.add(date_string)
                            } else {
                                def sumTime = 0
                                objectJson.each {
                                    sumTime += Integer.parseInt(it.timeSpentSeconds.toString())
                                }
                                listTimeSpend.add(dateCalendarStr.split('-')[2]+":"+String.format("%.1f",(sumTime / 60 / 60)) + " ч")
                            }
                            copyCalendar.add(Calendar.DATE, 1)
                            method.releaseConnection();
                            mapTeam.put(username, listDate)
                            listTimeSpendSecondsTeam.put(username, listTimeSpend)
                        }
                    }
                    flag = false
                }

                def fistDateStr = listDateWeek[0]
                def lastDateStr = listDateWeek[listDateWeek.size()-1]
                def nameTeam = jira.get(path: JIRA_API_URL + "/" + teams[idx].id).name
                def period = fistDateStr.split('-')[2]+"."+fistDateStr.split('-')[1]+"."+fistDateStr.split('-')[0] + "-" +
                        lastDateStr.split('-')[2]+"."+lastDateStr.split('-')[1]+"."+lastDateStr.split('-')[0]
                def summaryEmail = "Отчет о списании времени команды ${nameTeam} за период " + period
                def styleTh = 'style="font-weight: bold;padding: 5px;background: #efefef;border: 1px solid #dddddd;"'
                def styleTd = 'style="border: 1px solid #dddddd;padding: 5px;"'
                def styleRedTd = 'style="font-weight: bold;padding: 5px;background: #FF0000;border: 1px solid #dddddd;"'
                def styleYellowTd = 'style="font-weight: bold;padding: 5px;background: #FFEA00;border: 1px solid #dddddd;"'
                def headerTh = ""
                def listDay = []
                listDateWeek.each {
                    def day = it.split("-")[2]
                    headerTh += "<th ${styleTh}>${day}</th>"
                    listDay.add(day)
                }
                def headerTable = """<table style="width:100%;margin-bottom:20px;border:1px solid #dddddd;border-collapse: collapse;">
                <caption><h2>Сотрудники с незаполненным временем в Tempo</h2></caption>
                <br>
                <tr>
                    <th ${styleTh}>ФИО</th>
                    ${headerTh}
                </tr>
                """
                def footerTable = """
                    </table>
                """
                def bodyTable = """"""

                mapTeam.each {
                    def rowTd = ""
                    def arrRedDay = []
                    try {
                        arrRedDay = it.value.collect { el ->
                            Integer.parseInt(el.split('-')[2])
                        }
                    } catch (Exception ex) {}

                    for (i in listDay) {
                        if (arrRedDay.size() > 0) {
                            def daySaturday = Integer.parseInt(listDay.get(listDay.size()-2))
                            def daySunday = Integer.parseInt(listDay.get(listDay.size()-1))
                            def currentDay = Integer.parseInt(i)
                            if (arrRedDay.contains(currentDay) && (currentDay != daySaturday && currentDay != daySunday)) {
                                rowTd += "<td ${styleRedTd}></td>"
                            } else {
                                def hour = ""
                                try {
                                    def localUserName = it.key
                                    def listLocalTimeSpend = listTimeSpendSecondsTeam.find{ it.key == localUserName }?.value
                                    hour = listLocalTimeSpend.find { it.split(':')[0] == i.toString()}.split(':')[1]
                                } catch (Exception ex) {
                                    hour = ""
                                }
                                try {
                                    def hour_8 =  Double.parseDouble(hour.split(' ')[0]);
                                    if (hour_8 < 8 || hour_8 > 8) {
                                        rowTd += "<td ${styleYellowTd}>${hour}</td>"
                                    } else {
                                        rowTd += "<td ${styleTd}>${hour}</td>"
                                    }
                                } catch (Exception ex) {
                                    rowTd += "<td ${styleTd}>${hour}</td>"
                                }
                            }
                        } else {
                            def hour = ""
                            try {
                                def localUserName = it.key
                                def listLocalTimeSpend = listTimeSpendSecondsTeam.find{ it.key == localUserName }?.value
                                hour = listLocalTimeSpend.find { it.split(':')[0] == i.toString()}.split(':')[1]
                            } catch (Exception ex) {
                                hour = ""
                            }
                            try {
                                def hour_8 =  Double.parseDouble(hour.split(' ')[0]);
                                if (hour_8 < 8 || hour_8 > 8) {
                                    rowTd += "<td ${styleYellowTd}>${hour}</td>"
                                } else {
                                    rowTd += "<td ${styleTd}>${hour}</td>"
                                }
                            } catch (Exception ex) {
                                rowTd += "<td ${styleTd}>${hour}</td>"
                            }
                        }
                    }
                    def unFullName = it.key.toString().contains("%20") ? it.key.toString().replaceAll("%20"," ") : it.key.toString()
                    def userFullName = ComponentAccessor.userManager.getUserByName(unFullName)?.displayName?.toString()
                    bodyTable += """ 
                    <tr>
                        <td ${styleTd}>${userFullName}</td>${rowTd}
                    </tr>
                    """
                }

                def legend = """<br><br>
                    <table style="width:100%;margin-bottom:20px;border:0px solid #dddddd;border-collapse: collapse;">
                        <tr>
                            <th ${styleRedTd}></th>
                            <th style="text-align:left">  - не списано время</th>
                        </tr>
                        <tr>
                            <th ${styleYellowTd}></th>
                            <th style="text-align:left">  - списано меньше или больше 8ч.</th>
                        </tr>
                    </table>
                """
                sendEmail(leadEmail, summaryEmail, headerTable+bodyTable+footerTable  + legend)
                if (teams[idx].id == 25) sendEmail('Kovalenko_M@russianpost.ru', summaryEmail, headerTable+bodyTable+footerTable + legend)

                sendEmail('Kuznetsov.Denis@russianpost.ru', summaryEmail, headerTable+bodyTable+footerTable  + legend)
                //sendEmail('Irina_Koroleva@russianpost.ru', summaryEmail, headerTable+bodyTable+footerTable  + legend)
                //}
            }
        } catch (Exception ex) {
            log.error(ex.message)
        }
    }

} catch (Exception ex) {
    log.error(ex.message)
}
