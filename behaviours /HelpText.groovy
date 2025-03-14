

if (underlyingIssue?.getStatus()?.name in ["Согласование инвест. заявки"]) {

    getFieldById("customfield_33611").setRequired(true)
    getFieldById("customfield_33611").setHelpText("<b>«Проверьте корректность индекса PI и проставьте о верификации / не верификации показателя»</b>")
}