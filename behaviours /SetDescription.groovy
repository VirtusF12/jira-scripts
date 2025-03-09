def desc = getFieldById("description")

def defaultValue = """
*Предусловия*
 
 
*Шаги воспроизведения*
 
 
*Фактический результат*
 
 
*Ожидаемый результат*
 
 
""".replaceAll(/    /, '')

if (! underlyingIssue?.description) {
    desc.setFormValue(defaultValue)
}
