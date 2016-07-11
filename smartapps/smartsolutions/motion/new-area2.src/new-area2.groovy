/*
* New Area/Room
*
* Author: SmartThings
*
* Date: 2013-11-10
*/
definition(
name: "New Area2",
namespace: "SmartSolutions/Motion",
parent: "SmartSolutions/Motion:Motion2",
author: "SmartThings",
description: "An area or room.",
category: "SmartSolutions",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/areas.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/areas@2x.png"
)

preferences {
page(name: "createArea", title: "Add a new area or room", install: true) {//nextPage: "selectSensors") {
section {
label title:"Give your area or room a name", description: "e.g. Garage, Kitchen, Kid’s Room"
input "motionSensors", "capability.motionSensor", title: "Choose motion sensors for this area or room", multiple: true, required: false
}
}
page(name: "selectSensors", title: "Select motion sensors", nextPage: "areaDetail")
page(name: "areaDetail", title: "What do you want to do when there is activity in this area?", install: true)
page(name: "editArea", title: userHasAnyTag(["internal","vcx_camera"]) ? "Edit name, motion sensors, and camera" : "Edit name and motion sensors")
page(name: "hidden", title: "Static page to hold devices") {
section {
input "motionSensors", "capability.motionSensor", title: "Choose motion sensors for this area or room", multiple: true, required: false
input "turnedOffLights", "capability.switch", title: "Lights to turn on when taking pictures", multiple: true, required:false
input "camera", "capability.imageCapture", title: "Choose camera for this area or room", multiple: false, required: false
}
}
}

def selectSensors() {
dynamicPage(name: "selectSensors", title: "Which things will keep an eye on this room or area?", nextPage: "areaDetail") {
section {
input "motionSensors", "capability.motionSensor", pairedDeviceName: nextPairedDeviceName("$app.label Motion Sensor", motionSensors),
title: "Choose one or more motion sensors", multiple: true, required: false
}

userHasAnyTag(["internal","vcx_camera"]) {
section {
input "camera", "capability.imageCapture", pairedDeviceName: "$app.label Camera",
title: "Choose camera", multiple: false, required: false
}
}
}
}

def areaDetail() {
dynamicPage(name: "areaDetail", title: "What do you want to do when there is activity in the $app.label?", install: true, popToAncestor: "all") {

section {
app "motionAlerts", "SmartSolutions/Motion", "Motion Alerts", title:"Get notified when there is activity", page: "motionAlerts", multiple: false, install: true
}
if (camera) {
section {
app "motionPhotoActivity", "SmartSolutions/Motion", "Motion Photo Activity", title:"Take photos when there is activity", page: "motionPhotoActivity", multiple: false, install: true
}
section {
app "motionPhotoPeriodic", "SmartSolutions/Motion", "Motion Photo Periodic", title:"Take photos periodically", page: "motionPhotoPeriodic", multiple: false, install: true
}
}

section {
href "editArea", title: userHasAnyTag(["internal","vcx_camera"]) ? "Edit name, motion sensors, and camera" : "Edit name and motion sensors", description: ""
}
}
}

def editArea() {
dynamicPage(name: "editArea", title: userHasAnyTag(["internal","vcx_camera"]) ? "Edit name, motion sensors, and camera for the $app.label" : "Edit name and motion sensors for the $app.label") {
section {
label title:"Area Name"
}
section("Motion Sensors") {
input "motionSensors", "capability.motionSensor", pairedDeviceName: nextPairedDeviceName("$app.label Motion Sensor", motionSensors),
title: "Choose motion sensors for the $app.label", multiple: true, required: false
}
userHasAnyTag(["internal","vcx_camera"]) {
section {
input "camera", "capability.imageCapture", pairedDeviceName: "$app.label Camera",
title: "Choose camera for the $app.label", multiple: false, required: false
}
}
}
}

def installed() {
log.debug "Installed with settings: ${settings}"

subscribeToDevices()
}

def updated() {
log.debug "Updated with settings: ${settings}"

unsubscribe()
subscribeToDevices()
}

def subscribeToDevices() {
subscribe(motionSensors, "motion", motionHandler)
subscribe(camera, "image", imageHandler)
updateSolutionState()
parent.updateSolutionSummary()
}

def motionHandler(evt) {
log.info "switchHandler($evt.linkText, $evt.name:$evt.value)"
updateSolutionState()
}

def imageHandler(evt) {
log.debug "imageHandler($evt.linkText, $evt.value)"

def burstComplete = imageDescriptionHandleBurst(evt.value)

if (burstComplete) {
def allBurstImageUrls = atomicState.currentBurstImages*.url
atomicState.currentBurstImages.eachWithIndex { imageData, index ->
index++
def burstSize = allBurstImageUrls.size()
def description
def data = [icon: imageData.url, images: allBurstImageUrls]
if (index == 1 && burstSize == 1) {
description = "A photo was taken in the ${app.label}"

def parsedData = parseJson(evt.data)
if (parsedData.videoURL) {
//This is a video event
data = [:]
data.imagePath = parsedData.imagePath
data.videoURL = parsedData.videoURL
}
} else if (index < burstSize) {
description = "Burst photo $index of $burstSize was taken in the ${app.label}"
} else {
description = "A burst of $burstSize photos was taken in the ${app.label}"
}
log.debug "DATA: $data"
log.debug "DEVICEID $deviceId"
sendEvent(descriptionText: description, deviceId: evt.deviceId, eventType:"IMAGE", displayed: false, name: "image", value: imageData.value,
data: data, isStateChange: true)
}
atomicState.inBurst = false
atomicState.currentBurstImages = []

// Not in the middle of a photo burst, so update our solution summary
setSolutionState(getSolutionEventValue())
parent.updateSolutionSummary()
// Turn back off any lights that were turned on to take the picture after short pause for settings to update
pause(5000)
turnedOffLights?.off()
}
}

def imageDescriptionHandleBurst(value) {
def inBurst = atomicState.inBurst?.value ?: false
log.debug "imageDescriptionHandleBurst - inBurst = $inBurst"
def burstComplete = true
if (inBurst) {
def currentBurstImages = atomicState.addAndGet('currentBurstImages', [value: value, url: imageUrl(value)])
log.debug "currentBurstImages - " + currentBurstImages
int burstCount = currentBurstImages.size()
log.debug "imageDescriptionHandleBurst - burstCount = $burstCount"

if (burstCount < 5) {
burstComplete = false
}
} else {
atomicState.currentBurstImages = [[value: value, url: imageUrl(value)]]
}
burstComplete
}

def imageUrl(value) {
if (value.startsWith('http')) {
return value
} else {
def key
def bucket
def splitVal = value.split(":")
if (splitVal.size() == 2) {
bucket = splitVal[0]?.trim()
key = splitVal[1]?.trim()
}
else if (splitVal.size() > 2) {
bucket = splitVal[0]?.trim()
key = splitVal[1..-1].join(":")?.trim()
}

return "/api/s3/$bucket/$key"
}
}

def setMotionDevices(motions) {
log.debug "setMotionDevices($motions)"
app.updateSetting("motionSensors", motions*.id)
}

def setCamera(device) {
log.debug "setCameraDevice($device)"
app.updateSetting("camera", device.id)
}

def updateSolutionState(overrideValue = null) {
log.trace "updateSolutionState($motionSensors) - overrideValue:$overrideValue"
if (overrideValue) {
setSolutionState(overrideValue)
parent.updateSolutionSummary(overrideValue)
} else {
def value = getSolutionEventValue()
setSolutionState(value)
parent.updateSolutionSummary()
// Suppress duplicate Recently events if our state hasn't changed
if (value && value != atomicState.value) {
setSolutionEvent(value)
atomicState.value = value
}
}
}

// i.e. Right Now
def setSolutionState(value) {
log.trace "setSolutionState()"
def displayValue = valueLabel(value)
def stateData = getSolutionStateData(value)
log.info "SOLUTION_STATE stateData: $stateData"
sendEvent(linkText:app.label, descriptionText:app.label + " sees " + displayValue, eventType:"SOLUTION_STATE", displayed: false, name: "summary", value:value,
data:stateData, isStateChange: true)
}

def getSolutionStateData(value) {
def cameraCount = camera ? 1 : 0
def motionValues = motionSensors.collect { it.latestValue("motion") }.findAll { it }
def activeCount = motionValues.count { it == "active" }
def inactiveCount = motionValues.count { it == "inactive" }
[activeCount: activeCount, inactiveCount: inactiveCount, cameraCount: cameraCount] + getIconData(value)
}

// i.e. Recently
def setSolutionEvent(value) {
log.trace "setSolutionEvent()"
def displayValue = valueLabel(value)
def descriptionText = "$app.label saw ${displayValue}"
sendEvent(linkText:app.label, descriptionText:descriptionText, eventType:"SOLUTION_EVENT", displayed: false, name:"motion", value:value, data: getIconData(value))
}

private getIconData(value) {
def iconData = [:]
// Get state icon and backgroundColor
if (value == "active" ) {
iconData += [icon: "st.motion.motion.active", backgroundColor: "#53a7c0"]
} else if (value == "inactive") {
iconData += [icon: "st.motion.motion.inactive", backgroundColor: "#e8e9eb"]
} else if (value == "no activity") {
iconData += [icon: "st.camera.take-photo", backgroundColor: "#e8e9eb"]
} else {
iconData += [icon: "st.camera.take-photo", value:"", backgroundColor:"#53a7c0"]
}
if (camera) {
iconData += [name: "take"]
}
iconData
}

private getSolutionEventValue() {
def value

if (motionSensors) {
if (!atomicState.value) {
value = "inactive"
} else {
value = motionSensors?.find{it.latestValue("motion") == "active"} ? "active" : "inactive"
}
} else {
value = "no activity"
}
value
}

private valueLabel(value) {
if (value == "active") {
"motion"
} else if (value == "inactive") {
"no motion"
} else {
value
}
}

def takePicture() {
updateSolutionState("camera activity")
atomicState.inBurst = false
atomicState.currentBurstImages = []
camera.take()
}