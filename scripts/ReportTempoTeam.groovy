import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonParserType
import java.util.ArrayList
import java.time.*
import com.atlassian.jira.user.util.UserManager
import static groovyx.net.http.Method.POST
import org.apache.commons.httpclient.Credentials
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.*
import com.atlassian.jira.bc.project.component.*
import groovyx.net.http.Method.*
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
import groovyx.net.http.HTTPBuilder
import java.time.temporal.TemporalAdjusters
import com.atlassian.jira.bc.user.search.UserSearchService
import static groovyx.net.http.ContentType.JSON
import com.atlassian.jira.event.type.EventDispatchOption
import groovy.json.JsonOutput
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.user.util.UserUtilImpl
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.onresolve.scriptrunner.runner.customisers.PluginModule
import java.util.*
import com.atlassian.jira.util.BuildUtilsInfo
import groovy.transform.Field
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor as CA
import javax.ws.rs.core.Response
import groovy.json.JsonSlurper
import java.time.*
import java.net.URLEncoder
import org.apache.commons.httpclient.methods.*
import com.atlassian.jira.bc.project.component.*
import groovyx.net.http.Method.*
import java.util.Date
import java.sql.Timestamp
import com.atlassian.jira.datetime.*
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
import com.atlassian.jira.component.ComponentAccessor
import org.apache.commons.httpclient.Credentials
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.*
import groovy.json.JsonSlurper
import com.atlassian.jira.bc.project.component.*
import groovyx.net.http.HTTPBuilder
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
import groovyx.net.http.Method.*
import com.atlassian.jira.mail.Email
import com.atlassian.plugin.util.ContextClassLoaderSwitchingUtil
import java.util.Arrays
import java.time.*
import java.time.temporal.TemporalAdjusters
import com.atlassian.jira.bc.user.search.UserSearchService
import javax.activation.DataHandler
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMultipart
import com.atlassian.jira.component.ComponentAccessor
import com.atlassian.jira.mail.util.ByteArrayDataSource
import com.atlassian.mail.Email
import com.atlassian.mail.queue.SingleMailQueueItem
import com.onresolve.scriptrunner.runner.customisers.WithPlugin
import com.atlassian.jira.issue.attachment.CreateAttachmentParamsBean
import com.atlassian.jira.issue.Issue
import com.atlassian.jira.issue.IssueManager
import com.atlassian.jira.issue.search.SearchProvider
import com.atlassian.jira.jql.parser.JqlQueryParser
import com.atlassian.jira.web.bean.PagerFilter
import org.springframework.util.FileCopyUtils
import org.apache.poi.xwpf.usermodel.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.hssf.usermodel.*
import org.apache.poi.sssf.usermodel.*
import org.apache.poi.ss.util.*
import com.atlassian.jira.user.util.UserManager
import com.atlassian.jira.component.ComponentAccessor as CA
import static groovyx.net.http.Method.POST
import org.apache.commons.httpclient.Credentials
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.*
import groovy.json.JsonSlurper
import com.atlassian.jira.bc.project.component.*
import groovyx.net.http.Method.*
import java.time.*
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpRequest
import org.apache.http.protocol.HttpContext
import groovyx.net.http.HTTPBuilder
import java.time.temporal.TemporalAdjusters
import com.atlassian.jira.bc.user.search.UserSearchService
import static groovyx.net.http.ContentType.JSON
import com.atlassian.jira.event.type.EventDispatchOption
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonOutput
import com.atlassian.jira.user.util.UserUtil
import com.atlassian.jira.user.util.UserUtilImpl

def getRequest(def url) {

    HttpClient client = new HttpClient()
    GetMethod method = new GetMethod(url.toString())
    Credentials credentials = new UsernamePasswordCredentials("username","password")
    client.getParams().setAuthenticationPreemptive(true)
    client.getState().setCredentials(AuthScope.ANY, credentials)
    client.executeMethod(method)
    def response = method.getResponseBodyAsString()
    def jsonSlurper = new JsonSlurper()

    return jsonSlurper.parseText(response)
}

try {

    def baseUrl = CA.getApplicationProperties().getString("jira.baseurl")
    def JIRA_API_URL = baseUrl + "/rest/tempo-teams/1/team"
    def JIRA_API_URL_2 = baseUrl + "/rest/tempo-teams/2/team/"
    def user_pass = "username:password"
    //def user_pass = "TempoBot:d.T+3U'o{%p_k"
    log.error("user_pass = ${user_pass}")

    def jira = new HTTPBuilder(JIRA_API_URL)
    jira.client.addRequestInterceptor(new HttpRequestInterceptor() {
        void process(HttpRequest httpRequest, HttpContext httpContext) {
            httpRequest.addHeader('Authorization', 'Basic ' + user_pass.bytes.encodeBase64().toString())
        }
    })

    def user = ComponentAccessor.getJiraAuthenticationContext()?.getUser()
    def attachmentManager = ComponentAccessor.getAttachmentManager()
    def String filename = "reportUsersInTeam.xls"
    File f = new File("/tmp/"+filename)
    f.getParentFile().mkdirs()
    f.createNewFile()

    FileOutputStream out = new FileOutputStream("/tmp/"+filename)
    Workbook wb = new HSSFWorkbook();
    Sheet s = wb.createSheet();
    int rownum = 0
    Row r = s.createRow(rownum++)

    /* style */
    CellStyle style = wb.createCellStyle()
    Font font = wb.createFont()
    font.setBold(true)
    style.setAlignment(HorizontalAlignment.LEFT)
    style.setFont(font)

    /* header table */
    def listHeaderTable = ["ID", "Название команды", "Логин", "ФИО"]
    listHeaderTable.eachWithIndex { it,index->
        Cell cell = r.createCell(index)
        cell.setCellValue(it.toString())
        cell.setCellStyle(style)
    }

    /* body table */
    // def accountsTempo = jira.get(path: JIRA_API_URL)
    def teams = jira.get(path: JIRA_API_URL)
    log.error("teams.size() = " + teams.size())

    for (int idx = 0; idx < teams.size(); idx++) {

        def leadTeam = teams[idx].lead
        def idTeam = teams[idx].id
        def fioLeadTeam = CA.getUserManager().getUserByKey(leadTeam)?.displayName
        def nameTeam = teams[idx].name
        def programTeam = teams[idx].program
        def isPublicTeam = teams[idx].isPublic
        log.debug("idTeam = ${idTeam}, nameTeam = ${nameTeam}, fioLeadTeam = ${fioLeadTeam}, leadTeam = ${leadTeam}, programTeam = ${programTeam}, isPublicTeam = ${isPublicTeam}")
        // def leadEmail = CA.getUserManager().getUserByKey(teams[idx].lead).getEmailAddress().toString()

        r = s.createRow(rownum++)
        def listBodyTable = [idTeam,nameTeam,leadTeam,fioLeadTeam]
        listBodyTable.eachWithIndex { it,index->
            Cell cell = r.createCell(index)
            cell.setCellValue(it.toString())
        }

        // id / Название команды / логин участника / ФИО участника
        getRequest(baseUrl + "/rest/tempo-teams/2/team/"+idTeam+"/member/").each { u ->

            def username = u["member"]["name"]
            def fullname = u["member"]["displayname"]

            r = s.createRow(rownum++)
            listBodyTable = [idTeam,nameTeam,username,fullname]
            listBodyTable.eachWithIndex { it,index->
                Cell cell = r.createCell(index)
                cell.setCellValue(it.toString())
            }
        }
    }

    listHeaderTable.eachWithIndex { it,index->
        s.autoSizeColumn(index)
    }

    wb.write(out)
    out.close()

    def bean = new CreateAttachmentParamsBean.Builder()
            .file(f)
            .filename(filename)
            .contentType("text/plain")
            .author(user)
            .issue(issue)
            .build()
    attachmentManager.createAttachment(bean)

} catch (Exception ex) {
    log.error(ex.message)
}