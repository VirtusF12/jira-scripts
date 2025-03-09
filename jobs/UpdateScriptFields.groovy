import com.atlassian.jira.component.ComponentAccessor as CA
import com.onresolve.scriptrunner.db.DatabaseUtil

// DatabaseUtil.withSql("default") { sql ->
//     sql.execute("""CREATE TABLE roi_scriptfields (
//     issue_id INTEGER,
//     issue_key VARCHAR,
//     field_27404 VARCHAR,
//     field_29402 VARCHAR,
//     field_29403 VARCHAR,
//     field_30201 VARCHAR,
//     field_30202 VARCHAR,
//     PRIMARY KEY (issue_id)
// );""") }

// DatabaseUtil.withSql("default") { sql ->
//     sql.execute("ALTER TABLE roi_scriptfields ADD CONSTRAINT unique_issue_id UNIQUE (issue_id);") }

// DatabaseUtil.withSql("default") { sql ->
//     sql.rows("select * from roi_scriptfields") }

def customFieldManager = CA.customFieldManager
def field_27404 = customFieldManager.getCustomFieldObject(27404L)
def field_29402 = customFieldManager.getCustomFieldObject(29402L)
def field_29403 = customFieldManager.getCustomFieldObject(29403L)
def field_30201 = customFieldManager.getCustomFieldObject(30201L)
def field_30202 = customFieldManager.getCustomFieldObject(30202L)

Issues.search("project = ROI").each { issue ->

    DatabaseUtil.withSql("default") { sql ->

        sql.executeInsert("""INSERT INTO roi_scriptfields (issue_id, issue_key, field_27404, field_29402, field_29403, field_30201, field_30202)
VALUES
    (${issue.id}, ${issue.key}, ${issue?.getCustomFieldValue(field_27404)?.toString() ?: ''}, ${issue?.getCustomFieldValue(field_29402)?.toString() ?: ''}, ${issue?.getCustomFieldValue(field_29403)?.toString() ?: ''}, ${issue?.getCustomFieldValue(field_30201)?.toString() ?: ''}, ${issue?.getCustomFieldValue(field_30202)?.toString() ?: ''})
ON CONFLICT (issue_id) DO UPDATE
SET
    issue_key = ${issue.key},
    field_27404 = ${issue?.getCustomFieldValue(field_27404)?.toString() ?: ''},
    field_29402 = ${issue?.getCustomFieldValue(field_29402)?.toString() ?: ''},
    field_29403 = ${issue?.getCustomFieldValue(field_29403)?.toString() ?: ''},
    field_30201 = ${issue?.getCustomFieldValue(field_30201)?.toString() ?: ''},
    field_30202 = ${issue?.getCustomFieldValue(field_30202)?.toString() ?: ''};""")
    }
}