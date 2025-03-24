
/*
    ATL New Feature Workflow
    Выгрузка талона в Excel
*/

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

def user = ComponentAccessor.getJiraAuthenticationContext()?.getUser()
def attachmentManager = ComponentAccessor.getAttachmentManager()
def String filename = "ssd.xls"
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
def listHeaderTable = ["ADDRESSLINE","ADRESAT","MASS","VALUE","PAYMENT","COMMENT","ORDERNUM","TELADDRESS","MAILTYPE","MAILCATEGORY","INDEXFROM","VLENGTH","VWIDTH","VHEIGHT","FRAGILE","ENVELOPETYPE","NOTIFICATIONTYPE","COURIER","SMSNOTICERECIPIENT","WOMAILRANK","PAYMENTMETHOD","NOTICEPAYMENTMETHOD","COMPLETENESSCHECKING",]
listHeaderTable.eachWithIndex { it,index->
    Cell cell = r.createCell(index)
    cell.setCellValue(it.toString())
    cell.setCellStyle(style)
}

/* body table */
def assignee = issue.getAssignee().getDisplayName().toString()

r = s.createRow(rownum++)
def listBodyTable = ["Москва, Варшавское шоссе, 37",assignee,"2.150","1000.25","","Заказ 10001","10001","7 (495) 956-20-67","4"]
listBodyTable.eachWithIndex { it,index->
    Cell cell = r.createCell(index)
    cell.setCellValue(it.toString())
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