/**
* Rules
*
* Author: SmartThings
*
* Date: 2013-02-21
*/
definition(
name: "Rules",
namespace: "rhworkshop/Rules",
parent: "rhworkshop/Rules:Rule Builder",
author: "SmartThings",
description: "Build generic rules to be executed.",
category: "SmartSolutions",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/rule-builder.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/rule-builder@2x.png"
)

preferences {
page(name: "hrefsPage")
page(name: "triggersPage")
page(name: "conditionsPage")
page(name: "actionsPage")
}

def hrefsPage() {
dynamicPage(name: "hrefsPage", title: "Build your rule", install: true, uninstall: true) {
section {

def selectedTriggers = selectedTriggersText()
def selectedConditions = selectedConditionsText()
def selectedActions = selectedActionsText()

href(
name: "toTriggersPage",
page: "triggersPage",
title: selectedTriggers ?: "Select your triggers",
required: false,
description: selectedTriggers ? null : "Tap to show",
state: selectedTriggers ? "complete" : ""
)
href(
name: "toConditionsPage",
page: "conditionsPage",
title: selectedConditions ?: "Select your conditions",
required: false,
description: selectedConditions ? null : "Tap to show",
state: selectedConditions ? "complete" : ""
)
href(
name: "toActionsPage",
page: "actionsPage",
title: selectedActions ?: "Select your actions",
required: false,
description: selectedActions ? null : "Tap to show",
state: selectedActions ? "complete" : ""
)
}
section {
label(
title: "Label this SmartApp",
required: false
)
}
// TODO: Add optional mode selection
}
}

def triggersPage(params) {

dynamicPage(name: "triggersPage", title: "Select your triggers") {

getAllTriggerSettings().each { settingName, triggerDevice ->
try {
section {
deviceSelectionInput("sensor", settingName)
if(triggerDevice)
{
triggerDeviceStateInput(settingName, triggerDevice)
}
}
} catch(Exception e){ log.warn "Bad input, skipping $settingName in RuleBuilder. $e" }
}

section {
input(
name: newDeviceInputName("trigger", ""),
type: "capability.sensor",
title: "Add a trigger",
required: false,
multiple: false,
refreshAfterSelection:true
)
}

}
}

def conditionsPage(params) {

dynamicPage(name: "conditionsPage", title: "Select your conditions") {

getAllConditionSettings().each { settingName, conditionDevice ->
try {
section {
deviceSelectionInput("sensor", settingName)
if(conditionDevice)
{
conditionDeviceStateInput(settingName, conditionDevice)
conditionDeviceStateValueInput(settingName)
}
}
} catch(Exception e){ log.warn "Bad input, skipping $settingName in RuleBuilder. $e" }
}

section {
input(
name: newDeviceInputName("condition", ""),
type: "capability.sensor",
title: "Add a condition",
required: false,
multiple: false,
refreshAfterSelection:true
)
}
}
}


def actionsPage() {

dynamicPage(name: "actionsPage", title: "Select your actions") {

getAllActionSettings().each { settingName, actionDevice ->
try {
section {
deviceSelectionInput("actuator", settingName)
if(actionDevice)
{
actionDeviceStateInput(settingName, actionDevice)
}
}
} catch(Exception e){ log.warn "Bad input, skipping $settingName in RuleBuilder." }
}

section {
input(
name: newDeviceInputName("action", ""),
type: "capability.actuator",
title: "Add an action",
required: false,
multiple: false,
refreshAfterSelection:true
)
}

section {
input(
name: "action.notify",
type: "enum",
title: "Notify me",
options:notificationOptions(),
required: false,
multiple: true,
refreshAfterSelection:true
)
if(settings["action.notify"])
{
if(settings["action.notify"].contains("text"))
{
input("recipients", "contact", title: "Send notifications to", required: false) {
input "phone", "phone", title: "Phone Number", required: false
}
}

input "messageText", "text", title: "Message Text", required: false
input "frequency", "decimal", title: "Minimum time between messages (optional, defaults to every message)", required: false
}
}

}
}


// ELEMENTS

def deviceSelectionInput(inputType, inputName, title=null)
{
try
{
if(title == null)
{
title = "Select ${inputType} devices"
}

input(
name: inputName,
type: "capability.${inputType}",
title: title,
required: false,
multiple: false,
refreshAfterSelection:true
)
}
catch(Exception e) { log.warn "RuleBuilder: Error displaying input for ${inputType} ${inputName}: $e" }
}

def deviceInput(inputName, title=null) {
try
{
def capability = inputName.split(/\\./)[2]

if(title == null)
{
title = "Select ${capability} devices"
}

input(
name: inputName,
type: "capability.${capability}",
title: title,
required: false,
multiple: true,
refreshAfterSelection:true
)
}
catch(Exception e) { log.warn "RuleBuilder: Error displaying input for ${inputName}: $e" }
}

def triggerDeviceStateInput(deviceInputName, triggerDevice) {
try
{
def inputName = deviceInputName.replaceAll(/triggerDevice\\./, "triggerState.")
input(
name: inputName,
title: "When",
type: "enum",
options: triggerOptions(triggerDevice),
required: false,
multiple: false
)
} catch(Exception e){ log.debug "Error triggerDeviceStateInput(${deviceInputName}, ${triggerDevice}): $e" }
}

def conditionDeviceStateInput(deviceInputName, conditionDevice) {
try
{
def inputName = conditionStateInputNameFromInputName(deviceInputName)
input(
name: inputName,
title: "When",
type: "enum",
options: conditionOptions(conditionDevice),
required: false,
multiple: false,
refreshAfterSelection:true
)
} catch(Exception e){}
}

def conditionDeviceStateValueInput(deviceInputName) {
try
{
def inputName = conditionStateInputNameFromInputName(deviceInputName)
def attribute = settings[inputName]

def value = attribute.split(/\\./).first()

if(value == "NUMBER" || value == "STRING" || value == "DYNAMIC_ENUM")
{
def valueType = value.toLowerCase()
if(value == "DYNAMIC_ENUM")
{
valueType = "string"
}

input(
name: conditionStateInputComparisonOperatorFromInputName(deviceInputName),
title: "When",
type: "enum",
options: conditionStateInputComparisonOperatorOptions(),
required: false,
multiple: false,
defaultValue: "equal"
)

input(
name: conditionStateInputComparisonNameFromInputName(deviceInputName),
title: "When",
type: valueType,
required: false
)
}
} catch(Exception e){}
}

def conditionStateInputNameFromInputName(deviceInputName)
{
return deviceInputName.replaceAll(/conditionDevice\\./, "conditionState.")
}

def conditionStateInputComparisonOperatorFromInputName(deviceInputName)
{
return deviceInputName.replaceAll(/conditionDevice\\./, "conditionStateComparisonOperator.")
}

def conditionStateInputComparisonNameFromInputName(deviceInputName)
{
return deviceInputName.replaceAll(/conditionDevice\\./, "conditionStateComparisonValue.")
}

def conditionStateInputComparisonOperatorOptionsDefinition()
{
return [
[key:"equal", symbol:"=", display: "equal to (=)"],
[key:"notEqual", symbol:"!=", display: "not equal to (!=)"],
[key:"lessThan", symbol:"<", display: "less than (<)"],
[key:"greaterThan", symbol:">", display: "greater than (>)"],
[key:"lessThanEqual", symbol:"<=", display: "less than or equal to (<=)"],
[key:"greaterThanEqual", symbol:">=", display: "greater than or equal to (>=)"]
]
}

def conditionStateInputComparisonOperatorOptions()
{
return conditionStateInputComparisonOperatorOptionsDefinition().inject([:]) { c, it ->
c[it.key] = it.display
return c
}

// return [
// "equal": "equal to (=)",
// "notEqual": "not equal to (!=)",
// "lessThan": "less than (<)",
// "greaterThan": "greater than (>)",
// "lessThanEqual": "less than or equal to (<=)",
// "greaterThanEqual": "greater than or equal to (>=)"
// ]
}

def comparisonOperatorDisplayName(comparisonOperatorKey)
{
return conditionStateInputComparisonOperatorOptionsDefinition().find { it.key == comparisonOperatorKey }?.symbol
}

def actionDeviceStateInput(deviceInputName, actionDevice) {
try
{
def inputName = deviceInputName.replaceAll(/actionDevice\\./, "actionState.")
input(
name: inputName,
title: "When",
type: "enum",
options: actionOptions(actionDevice),
required: false,
multiple: false
)
} catch(Exception e){ log.debug "error actionDeviceStateInput(${deviceInputName}, ${actionDevice}): $e" }
}

// SUPPORTED CAPABILITIES

def supportedTriggerCapabilities() {
[
"sensor"
]
}

def supportedActionCapabilities() {
[
"actuator"
]
}

def supportedConditionCapabilities() {
return supportedTriggerCapabilities() + supportedActionCapabilities()
}

// ELEMENT OPTIONS

def triggerOptions(device) {
def inputEnumValues = device.supportedAttributes.inject([:]) { collector, it ->
if (it.dataType == "ENUM") {
it.values.each { value ->
def k = "${it.name}.${value}"
def v = "${it.name} is ${value}"
collector[k] = v
}
}
return collector
}
}

def conditionOptions(device) {
def inputEnumValues = device.supportedAttributes.inject([:]) { collector, it ->
if (it.dataType == "ENUM")
{
it.values.each { value ->
def k = "${it.dataType}.${it.name}.${value}"
def v = "${it.name} is ${value}"
collector[k] = v
}
}
else if(it.dataType == "NUMBER" || it.dataType == "STRING" || it.dataType == "DYNAMIC_ENUM")
{
def k = "${it.dataType}.${it.name}"
collector[k] = it.name
}
return collector
}
}

def triggerReadableVersion(triggerEventName)
{
return (triggerEventName - "ENUM.").replaceAll(/\\./, ' is ')
}

def conditionReadableVersion(conditionEventName)
{
return conditionEventName.split(/\\./).drop(1).join(' is ')
}

def actionOptions(actionDevice) {
def actionEnumValues = actionDevice.capabilities*.commands.collect { it.name }.flatten()

return actionEnumValues
}


// SETTINGS

Map getAllSettings(type) { return settings.findAll { it.key.contains(".${type}.") }.sort() }

Map getAllTriggerSettings() { return getAllSettings("triggerDevice") }
Map getAllConditionSettings() { return getAllSettings("conditionDevice") }
Map getAllActionSettings() { return getAllSettings("actionDevice") }

def getTriggerStateSetting(deviceInputName) { return getStateSettingFor("trigger", deviceInputName) }
def getConditionStateSetting(deviceInputName) { return getStateSettingFor("condition", deviceInputName) }
def getActionStateSetting(deviceInputName) { return getStateSettingFor("action", deviceInputName) }

def getStateSettingFor(deviceInputType, deviceInputName)
{
def inputName = getStateSettingNameForDeviceInputSettingName(deviceInputType, deviceInputName)
return settings[inputName]
}

def getStateSettingNameForDeviceInputSettingName(deviceInputType, deviceInputName)
{
return deviceInputName.replaceAll(/\\.${deviceInputType}Device\\./, ".${deviceInputType}State.")
}


def newDeviceInputName(inputDeviceType, capability) { "${now()}.${inputDeviceType}Device.${capability}" }
def newTriggerName(capability) { return newDeviceInputName("trigger", capability) }
def newConditionName(capability="") { return newDeviceInputName("condition", capability) }
def newActionName(capability) { return newDeviceInputName("action", capability) }


// LABELS

def userFacingLabel(device) { return device.label ?: device.name }

def selectedTriggersText()
{
def text = null

def l = getAllTriggerSettings().collect { inputName, triggerDevice ->
def triggerDeviceState = getTriggerStateSetting(inputName)
if(triggerDeviceState || triggerDevice instanceof physicalgraph.app.DeviceWrapper )
{
return [triggerDeviceState, triggerDevice]
}
return null
}.findAll { it }

if(l)
{
text = "When " + l.collect { buildTriggerItemText(it[0], it[1]) }.findAll { it }.join(" OR ")
}

return text
}

def buildTriggerItemText(deviceState, device)
{
if(deviceState && device instanceof physicalgraph.app.DeviceWrapper )
{
return "${device} ${triggerReadableVersion(deviceState)}"
}
return null
}

def selectedActionsText()
{
def text = null
// def l = supportedActionCapabilities().collect { actionCapability ->
def l = getAllActionSettings().collect { inputName, actionDevices ->
def actionDeviceState = getActionStateSetting(inputName)

if(actionDeviceState || actionDevices?.findAll { it instanceof physicalgraph.app.DeviceWrapper })
{
return [actionDeviceState, actionDevices]
}
return null
}.findAll { it }

if(l || settings["action.notify"])
{
text = "Then "
}

if(l)
{
text += "set " + l.collect { buildActionItemText(it[0], it[1]) }.findAll { it }.join(" AND ")
}

if(l && settings["action.notify"])
{
text += " AND "
}

if(settings["action.notify"])
{
text += "Notify me via " + settings["action.notify"].collect { messageNotificationDisplayName(it) }.join(" and ")
}

return text
}

def notificationOptions()
{
return ["text":"Text Message", "push":"Push Notification"]
}

def messageNotificationDisplayName(notificationKey)
{
return notificationOptions()[notificationKey]
}

def buildActionItemText(deviceState, device)
{
if(deviceState && device instanceof physicalgraph.app.DeviceWrapper )
{
return "${device} to ${deviceState}"
}
return null
}

def selectedConditionsText()
{
def text = null

try
{
def l = getAllConditionSettings().collect { inputName, conditionDevice ->

log.debug "selectedConditionsText(): $inputName $conditionDevice (${conditionDevice instanceof physicalgraph.app.DeviceWrapper})"

def conditionDeviceState = getConditionStateSetting(inputName)

if(conditionDeviceState || conditionDevice instanceof physicalgraph.app.DeviceWrapper )
{
return [conditionDeviceState, conditionDevice, inputName]
}
return null
}.findAll { it }

if(l)
{
text = "And " + l.collect { buildConditionItemText(it[0], it[1], it[2]) }.findAll { it }.join(" AND ")
}
} catch(Exception e) { log.error "uh-oh selectedConditionsText() :: $e" }


return text
}

def buildConditionItemText(deviceState, device, deviceInputName)
{
if(deviceState && device instanceof physicalgraph.app.DeviceWrapper )
{
if(isEnum(deviceState))
{
return "${device} ${conditionReadableVersion(deviceState)}"
}
else
{
def name = conditionReadableVersion(deviceState)
def comparisonOperatorName = settings[conditionStateInputComparisonOperatorFromInputName(deviceInputName)]
def comparisonValue = settings[conditionStateInputComparisonNameFromInputName(deviceInputName)]

def comparisonOperator = comparisonOperatorDisplayName(comparisonOperatorName)

return "${device} $name $comparisonOperator $comparisonValue"
}
}
return null
}

def isEnum(deviceStateName)
{
return deviceStateName.contains("ENUM")
}

// HANDLERS

def installed() {
log.debug "Installed with settings: ${settings}"

initialize()
}

def updated() {
log.debug "Updated with settings: ${settings}"

unsubscribe()
initialize()
}

def initialize() {

getAllTriggerSettings().each { settingName, triggerDevices ->
def triggerSetting = getTriggerStateSetting(settingName)
triggerDevices.each { trigger ->
subscribe(trigger, "${triggerSetting}", eventHandler)
}
}
parent?.updateSolutionSummary()
}

def compareValues(thisConditionValue, comparisonOperatorName, comparisonValue)
{
def value = false
switch(comparisonOperatorName)
{
case "equal":
value = (thisConditionValue == comparisonValue)
break

case "notEqual":
value = (thisConditionValue != comparisonValue)
break

case "lessThan":
value = (thisConditionValue < comparisonValue)
break

case "greaterThan":
value = (thisConditionValue > comparisonValue)
break

case "lessThanEqual":
value = (thisConditionValue <= comparisonValue)
break

case "greaterThanEqual":
value = (thisConditionValue >= comparisonValue)
break

default:
value = false
break
}
return value
}

def eventHandler(evt) {
log.debug "eventHandler: ${evt.deviceId}"
def id = evt.deviceId

def conditionsMet = true

// supportedConditionCapabilities().each { conditionCapability ->
getAllConditionSettings().each { inputName, conditionDevice ->
def conditionDeviceState = getStateSettingFor("condition", inputName)

def conditionDeviceStateParts = conditionDeviceState?.split(/\\./) // "ENUM.motion.active" -> "ENUM", "motion", "active"

if(conditionDeviceStateParts)
{
def conditionStateType = conditionDeviceStateParts[0]
if(isEnum(conditionDeviceState) && conditionDeviceStateParts.size() == 3)
{
def thisConditionValue = conditionDevice.latestValue(conditionDeviceStateParts[1]) // motion
def thisConditionMet = thisConditionValue == conditionDeviceStateParts[2] // active

conditionsMet = conditionsMet && thisConditionMet

log.debug "checking ENUM condition $inputName ${conditionDeviceStateParts[1]} $thisConditionValue == ${conditionDeviceStateParts[2]} (${thisConditionMet}), so all conditionsMet: $conditionsMet"
}
else if(conditionDeviceStateParts.size() == 2 && (conditionStateType == "NUMBER" || conditionStateType == "STRING" || conditionStateType == "DYNAMIC_ENUM"))
{
def thisConditionValue = conditionDevice.latestValue(conditionDeviceStateParts[1])

def comparisonOperatorName = settings[conditionStateInputComparisonOperatorFromInputName(inputName)]
def comparisonValue = settings[conditionStateInputComparisonNameFromInputName(inputName)]

def thisConditionMet = compareValues(thisConditionValue, comparisonOperatorName, comparisonValue)

conditionsMet = conditionsMet && thisConditionMet

log.debug "checking ${conditionDeviceStateParts[0]} condition $inputName ${conditionDeviceStateParts[1]} $thisConditionValue $comparisonOperatorName $comparisonValue (${thisConditionMet}), so all conditionsMet: $conditionsMet"
}
}

}


if(conditionsMet)
{
getAllActionSettings().each { inputName, actionDevices ->

def action = getActionStateSetting(inputName)
log.debug "action: ${actionDevices} -> ${action}"

actionDevices."${action}"()
}

if(canSendMessage())
{

if (location.contactBookEnabled) {
sendNotificationToContacts(settings.messageText, recipients)
}

else {
if (settings["action.notify"]?.contains("push")) {
log.debug "sending push notification ${settings.messageText}"
sendPush(settings.messageText)
} else {
log.debug "NOT sending push notification because settings['action.notify'] contains 'push': ${settings['action.notify']?.contains('push')}"
}

if (settings["action.notify"]?.contains("text") && settings.phone) {
log.debug "sending text message ${settings.messageText} to ${settings.phone}"

sendSms(settings.phone, settings.messageText)
} else {
log.debug "NOT sending text message because settings['action.notify'] contains 'text': ${settings['action.notify']?.contains('push')} and to ${settings.phone}"
}
}
}
else
{
log.debug "Not sending message"
}


}
}

Boolean canSendMessage()
{
return settings["action.notify"] && settings.messageText // && frequency not exceeded
}