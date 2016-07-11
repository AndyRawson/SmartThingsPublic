/**
* Motion & Cameras
*
* Author: SmartThings
* Date: 2013-10-04
*/
definition(
name: "Motion",
namespace: "SmartSolutions/Motion2",
author: "SmartThings",
description: "Your Home & Family dashboard solution.",
category: "SmartSolutions",
iconUrl: "https://s3.amazonaws.com/smartapp-icons/Solution/areas.png",
iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Solution/areas@2x.png"
)

preferences {
page(name: "root", title: "Keep an eye on your home from anywhere", install: true) {
section {
app "person", "SmartSolutions/Motion2", "New Area2", title: "Add an area or room2",
page: "createArea", childTitle: "", childPage: "areaDetail", install: true, multiple: true
}
}
page(name: "useCaseRoot", title: "Configure or Add an Area", install: true) {
section {
app "areaUseCase", "SmartSolutions/Motion2", "New Area2", title: "Add an area or room2", page: "createArea", childTitle: "", install: true, multiple: true
}
}
}

solutionSummary(title: true ? "Motion & Cameras" : "Motion")

cards {
card(name:"Right Now", sortable:false) {
tiles {
stateTile { }
}
}
card("Recently") {
tiles {
eventTile(eventTypes: true ? ["SOLUTION_EVENT", "IMAGE"] : ["SOLUTION_EVENT"]) { }
}
}

if(true) {
// TODO: Only display these cards if a camera is configured
// if (childApps*.camera) {
card("Recent Camera Activity") {
tiles {
if (true) {
smartAppImageTile { }
} else {
eventTile(eventTypes: ["IMAGE"], max: 1) { }
}
}
}
// }
}

if(true) {
card(name:"Shortcuts", sortable: iosClient("1.5.4.beta5")) {
tiles {
smartAppGroupTile {
appState "take", label: 'Take', action: "takePicture", icon: "st.camera.take-photo", backgroundColor: "#e8e9eb", nextState: "taking"
appState "taking", label: 'Taking', icon: "st.camera.take-photo", backgroundColor: "#53a7c0"
}
}
}
}

if (androidClient() || iosClient("1.5.5.RC1")) {
card() {
tiles {
exploreTile(url: "https://shop.smartthings.com/#/taxons/things/motion",
imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-3.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-3@2x.png") {}
}
}
} else {
// TODO: Remove once 3rd explore card is stored on the phone for iOS and no one is on < 1.5.5.RC1
card() {
tiles {
exploreTile(url: "https://shop.smartthings.com/#/taxons/things/motion",
imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-2.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-2@2x.png") {}
}
}
}

if (iosClient("1.5.4") || androidClient("1.5.0")) {
card(displayConfigured: false, displayLoggedOut: false) {
tiles {
exploreTile(imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-0.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-0@2x.png") {}
}
}
}

if(true) {
card(displayConfigured: false) {
tiles {
exploreTile(imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-1.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-1@2x.png") {}
}
}
}

if (androidClient() || iosClient("1.5.5.RC1")) {
card(displayConfigured: false) {
tiles {
exploreTile(imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-2.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-2@2x.png") {}
}
}
card(displayConfigured: false) {
tiles {
exploreTile(url: "https://shop.smartthings.com/#/taxons/things/motion",
imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-3.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-3@2x.png") {}
}
}
} else {
// TODO: Remove once 3rd explore card is stored on the phone for iOS and no one is on < 1.5.5.RC1
card(displayConfigured: false) {
tiles {
exploreTile(url: "https://shop.smartthings.com/#/taxons/things/motion",
imageUrl: "http://cdn.explore-cards.smartthings.com/motion-cameras-2.png",
imageX2Url: "http://cdn.explore-cards.smartthings.com/motion-cameras-2@2x.png") {}
}
}
}

}

def installed() {
log.debug "Installed with settings: ${settings}"
}

def updated() {
log.debug "Updated with settings: ${settings}"

unsubscribe()
updateSolutionSummary()
}

def childUninstalled() {
updateSolutionSummary()
}

def updateSolutionSummary(overrideValue = null) {
def summaryData = []
if (overrideValue) {
summaryData << ["icon":"indicator-dot-orange","value":overrideValue.capitalize()]
} else {
def activeCount = 0
def notActiveCount = 0
def motionCount = 0
def cameraCount = 0

childApps.each {child ->
def states = getChildSolutionStates(child)
log.debug "$child.label states: ${states.collect{[(it.name):it.value]}}"
if (states && states[0].data) {
def data = states[0].jsonData
if (data.activeCount) {
activeCount++
} else if (data.inactiveCount) {
notActiveCount++
} else if (data.cameraCount) {
cameraCount++
}
motionCount += (data.activeCount ?: 0) + (data.inactiveCount ?: 0)
}
}

log.trace "activeCount: $activeCount, notActiveCount: $notActiveCount"
log.trace "motionCount: $motionCount, cameraCount: $cameraCount"

if (motionCount) {
if (activeCount == 0) {
summaryData << ["icon":"indicator-dot-green","iconColor":"#878787","default":"true","value":"No motion"]
} else {
if (notActiveCount == 0) {
summaryData << ["icon":"indicator-dot-orange","iconColor":"#53a7c0","default":"true","value":"All have motion"]
} else {
summaryData << ["icon":"indicator-dot-orange","iconColor":"#53a7c0","default":"true","value":"$activeCount motion"]
summaryData << ["icon":"indicator-dot-green","iconColor":"#878787","value":"$notActiveCount no motion"]
}
}
} else {
summaryData << ["icon":"indicator-dot-green","iconColor":"#878787","default":"true","value":"No activity"]
}
}
log.info "SOLUTION_SUMMARY data: $summaryData"
def summaryDataJSON = groovy.json.JsonOutput.toJson(summaryData)
sendEvent(linkText:app.label, descriptionText:app.label + " updating summary", eventType:"SOLUTION_SUMMARY",
name: "summary", value: summaryData*.value?.join(", "), data: summaryDataJSON, isStateChange: true, displayed: false)
}
