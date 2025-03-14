import com.atlassian.jira.component.ComponentAccessor as CA
import com.atlassian.jira.component.ComponentAccessor
import groovy.json.JsonParserType
import java.util.ArrayList
import javax.ws.rs.core.Response
import groovy.json.JsonSlurper
import java.time.*
import java.net.URLEncoder
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
import ru.atlassian.jira.api.MyPluginComponent
import java.util.*
import com.atlassian.jira.util.BuildUtilsInfo
import groovy.transform.Field


@WithPlugin("ru.atlassian.jira.jsp")
@PluginModule MyPluginComponent myPluginComponent

def baseUrl = CA.getApplicationProperties().getString("jira.baseurl")
Map<String,Integer> mapCDBTeamId = new HashMap()
mapCDBTeamId.put("6813ba53-cba6-4472-8470-d05a935bbe13", 232) // key = имя_конманды_кшд, value = id_команды_jira
mapCDBTeamId.put("6813ba62-cba8-4272-8480-d05a935bbe14", 236)

def hostCDB = "databus-pre.test.rp.ru" // "databus.test.rp.ru"
// def host_test = "https://databus.test.rp.ru"
def host_pre_test = "https://databus-pre.test.rp.ru"
//def host_cdb = "https://dc02-cdb.rp.ru"
def topic_name = "HRPT-TYPE-TEAM"
def tail_offsets = "/api/v2/topics/${topic_name}/partitions/0/offsets"
def tail = "/api/v2/topics/${topic_name}/partitions/0/messages/types/TEAM?offset=4&count=10"
def url = "${host_pre_test}${tail}"
def url_offsets = "${host_pre_test}${tail_offsets}"
def token = "token"  // от databus.test.rp.ru
def message_type = "json"
// log.debug("url = ${url}")

class ExceptionTeam {

    boolean isValid;
    String errorMessage;

    ExceptionTeam(boolean isValid, String errorMessage) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
    }

    boolean isValidTeam() { return this.isValid }
    String getErrorMessage() { return  this.errorMessage }
}

ExceptionTeam exceptionTeam(String valueTeamJson) {

    String exceptionMessage = ""
    boolean isValidTeam = true
    def slurper = new JsonSlurper()
    boolean isValidJson = true
    try {
        slurper.parseText(valueTeamJson)
        isValidJson =  true
    } catch (ignored) {
        isValidJson =  false
    }

    if (isValidJson) {
        def objValueJson = slurper.parseText(valueTeamJson)
        def uuidTeam = objValueJson.uuid
        def objTeamList = objValueJson.team_list
        log.debug("objTeamList.size() = ${objTeamList.size()}")

        /* ----- check TEAM (lead) ----- */
        try {
            def nameTeam = objValueJson.team_name
            def objTeamLead = objValueJson.team_lead
            def nameTeamLead = objTeamLead.name
            def emailTeamLead = objTeamLead.email
            def usernameTeamLead = emailTeamLead.split("@")[0]
            log.debug("""
                    **** TEAM LEAD VALID ****
                    uuidTeam = ${uuidTeam}
                    nameTeam = ${nameTeam}
                    nameTeamLead = ${nameTeamLead}
                    emailTeamLead = ${emailTeamLead}
                    usernameTeamLead = ${usernameTeamLead}
                    **************
                    """)
            CA.getUserManager().getUserByName(usernameTeamLead as String).getKey()
        } catch (ignored) {
            isValidTeam = false

            exceptionMessage += """
            
            Team Lead (username): ${objValueJson.team_lead.email.split("@")[0]} is not found in Jira. ${valueTeamJson}
            
            """
        }

        if (objTeamList.size() > 0) {

            objTeamList.each { item ->

                try {
                    // атрибуты пользователя 
                    def teamMemberName = item.team_member.name
                    def teamMemberEmail = item.team_member.email
                    def username = teamMemberEmail.split("@")[0]
                    def function = item.function.get(0)
                    def uuidRole = function.uuid
                    def roleName = function.role_name
                    def occupation = item.occupation
                    def dateFrom = item.date_from
                    CA.getUserManager().getUserByName(username as String).getKey()
                } catch (ignored) {
                    isValidTeam = false
                    exceptionMessage += """
            
                    User (username): ${item.team_member.email.split("@")[0]} is not found in Jira. ${valueTeamJson}
            
                    """
                }
            }
        }
    } else {
        isValidTeam = false
        exceptionMessage += """
        
        Json не валиден: ${valueTeamJson}
        
        """
    }

    // реализовать написание логов в плагин

    return new ExceptionTeam(isValidTeam, exceptionMessage)
}

def requestGET(String url) {

    def get = new URL(url).openConnection()
    def auth = "username:password".bytes.encodeBase64()
    get.setRequestMethod("GET")
    get.setDoOutput(true)
    get.setRequestProperty("Content-Type", "application/json")
    get.setRequestProperty("Authorization", "Basic $auth")

    def rc = null
    def text = null
    def stream = null
    def result = null
    def slurper = new JsonSlurper()

    try {
        rc = get.getResponseCode()
        stream = get.getErrorStream() ?: get.getInputStream()
        text = stream.getText()
        result = slurper.parseText(text)
        log.debug("exc ===> ${result}")
    } catch(Exception exc) {
        log.debug("exc ===> ${exc}")
        throw exc
    }

    return result
}

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

def postRequest(String jsonString, def url, def method) {

    def post = new URL(url.toString()).openConnection()
    def user_pass = "username:password"
    def auth = user_pass.bytes.encodeBase64()
    post.setRequestMethod(method.toString())
    post.setDoOutput(true)
    post.setRequestProperty("Content-Type", "application/json")
    post.setRequestProperty("Authorization", "Basic $auth")

    def rc = null
    def text = null
    def stream = null
    def result = null
    def slurper = new JsonSlurper()

    try {
        post.getOutputStream().write(jsonString.getBytes("UTF-8"))
        rc = post.getResponseCode()
        stream = post.getErrorStream() ?: post.getInputStream()
        text = stream.getText()
        result = slurper.parseText(text)
        log.debug("exc ===> ${result}")
    } catch(Exception exc) {
        log.debug("exc ===> ${exc}")
        throw exc
    }

    return result
}

def getOffsets(String url, String token, String messageType, String hostCDB) {

    def slurper = new JsonSlurper()
    def get = new URL(url.toString()).openConnection()
    get.setRequestMethod("GET")
    get.setDoOutput(true)
    get.setRequestProperty("Host", hostCDB)
    get.setRequestProperty("Authorization", "AccessToken ${token}")
    get.setRequestProperty("Content-Type", "application/vnd.kafka.${messageType}.v1+json")

    def rc = null
    def text = null
    def stream = null
    def result = null

    try {
        rc = get.getResponseCode()
        stream = get.getErrorStream() ?: get.getInputStream()
        text = stream.getText()
        result = slurper.parseText(text)
        log.error("exc ===> ${result}")
    } catch(Exception exc) {
        log.error("exc ===> ${exc}")
        throw exc
    }

    return result
}

/*
    topic_name_status = "CDB-TYPE-STATUS"
    description: https://confluence.tools.rp.ru/pages/viewpage.action?pageId=489059856#
*/
def requestToCDBTypeStatus(String jsonString, String url, String method, String token, String messageType, String hostCDB) {

    log.debug("---------- start STATUS ----------")
    log.debug(jsonString)
    def slurper = new JsonSlurper()
    def post = new URL(url.toString()).openConnection()
    post.setRequestMethod(method.toString())
    post.setDoOutput(true)
    post.setRequestProperty("Host", hostCDB)
    post.setRequestProperty("Authorization", "AccessToken ${token}")
    post.setRequestProperty("Content-Type", "application/vnd.kafka.${messageType}.v1+json")

    def rc = null
    def text = null
    def stream = null
    def result = null

    try {
        post.getOutputStream().write(jsonString.getBytes("UTF-8"))
        rc = post.getResponseCode()
        log.debug("rc ===> ${rc}")
        stream = post.getErrorStream() ?: post.getInputStream()
        text = stream.getText()
        result = slurper.parseText(text)
        log.debug("exc ===> ${result}")
        log.debug("---------- end STATUS ----------")
    } catch(Exception exc) {
        log.debug("exc ===> ${exc}")
        throw exc
    }

    return result
}


def sendStatusMessage(String message, String token, String messageType, String hostCDB, String msgId, String systemName, String systemVersion) {
    /* 
        STATUS CDB
    */
    BuildUtilsInfo buildUtils = ComponentAccessor.getComponent(BuildUtilsInfo.class)
    def jiraVersion = buildUtils.version
    def nowDateTime = new Date().format("YYYY-MM-dd'T'HH:mm:ss", TimeZone.getTimeZone("Europe/Moscow")).toString() + "+03:00"
    log.debug("${nowDateTime}")
    def resultStatusCDB = requestToCDBTypeStatus("""
    {
        "records": 
        [
            {
                "key": 
                {
                    "msgId": "${msgId}",
                    "creationTime": "${nowDateTime}",
                    "system": {
                        "name": "Jira",
                        "version": "${jiraVersion}"
                    },
                    "recipient": {  
                        "address": "deprecated",
                        "cluster": "cdb",
                        "topic": "HRPT-TYPE-STATUS"
                    },
                    "type": "STATUS"
                },
                "value": 
                {
                    "replyOn": "${msgId}",
                    "sourceSystem": {
                        "name": "Jira",
                        "version": "${jiraVersion}"
                    },
                    "destinationSystem": {
                        "name": "${systemName}",
                        "version": "${systemVersion}"
                    },
                    "description": {
                        "result": "Not processed",
                        "details": "${message}"
                    },
                    "status": "ERROR",
                    "time": "${nowDateTime}",
                    "attributes": {
                        "topicName": "HRPT-TYPE-TEAM"
                    }
                }
            }
        ]
    }
    """, "https://${hostCDB}/api/v2/topics/CDB-TYPE-STATUS", "POST", token, messageType, hostCDB)
    /* */
}



def requestToCDB(String type, String url, String token, String messageType, String host, Map<String,String> mapUuidKey, String hostCDB, MyPluginComponent myPluginComponent) {

    log.debug("hostCDB = ${hostCDB}")
    def slurper = new JsonSlurper()
    def get = new URL(url).openConnection()
    def result = null
    get.setRequestMethod(type)
    get.setDoOutput(true)
    get.setRequestProperty("Host", hostCDB)
    get.setRequestProperty("Authorization", "AccessToken ${token}")
    get.setRequestProperty("Content-Type", "application/vnd.kafka.${messageType}.v1+json")

    try {
        def rc = get.getResponseCode()
        log.debug("rc* = ${rc}")
        def stream = get.getErrorStream() ?: get.getInputStream()
        def text = stream.getText()
        log.debug("text* = ${text}")
        result = slurper.parseText(text)
        log.debug("exc* ===> ${result}")

        def arr = result.records
        log.error("count records arr.size() = ${arr?.size()}")
        def count = 1

        arr.each { data ->

            def objStringKey = new String(data.key.decodeBase64()) // json (key)
            def objStringTeam = new String(data.value.decodeBase64()) // json (value)
            log.debug("""
            -------------------------------------------
            ----------------- DATA CDB ${count} ---------------
            -------------------------------------------
            objStringKey = ${objStringKey}
            objStringTeam = ${objStringTeam}
            """)

            /********** VALID TEAM CDB **********/
            def objExceptionTeam = exceptionTeam(objStringTeam)
            log.error("ERROR MESSAGE TEAM = ${objExceptionTeam.getErrorMessage()}")
            log.error("IS VALID = ${objExceptionTeam.isValidTeam()}")

            /* VALID TEAM == true */
            if (objExceptionTeam.isValidTeam()) {


                /* KEY CDB */
                def objKeyJson = slurper.parseText(objStringKey)
                def msgIdKey = objKeyJson.msgId.toString()
                def creationTimeKey = objKeyJson.creationTime.toString()
                def systemKey = objKeyJson.system
                def systemNameKey = systemKey.name
                def systemVersionKey = systemKey.version
                def recipientKey = objKeyJson.recipient.toString()
                def typeKey = objKeyJson.type.toString()

                String resultKey = """ 
                    msgId = ${msgIdKey},
                    creationTime = ${creationTimeKey},
                    system = ${systemNameKey},
                    recipient = ${recipientKey},
                    type = ${typeKey}
                """
                log.debug("resultKey = ${resultKey}")



                /********** VALUE CDB **********/
                def objValueJson = slurper.parseText(objStringTeam)
                def uuidTeam = objValueJson.uuid
                def teamId = mapUuidKey.get(uuidTeam) != null ? mapUuidKey.get(uuidTeam) : -1
                log.debug("teamId = ${teamId}")

                if (teamId > 0) {

                    /* ----- TEAM (name, lead) ----- */
                    def nameTeam = objValueJson.team_name
                    def objTeamLead = objValueJson.team_lead
                    def nameTeamLead = objTeamLead.name
                    def emailTeamLead = objTeamLead.email
                    def usernameTeamLead = emailTeamLead.split("@")[0]
                    log.debug("""
                    **** TEAM ****
                    uuidTeam = ${uuidTeam}
                    nameTeam = ${nameTeam}
                    nameTeamLead = ${nameTeamLead}
                    emailTeamLead = ${emailTeamLead}
                    usernameTeamLead = ${usernameTeamLead}
                    **************
                    """)

                    def userKeyTeamLead = CA.getUserManager().getUserByName(usernameTeamLead.toString())?.getKey()
                    //def userKeyTeamLead = CA.getUserManager().getUserByName("nataliya.sosedova")?.getKey() 
                    log.debug("userKeyTeamLead = ${userKeyTeamLead}")
                    if (userKeyTeamLead == null) {
                        log.error("ERROR (Начало отправки) ERROR")
                        /* SEND ERROR MESSAGE TO CDB */
                        // sendStatusMessage("Сообщение разобрано с ошибкой (не найден пользователь ${usernameTeamLead})", token, messageType, hostCDB, msgIdKey, systemNameKey, systemVersionKey)
                        //////////////////////////////
                        log.error("ERROR (Ошибка отправлена в CDB) ERROR")
                    }

                    def resultUpdateTeam = postRequest("""{
                                            "id": ${teamId},
                                            "isPublic": true,
                                            "lead": "${userKeyTeamLead}",
                                            "leadUser": {
                                                "displayname": "${nameTeamLead}",
                                                "jiraUser": true,
                                                "key": "string",
                                                "name": "${usernameTeamLead}"
                                            },
                                            "mission": "",
                                            "name": "${nameTeam}",
                                            "summary": ""
                                        }""", host + "/rest/tempo-teams/2/team/${teamId}", "PUT")
                    // ,
                    //     "teamProgram": {
                    //         "id": 22,
                    //         "manager": {},
                    //         "name": "Департамент развития и сопровождения продуктов 1С"
                    //     } 
                    log.debug("resultUpdateTeam = ${resultUpdateTeam}")


                    /* ----- VALUE STREAM ----- */
                    def objValueStream = objValueJson?.value_stream

                    if (objValueStream != null) {
                        def uuidValueStream = objValueStream.uuid
                        def valueStreamName = objValueStream.stream_name
                        def objStreamCTO = objValueStream.stream_cto
                        def nameStreamCTO = objStreamCTO.name
                        def emailStreamCTO = objStreamCTO.email
                        def usernameStreamCTO = emailStreamCTO.toString().split("@")[0]
                        def userKeyCTO = CA.getUserManager().getUserByName(usernameStreamCTO).getKey()

                        log.debug("""
                        **** VALUE STREAM ****
                        uuidValueStream = ${uuidValueStream}
                        valueStreamName = ${valueStreamName}
                        nameStreamCTO = ${nameStreamCTO}
                        emailStreamCTO = ${emailStreamCTO} 
                        **********************
                        """)

                        /* ID PROGRAM CTO */
                        def idProgramJira = requestGET(host + "/rest/tempo-teams/2/program")
                                .find { itemProgram -> itemProgram.name.toLowerCase().equals(valueStreamName.toLowerCase()) }
                                .find {k,v -> k == "id"}?.value
                        log.debug("idProgramJira = " + idProgramJira)

                        if ((idProgramJira == "null") || (idProgramJira == null)) { // если такого id программы в Tempo НЕТ 

                            def resultAddProgramToJira = postRequest("""
                            {
                                "name": "${valueStreamName}",
                                "manager": {
                                    "name": "${usernameStreamCTO}",
                                    "key": "${userKeyCTO}",
                                    "jiraUser": true,
                                    "displayname": "${nameStreamCTO}"
                                }
                            }
                            """,
                                    host + "/rest/tempo-teams/2/program",
                                    "POST")
                            log.debug("resultAddProgramToJira = ${resultAddProgramToJira}")
                        }

                        def resultUpdateTeamProgram = postRequest("""{
                                            "id": ${teamId},
                                            "isPublic": true,
                                            "lead": "${userKeyTeamLead}",
                                            "leadUser": {
                                                "displayname": "${nameTeamLead}",
                                                "jiraUser": true,
                                                "key": "string",
                                                "name": "${usernameTeamLead}"
                                            },
                                            "mission": "",
                                            "name": "${nameTeam}",
                                            "summary": "",
                                            "teamProgram": {
                                                "id": ${idProgramJira},
                                                "manager": {},
                                                "name": "${valueStreamName}"
                                            } 
                                        }""", host + "/rest/tempo-teams/2/team/${teamId}", "PUT")

                        log.debug("resultUpdateTeamProgram = ${resultUpdateTeamProgram}")
                    }

                    def objTeamList = objValueJson.team_list
                    log.debug("objTeamList.size() = ${objTeamList.size()}")

                    if (objTeamList.size() > 0) {

                        objTeamList.each { item ->

                            // атрибуты пользователя 
                            def teamMemberName = item.team_member.name
                            def teamMemberEmail = item.team_member.email
                            def username = teamMemberEmail.split("@")[0]
                            def function = item.function.get(0)
                            def uuidRole = function.uuid
                            def roleName = function.role_name
                            def occupation = item.occupation
                            def dateFrom = item.date_from

                            log.debug("**** item = ${item}")
                            log.debug("""
                            **** MEMBER **** 
                            teamMemberName = ${teamMemberName} 
                            teamMemberEmail = ${teamMemberEmail}
                            username = ${username}
                            function = ${function}
                            uuidRole = ${uuidRole}
                            roleName = ${roleName}
                            occupation = ${occupation} 
                            dateFrom = ${dateFrom}
                            function = ${item.function}
                            **************** 
                            """)

                            /* TEMPO */
                            def idRoleJira = requestGET(host + "/rest/tempo-teams/2/role")
                                    .find { itemRole -> itemRole.name.toLowerCase().equals(roleName.toLowerCase()) }
                                    .find {k,v -> k == "id"}?.value
                            log.debug("idRoleJira = " + idRoleJira)

                            if ((idRoleJira == "null") || (idRoleJira == null)) { // если такого id роли в Tempo НЕТ 
                                def resultAddRoleToJira = postRequest("""
                                            {
                                                "name": "${roleName}"
                                            }
                                        """
                                        , host + "/rest/tempo-teams/2/role"
                                        , "POST")
                                log.debug("Роль добавлена в Jira Tempo")
                            }

                            def userInTeam = getRequest(host + "/rest/tempo-teams/2/team/"+teamId+"/member/").find { userTeam ->
                                userTeam["member"]?.name.equals(username)
                            }

                            if (userInTeam == null) { // пользователя нет в команде 

                                idRoleJira = requestGET(host + "/rest/tempo-teams/2/role")
                                        .find { itemRole -> itemRole.name.toLowerCase().equals(roleName.toLowerCase()) }
                                        .find {k,v -> k == "id"}?.value
                                log.debug("** idRoleJira = " + idRoleJira)

                                def resultCreateMember = postRequest("""
                                        {
                                            "member": {
                                                "name": "${username}",
                                                "type": "USER"
                                            },
                                            "membership": {
                                                "availability": "${occupation}",
                                                "dateFrom": "${dateFrom}",
                                                "dateTo": "",
                                                "role": {
                                                    "id": ${idRoleJira}
                                                }
                                            }
                                        }
                                        """
                                        , host + "/rest/tempo-teams/2/team/" + teamId + "/member", "POST")
                                log.debug("""
                                ------------------------------------------------------
                                ПОЛЬЗОВАТЕЛЬ ${username} ДОБАВЛЕН В КОМАНДУ ${teamId} 
                                ------------------------------------------------------
                                """)

                            } else { // пользователь есть в команде (обновить данные)

                                def memberId =  userInTeam["member"]?.teamMemberId
                                def membershipId = userInTeam["membership"]?.id
                                // def membershipDateFrom = userInTeam["membership"].dateFrom
                                // def membershipDateTo = userInTeam["membership"].dateTo
                                def membershipAvailability = occupation // userInTeam["membership"].availability

                                def resultUpdateUserRole = postRequest("""
                                        {
                                            "availability": "${membershipAvailability}",
                                            "dateFrom": "${dateFrom}",
                                            "dateTo": "",
                                            "role": {
                                                "id": ${idRoleJira}
                                            }
                                        }
                                        """
                                        , host + "/rest/tempo-teams/2/team/"+teamId+"/member/"+memberId+"/membership/"+membershipId
                                        , "PUT")
                            }
                            /* TEMPO */
                        }
                    }

                }
                count++
            } else { /* VALID TEAM == true */

                /**** SAVE DATA IN PLUGIN ****/
                myPluginComponent.saveDataCDB(objStringKey, objStringTeam + " | " + (objExceptionTeam.getErrorMessage() as String))
                // log.error("data = ${myPluginComponent.getDataCDB()}") 

                def objKeyJson = slurper.parseText(objStringKey)
                def msgIdKey = objKeyJson.msgId.toString()
                def creationTimeKey = objKeyJson.creationTime.toString()
                def systemKey = objKeyJson.system
                def systemNameKey = systemKey.name
                def systemVersionKey = systemKey.version

                /* SEND ERROR MESSAGE TO CDB */
                sendStatusMessage(objExceptionTeam.getErrorMessage(), token, messageType, hostCDB, msgIdKey.toString(), systemNameKey.toString(), systemVersionKey.toString())

                log.error("Отправлено сообщение в КШД STATUS")

            }
        } // end arr.each

    } catch(Exception exc) {
        log.debug("exc ===> ${exc}")
        throw exc
    }

    return result
}

log.error("url offsets = ${url_offsets}")
def offsets = getOffsets(url_offsets, token, message_type, hostCDB) // exc ===> [partition:0, startOffset:12, endOffset:12]
def partition = offsets["partition"]
def startOffset = offsets["startOffset"]
def endOffset = offsets["endOffset"]
log.error("""
            **** RESULT cdb partition = ${partition}    ****
            **** RESULT cdb startOffset = ${startOffset} ****
            **** RESULT cdb endOffset = ${endOffset}   ****
          """)

tail = "/api/v2/topics/${topic_name}/partitions/${partition}/messages/types/TEAM?offset=${startOffset}&count=100"
url = "${host_pre_test}${tail}"
log.error("url = ${url}")

def result = requestToCDB("GET", url, token, message_type, baseUrl, mapCDBTeamId, hostCDB, myPluginComponent)
log.error("**** RESULT cdb request = ${result} ****")
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////