/**
* Rule Builder
*
* Author: SmartThings
*
* Date: 2013-02-24
*/
definition(
name: "Rule Builder2",
namespace: "rhworkshop/Rules",
author: "SmartThings",
description: "Your Rule Builder dashboard solution.",
category: "SmartSolutions",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/rule-builder.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/rule-builder@2x.png"
)

preferences {
page(name: "firstPage")
}

def firstPage() {
dynamicPage(name: "firstPage", title: "Set up your rules") {
section {
app(name: "rulesChildren", appName: "Rules", namespace: "rhworkshop/Rules", title: "Create a new Rule", multiple: true)
}
}
}

def installed() {
log.debug "Installed with settings: ${settings}"

updateSolutionSummary()
}

def updated() {
log.debug "Updated with settings: ${settings}"

unsubscribe()
updateSolutionSummary()
}

def updateSolutionSummary() {
def rulesCount = childApps?.size() ?: 0
def summaryData = []
if (rulesCount) {
summaryData << ["icon":"indicator-dot-green","iconColor":"#49a201","default":"true","value":"$rulesCount rules configured"]
} else {
summaryData << ["icon":"indicator-dot-gray","iconColor":"#878787","default":"true","value":"$rulesCount rules configured"]
}
log.info "SUMMARY EVENT data: $summaryData"
sendEvent(linkText:app.label, descriptionText:app.label + " updating summary", eventType:"SOLUTION_SUMMARY",
name: "summary", value: summaryData*.value?.join(", "), data:summaryData, isStateChange: true, displayed: true)
}