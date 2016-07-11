/**
 *  Security - Wizard for creating advance monitoring rules
 *
 *  Copyright 2015 SmartThings
 *
 *  Author: Juan Risso - juan@smartthings.com
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

definition(
		name: "Security",
		namespace: "SmartSolutionsV2/SmartHomeMonitor",
		parent: "SmartSolutionsV2/SmartHomeMonitor:Smart Home Monitor",
		author: "SmartThings",
		description: "Intrusion alarm",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@2x.png"
){
	appSetting "s3Url"
}

preferences {
	page(name: "awayPage", title: "Armed/Away Intrusion Sensors", install: false, uninstall: true, nextPage: "stayPage")
	page(name: "stayPage", title: "Armed/Stay Intrusion Sensors", install: false, uninstall: true, nextPage: "notificationsPage")
	page(name: "notificationsPage", title: "Configure intrusion alarm notifications.", install: true, uninstall: true)

	page(name: "audio", title: "Audio Notifications")

	page(name: "timeIntervalInput", title: "Only during a certain time") {
		section {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
		}
	}

	page(name: "notifications", title: "Text & Push Notifications") {
		section("Users & Notifications") {
			input("recipients", "contact", title: "Select Users & Notifications", required: false, multiple: true) {
				input "pushNotification", "bool", title: "Send push notifications", required: false, defaultValue: true
				input "phone1", "phone", title: "Phone number?", required: false
			}
		}
	}

	page(name: "cameras", title: "Capture video from these cameras") {
		section {
			paragraph "(This is a premium feature of Smart Home Security. Enjoy it FREE until the end of 2015!)"
		}
		section {
			input "cameraDevices", "capability.videoCapture", title: "Select cameras", required: false, multiple: true, description: "", submitOnChange: true
			input "clipLength", "enum", title: "Recorded clip length", defaultValue: "60000", options: [["30000":"30 Seconds"], ["60000":"1 Minute"], ["120000":"2 Minutes"]]
		}
	}
}

def awayPage() {
	dynamicPage(name: "awayPage") {
		section("Select sensors to monitor when the home is unoccupied") {
			image "https://misc-bob-stuff.s3.amazonaws.com/homewatchs/images/arm-green.png"
			input "toggle1", "bool", title: "Use every open/close and motion sensor", defaultValue: "true", required: "false", submitOnChange: true
		}
		if (toggle1 == false)  {
			section("Select Sensors") {
				input "contactSensors", "capability.contactSensor", title: "Open/closed sensors", multiple: true, required: false
				input "motionSensors", "capability.motionSensor", title: "Motion sensors", multiple: true, required: false
			}
		}
	}
}

def stayPage() {
	// TODO - also remove stay sensors that were removed from away
	def awayContacts = deviceIds(contactSensors)
	def oldStayContacts = deviceIds(stayContactSensors)
	def newContacts = awayContacts - (state.awayContacts ?: [])
	def stayContacts = oldStayContacts + newContacts

	if (newContacts) {
		app.updateSettings(stayContactSensors: stayContacts)
	}

	state.awayContacts = awayContacts

	dynamicPage(name: "stayPage") {
		section("Select sensors to be monitored when home is occupied.") {
			image "https://misc-bob-stuff.s3.amazonaws.com/homewatchs/images/stay-green.png"
			input "toggle2", "bool", title: "Use every open/close sensor but no motion sensors", defaultValue: "true", required: "false", submitOnChange: true
		}
		if (toggle2 == false)  {
			section("Select Sensors") {
				input "stayContactSensors", "capability.contactSensor", title: "Open/closed sensors", multiple: true, required: false
				input "stayMotionSensors", "capability.motionSensor", title: "Motion sensors", multiple: true, required: false
			}
		}
	}
}

private deviceIds(List items) {
	items.collect{deviceId(it)}.findAll{it}
}
private deviceIds(items) {
	if (items) {
		[deviceId(items)]
	}
	else {
		[]
	}
}
private deviceId(String id) {
	id
}
private deviceId(device) {
	device.id
}

def notificationsPage() {
	dynamicPage(name: "notificationsPage") {
		section("Alarm & Notifications") {
			def notificationsConfigured = location.contactBookEnabled ? (recipients) : (pushNotification != false || phone1)
			href "notifications", title: "Text & Push Notifications", description: "", state: notificationsConfigured ? "complete" : "incomplete"
			if (userHasTag("internal")) {
				href "audio", title: "Audio Notifications (INTERNAL ONLY)", description: "", state: audioSet ? "complete" : "incomplete"
			}
			app "sirenApp", "SmartSolutionsV2/SmartHomeMonitor/Behaviors", "Alert with Sirens", title: "Alert with Sirens"
			app "lightsApp", "SmartSolutionsV2/SmartHomeMonitor/Behaviors", "Alert with Lights", title: "Alert with Lights"
			//app "unlockApp", "SmartSolutionsV2/SmartHomeMonitor/Behaviors", "Unlock Doors", title: "Unlock doors"
			//app "valveApp", "SmartSolutionsV2/SmartHomeMonitor/Behaviors", "Close Valves", title: "Close Valves"
		}

		section("Capture video from these cameras") {
			href "cameras", title: "Select Cameras", description: cameraDevices ? "${cameraDevices.displayName.join(',', 'and')}" : "", state: cameraDevices ? "complete" : "incomplete"
		}
	}
}

def audio(submited) {
	dynamicPage(name: "audio") {
		section {
			input "audioDevices", "capability.musicPlayer", title: "Select Audio Player", required: false, multiple: true
		}
		section{
			input "actionType", "enum", title: "Notification", required: true, defaultValue: "Alarm", options: [
					"Default Message",
					"Custom Message",
					"Alarm",
					"Bell 1",
					"Bell 2",
					"Dogs Barking",
					"Fire Alarm",
					"The mail has arrived",
					"A door opened",
					"There is motion",
					"Smartthings detected a flood",
					"Smartthings detected smoke",
					"Someone is arriving",
					"Piano",
					"Lightsaber"], submitOnChange: true
			if (actionType == "Custom Message") {
				input "audioMessage","text",title:"Play this message", required:false, multiple: false
			}
		}
		section("More options", hideable: true, hidden: true) {
			input "resumePlaying", "bool", title: "Resume currently playing music after notification", required: false, defaultValue: true
			href  "chooseTrack", title: "Or play this music or radio station", description: song ? state.selectedSong?.station : "Tap to set", state: song ? "complete" : "incomplete"
			input "volume", "number", title: "Temporarily change volume", description: "0-100%", required: false
		}
	}
}

def settingsPage() {
	dynamicPage(name: "settingsPage") {
		section("Locks") {
			input "lockOnArming", "capability.lock", title: "Lock doors when armed", required: false, multiple: true, description: ""
			input "unlockOnDisarm", "capability.lock", title: "Unlock doors when disarmed", required: false, multiple: true, description: ""
		}
		section("Armed/Away Settings") {
			paragraph "These settings apply when the system is armed and you are away from home."
			input "awayAlarmDelay", "enum",
					options: [[1:"None"],[5:"5 seconds"],[15:"15 seconds"],[30:"30 seconds"],[60:"60 seconds"], [300:"5 minutes"], [600:"10 minutes"]],
					title: "Delay between trigger and audible alarms", defaultValue: 1, required: false

			input "awayAutoReset", "enum", title: "Auto reset after being triggered", defaultValue: -1, required: false,
					options: [[(-1):"Never"], [5: "5 minutes"], [10: "10 minutes"], [30: "30 minutes"], [60: "60 minutes"]]

			if (audioDevices) {
				input "awaySoundsArming", "boolean", title: "Play notification on music systems when armed", defaultValue: true, required: false
				input "awaySoundsTriggered", "boolean", title: "Play warning on music systems when triggered", defaultValue: true, required: false
			}
		}
		section("Armed/Stay Settings") {
			paragraph "These settings apply when the system is armed and you are at home."
			input "stayAlarmDelay", "enum",
					options: [[1:"None"],[5:"5 seconds"],[15:"15 seconds"],[30:"30 seconds"],[60:"60 seconds"], [300:"5 minutes"], [600:"10 minutes"]],
					title: "Delay between trigger and audible alarms", defaultValue: 1, required: false

			input "stayAutoReset", "enum", title: "Auto reset after being triggered", defaultValue: -1, required: false,
					options: [[(-1):"Never"], [5: "5 minutes"], [10: "10 minutes"], [30: "30 minutes"], [60: "60 minutes"]]

			if (audioDevices) {
				input "staySoundsArming", "boolean", title: "Play notification on music systems when armed", defaultValue: true, required: false
				input "staySoundsTriggered", "boolean", title: "Play warning on music systems when triggered", defaultValue: true, required: false
			}
		}
		if (audioDevices) {
			section("Off Settings") {
				input "offSoundsDisarmed", "boolean", title: "Play notification on music systems when turned off", defaultValue: true, required: false
			}
		}
	}
}

def getAlarmDelay() {
	(app.currentState("systemStatus")?.value == "away" ? awayAlarmDelay : stayAlarmDelay) as Integer
}

def getPlaySoundsArming() {
	app.currentState("systemStatus")?.value == "away" ? awaySoundsArming : staySoundsArming
}

def getPlaySoundsTriggered() {
	app.currentState("systemStatus")?.value == "away" ? awaySoundsTriggered : staySoundsTriggered
}

def getPerimeterSensorState() {
	toggle1 != false || contactSensors || motionSensors ? "complete" : "incomplete"
}

def getInteriorSensorState() {
	toggle2 != false || interiorContactSensors || interiorMotionSensors ? "complete" : "incomplete"
}

mappings {
	path("/openCloseSensors") {
		action: [
				GET: "openCloseSensors"
		]
	}
	path("/openCloseSensorsData") {
		action: [
				GET: "openCloseSensorsData"
		]
	}
	path("/motionSensorsPage") {
		action: [
				GET: "motionSensors"
		]
	}
	path("/motionSensorsData") {
		action: [
				GET: "motionSensorsData"
		]
	}
	path("/rightNow") {
		action: [
				GET: "rightNow"
		]
	}
	path("/rightNowData") {
		action: [
				GET: "rightNowData"
		]
	}
}

private subscribeChildApps(contactSensors, motionSensors) {

	def behaviorApps = getChildApps()
	if (behaviorApps) {
		behaviorApps.each {a ->
			if (behaviorEnabled[a.name]) {
				log.trace "adding subscriptions to $a.name"
				//a.subscribeToTriggers([[devices: contactSensors, dataName: "contact.open"],[devices: motionSensors, dataName: "motion.active"]])
				//a.unsubscribe("primaryActionHandler")
				//a.subscribe(contactSensors, "contact.open", "primaryActionHandler")
				//a.subscribe(motionSensors, "motion.active", "primaryActionHandler")
				// TODO subscribe to messages from children when it's possible
				//subscribe(a, incidentMessageHandler)
				//a.update(dirty: UUID.randomUUID().toString())
				a.update(contactSensors: contactSensors*.id, motionSensors: motionSensors*.id)
			}
			else {
				log.trace "skipping unconfigured app $a.name"
			}
			//a.update()
		}
	}
}

private unsubscribeChildApps() {
	log.trace "unsubscribeChildApps()"
	def behaviorApps = getChildApps()
	if (behaviorApps) {
		behaviorApps.each {a ->
			if (behaviorEnabled[a.name]) {
				log.trace "removing subscriptions to $a.name"
				a.update(contactSensors: [], motionSensors: [])
			}
			else {
				log.trace "skipping unconfigured app $a.name"
			}
		}
	}
	log.trace "/unsubscribeChildApps"
}

def disarm(evt) {
	if (evt.value != app.currentState("systemStatus")?.value) {
		log.debug "Disarmed -> $evt - ${app.currentState("systemStatus")?.value}"

		sendEvent(name: "systemStatus", value: "off")
		if (offSoundsDisarmed) {
			playDisarmMessage()
		}

		parent.setSummary(systemStatus: "off")
		//parent.intrusionModeChange()
		//setSummary("Off", "st.security.alarm.alarm", "#ffffff")

		log.trace "unscheduling delayed alarms"
		unschedule("soundAlarm")

		log.trace "unsubscribing this app"
		unsubscribe("intrusionHandler")

		try {
			unsubscribeChildApps()
		}
		catch (Exception e) {
			log.error "$e"
		}

		log.trace "/disarm"
	}
}

def armStay(evt) {
	if (evt.value != app.currentState("systemStatus")?.value) {
		log.debug "Armed/stay -> $evt - ${app.currentState("systemStatus")?.value}"

		unsubscribe("intrusionHandler")

		if (toggle2 != false) {
			subscribe(location, "contact.open", intrusionHandler)
			subscribeChildApps(openCloseDevices, [])
		} else {
			subscribe(stayContactSensors, "contact.open", intrusionHandler)
			subscribe(stayMotionSensors, "motion.active", intrusionHandler)
			subscribeChildApps(stayContactSensors, stayMotionSensors)
		}

		sendEvent(name: "systemStatus", value: "stay")
		if (staySoundsArming) {
			playArmingMessage("Stay")
		}

		parent.setSummary(systemStatus: "stay")
		//parent.intrusionModeChange()
		//setSummary("Armed/stay", "st.security.alarm.alarm", "#90ee90")
		//log.debug "stay mode"
	}
}

def armAway(evt) {
	if (evt.value != app.currentState("systemStatus")?.value) {
		log.debug "Armed/away -> $evt - ${app.currentState("systemStatus")?.value}"

		unsubscribe("intrusionHandler")

		if (toggle1 != false) {
			subscribe(location, "contact.open", intrusionHandler)
			subscribe(location, "motion.active", intrusionHandler)
			subscribeChildApps(openCloseDevices, motionDevices)
		} else {
			subscribe(contactSensors, "contact.open", intrusionHandler)
			subscribe(motionSensors, "motion.active", intrusionHandler)
			subscribeChildApps(contactSensors, motionSensors)
		}
		sendEvent(name: "systemStatus", value: "away")
		if (awaySoundsArming) {
			playArmingMessage("Away")
		}

		parent.setSummary(systemStatus: "away")
		//parent.intrusionModeChange()
		//setSummary("Armed/away", "st.security.alarm.alarm", "#79b821")
		//log.debug "away mode"
	}
}

def installed() {
	initialize()
	createAccessToken()
	sendEvent(name: "systemStatus", value: "off")
	sendLocationEvent(name: "alarmSystemStatus", value: "off")
	parent.setSummary(alarmState: clear)
}

/**
 * Called on app uninstalled
 */
def uninstalled() {
	if(getActiveIncident() != null){
		dismissIncident()
	}
	sendLocationEvent(name: "alarmSystemStatus", value: "unconfigured")
}

def updated() {
	// TODO - legacy cleanup, remove after all apps are updated
	state.remove("clipCache")
	state.remove("clipData")
	state.remove("clipStartTimes")
	state.remove("clipStopTimes")
	state.remove("triggeredDevices")
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(location, "alarmSystemStatus.off", disarm)
	subscribe(location, "alarmSystemStatus.stay", armStay)
	subscribe(location, "alarmSystemStatus.away", armAway)

	def status = app.currentState("systemStatus")?.value

	switch (status) {
		case "away":
			// TODO -- ADD CHILD SUBSCRIPTIONS!
			if (toggle1 != false) {
				subscribe(location, "contact.open", intrusionHandler)
				subscribe(location, "motion.active", intrusionHandler)
				subscribeChildApps(openCloseDevices, motionDevices)
			} else {
				subscribe(contactSensors, "contact.open", intrusionHandler)
				subscribe(motionSensors, "motion.active", intrusionHandler)
				subscribeChildApps(contactSensors, motionSensors)
			}
			break
		case "stay":
			if (toggle2 != false) {
				subscribe(location, "contact.open", intrusionHandler)
				subscribeChildApps(openCloseDevices, [])
			} else {
				subscribe(stayContactSensors, "contact.open", intrusionHandler)
				subscribe(stayMotionSensors, "motion.active", intrusionHandler)
				subscribeChildApps(stayContactSensors, stayMotionSensors)
			}
			break
		default:
			unsubscribeChildApps()
			break
	}


	if (toggle1 != false) {
		// For a live event feed on Right Now card
		subscribe(location, "contact", motionOrContactHandler)
		subscribe(location, "motion", motionOrContactHandler)
	} else {
		// For a live event feed on Right Now card
		subscribe(contactSensors, "contact", motionOrContactHandler)
		subscribe(motionSensors, "motion", motionOrContactHandler)
	}

	childApps.each {
		if (!it.label?.startsWith(app.name)) {
			it.updateLabel("$app.name/$it.label")
		}
	}
}

// TODO - for migration only
def clipHandler(evt) {
	parent.clipHandler(evt)
}

def streamHandler(evt) {
	parent.streamHandler(evt)
}
// TODO - END migration

def motionOrContactHandler(evt){
	sendEvent(name: "motionOrContactUpdated", value: 0, isStateChange: true, displayed: false)
}

def updateBehaviorStatus(String name, Boolean enabled) {
	behaviorEnabled[name] = enabled
}

def findCamera(deviceId) {
	cameraDevices.find{it.id == deviceId}
}

def intrusionHandler(evt) {
	log.trace "intrusionHandler($evt.name: $evt.value)"

	def intrusionSource = evt.linkText
	def delay = alarmDelay as Integer

	state.intrusionSource = intrusionSource
	state.intrusionSourceType = evt.name
	state.triggerScheduled = true

	def incident = activeIncident
	if (!incident) {
		state.alarmTriggerTime = now()

		if (delay <= 1) {
			log.trace "No delay"
			startIncident("$intrusionSource intrusion detected.", false)
			soundAlarm()
		}
		else {
			log.trace "Delay = $delay"
			def msg = "$intrusionSource intrusion detected. Alarm will sound in $delay ${delay == 1 ? 'second' : 'seconds'}"
			startIncident(msg, true)
			runIn(delay, soundAlarm)

			sirens?.strobe()
			if (playSoundsTriggered) {
				playAlarmPendingMessage()
			}
		}

		// records last trigger from each device
		sendEvent(name: evt.deviceId, value: now())
	}
	else if (!triggeredRecently(evt.deviceId)) {
		def descriptionText = evt.name == "contact" ? "${intrusionSource} opened" : "${intrusionSource} detected motion"
		addMessage(descriptionText, evt.name)
		captureClips(incident)

		// records last trigger from each device
		sendEvent(name: evt.deviceId, value: now())
	}
}

private Boolean triggeredRecently(deviceId) {
	def lastTrigger = app.currentState(deviceId)?.longValue
	def result = lastTrigger && now() - lastTrigger < 60000
	log.trace "triggeredRecently($deviceId): $result, lastTrigger: $lastTrigger"
	result
}

def startIncident(descriptionText, delayed) {
	def status = "alarm"
	sendEvent(name: "intrusion", value: status)
	parent.setSummary(alarmState: status)

	// TODO - remove when we can use runPeriodically
	runIn(3600, openIncidentReminder)
	state.intrusionDescriptionText = descriptionText

	def incident = createOrGetActiveIncident("Intrusion Detected!", descriptionText, "intrusion")
	parent.newIncident("intrusion")
	captureClips(incident)

	sendMessage(descriptionText)

	// TODO - not needed if could subscribe to child events
	def behaviors = getChildApps()
	//log.trace "behaviors installed: ${behaviors.size()}"
	//log.trace "behaviors enabled: ${behaviorEnabled}"
	if (behaviorEnabled["Alert with Sirens"]) {
		addMessage("Siren was triggered", "alarm")
	}
	if (behaviorEnabled["Audio Notifications"]) {
		addMessage("Audio notification was triggered", "audio")
	}
	if (behaviorEnabled["Alert with Lights"]) {
		addMessage("Light alert was triggered", "light")
	}
}

def soundAlarm() {
	//log.trace "Sounding alarm"
	state.alarmSounding = true

	if (audioSet) {
		sendAudioMessage()
	}
}

def playArmingMessage(systemStatus) {
	def msg = "Alarm system is now armed in $systemStatus mode"
	log.debug msg
	audioDevices?.playText(msg)
}

def playDisarmMessage() {
	def msg = "Alarm system is now disarmed"
	log.debug msg
	audioDevices?.playText(msg)
}

def playAlarmPendingMessage() {
	if (state.triggerScheduled) {
		def timeRemaining = Math.round(((1000*alarmDelay) + state.alarmTriggerTime - now())/1000)
		def msg = "${state.intrusionSource} intrusion detected. Alarm will sound in $timeRemaining seconds"
		log.debug msg
		try {
			def sound = textToSpeech(msg)
			def duration = sound.duration as Integer
			//log.trace "sound = $sound"
		}
		catch (Exception e) {
			log.error "exception: $e converting text to speech"
		}
		audioDevices?.playTrack(sound.uri)
		if (timeRemaining > duration + 3) {
			runIn(duration + 2, playAlarmPendingMessage)
		}
	}
}

def playAlarmMessage() {
	if (state.alarmSounding) {
		//log.debug "Alarm sounding"
		audioDevices?.playTrack("http://s3.amazonaws.com/smartapp-media/sonos/alarm.mp3")
		runIn(18, playAlarmMessage)
	}
}

def getAlarmStatus() {
	//log.trace "state.alarmSounding: $state.alarmSounding"
	//log.trace "state.triggerScheduled $state.triggerScheduled"
	if (state.alarmSounding) {
		"active"
	}
	else if (state.triggerScheduled) {
		"pending"
	}
	else {
		"off"
	}
}

private getAlarmVolume() {
	50
}

private getWarningVolume() {
	30
}

def getTriggeredSensors() {
	return state.intrusionSource
}

def getIsMuteable() {
	def audio = audioDevices?.size() > 0
	def sirens = behaviorEnabled."Alert with Sirens" ?: false
	audio || sirens
}

// Notifications Implementation - Begining
def getMessageSet() {
	recipients || phone1 || pushNotification
}

Boolean getAudioSet() {
	audioDevices
}

Boolean getCameraSet() {
	cameraDevices
}

def addMessage(msg, sourceType, currentIncident = null){
	def incident = currentIncident ?: getActiveIncident()
	def incidentMessage = incident.addNotificationMessage(msg, sourceType)
	if(sourceType != "clear"){
		parent.newMessage("intrusion")
	}
	incidentMessage
}

def dismissSirens() {
	getChildApps()*.mute()
}

def dismissIncident() {
	log.debug "Dismissing Intrusion Incident"
	addMessage("Incident dismissed", "clear")

	initializeCameras()

	def incident = getActiveIncident()
	incident.close("Closed")

	audioDevices*.stop()

	def status = "clear"
	sendEvent(name: "intrusion", value: status)
	parent.setSummary(alarmState: status)

	state.triggerScheduled = false
	state.armingStartTime = null
	state.alarmSounding = false
	state.alarmTriggerTime = null

	unschedule("soundAlarm")
	unschedule("openIncidentReminder")

	getChildApps()*.reset()

	logUserPhrase("Alarm cleared/canceled")
}

/**
 * Resets trigger status of behavior apps
 */
def resetTrigger() {
	log.trace "resetTrigger()"
	childApps*.resetTrigger()
}

def sendAudioMessage() {
	addMessage("Playing alarm message","audio")
	if (actionType == "Alarm") {
		playAlarmMessage()
	} else {
		def message = audioMessage ? audioMessage : "$state.intrusionSource intrusion detected."
		//log.trace "Playing Message: $message"
		loadText(message)
		if (state.sound) {
			audioDevices.each {
				if (song) {
					it.playSoundAndTrack(state.sound.uri, state.sound.duration, state.selectedSong, volume)
				} else if (resumePlaying) {
					it.playTrackAndResume(state.sound.uri, state.sound.duration, volume)
				} else {
					it.playTrackAndRestore(state.sound.uri, state.sound.duration, volume)
				}
			}
		}
	}
}

private loadText(message) {
	switch (actionType) {
		case "Bell 1":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/bell1.mp3", duration: "10"]
			break;
		case "Bell 2":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/bell2.mp3", duration: "10"]
			break;
		case "Dogs Barking":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/dogs.mp3", duration: "10"]
			break;
		case "Fire Alarm":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/alarm.mp3", duration: "17"]
			break;
		case "The mail has arrived":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/the+mail+has+arrived.mp3", duration: "1"]
			break;
		case "A door opened":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/a+door+opened.mp3", duration: "1"]
			break;
		case "There is motion":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/there+is+motion.mp3", duration: "1"]
			break;
		case "Smartthings detected a flood":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/smartthings+detected+a+flood.mp3", duration: "2"]
			break;
		case "Smartthings detected smoke":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/smartthings+detected+smoke.mp3", duration: "1"]
			break;
		case "Someone is arriving":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/someone+is+arriving.mp3", duration: "1"]
			break;
		case "Piano":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/piano2.mp3", duration: "10"]
			break;
		case "Lightsaber":
			state.sound = [uri: "http://s3.amazonaws.com/smartapp-media/sonos/lightsaber.mp3", duration: "10"]
			break;
		default:
			try {
				if (message) {
					state.sound = textToSpeech(message instanceof List ? message[0] : message)
				}
				else {
					state.sound = textToSpeech("You selected the custom message option but did not enter a message in the $app.label Smart App")
				}
			}
			catch (Exception e) {
				log.error "exception: $e converting text to speech"
			}
			break;
	}
}

def getOpenCloseDevices() {
	if (toggle1 != false || toggle2 != false) {
		findAllDevicesByCapability("contactSensor").collect{new physicalgraph.app.DeviceWrapper(
				device: it.device,
				installedSmartApp: it.installedSmartApp,
				eventSvc: app.eventService,
				execSvc: it.execSvc,
				scheduledEventService: it.scheduledEventService)}
	}
	else {
		contactSensors ?: []
	}
}

def getMotionDevices() {
	if (toggle1 != false) {
		findAllDevicesByCapability("motionSensor").collect{new physicalgraph.app.DeviceWrapper(
				device: it.device,
				installedSmartApp: it.installedSmartApp,
				eventSvc: app.eventService,
				execSvc: it.execSvc,
				scheduledEventService: it.scheduledEventService)}
	}
	else {
		motionSensors ?: []
	}
}

// other cards
def rightNowData() {
	[html: openCloseSensorsData().html + "\
" + motionSensorsData().html]
}

def openCloseSensorsData() {
	//log.trace "openCloseSensorsData()"
	getOpenCloseDevices()?.size()
	def sb = new StringBuilder()
	getOpenCloseDevices()?.sort{it.displayName}.each {
		def value = it.currentValue("contact") ?: 0
		sb << """<tr id="${it.id}">
			<td class="st-icon-cell"><span class="st-icon st-icon-${value}">${iconImage(value)}<\u002fspan><\u002ftd>
			<td class="st-name">${it.displayName} is ${value}<\u002ftd>
		<\u002ftr>
		"""
	}
	[html: sb.toString()]
}

def motionSensorsData() {
	//log.trace "motionSensorsData()"
	getMotionDevices()?.size()
	def sb = new StringBuilder()
	getMotionDevices()?.sort{it.displayName}.each {
		def value = it.currentValue("motion") ?: 0
		sb << """<tr id="${it.id}">
			<td class="st-icon-cell"><span class="st-icon st-icon-${value}">${iconImage(value)}<\u002fspan><\u002ftd>
			<td class="st-name">${it.displayName} is ${value}<\u002ftd>
		<\u002ftr>
		"""
	}
	[html: sb.toString()]
}

private iconImage(value) {
	if (value == "open") {
		"""<img src="https://s3.amazonaws.com/smartthings-device-icons/contact/contact/open.png"/>"""
	}
	else if (value == "closed") {
		"""<img src="https://s3.amazonaws.com/smartthings-device-icons/contact/contact/closed.png"/>"""
	}
	else if (value == "active") {
		"""<img src="https://s3.amazonaws.com/smartthings-device-icons/motion/motion/active.png"/>"""
	}
	else if (value == "inactive") {
		"""<img src="https://s3.amazonaws.com/smartthings-device-icons/motion/motion/inactive.png"/>"""
	}
	else {
		""
	}
}

def rightNow() {
	//log.debug "rightNow()"
	devicePage("Right Now", "rightNowData")
}


def devicePage(String title, String callback) {
	renderHTML(title, true) {
		head {
			"""
			<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js"><\u002fscript>
			<script src="${resource('javascript/jquery-ui.min.js')}"><\u002fscript>
			<script src="${resource('javascript/touchpunch.js')}"><\u002fscript>
			<script src="https://cdnjs.cloudflare.com/ajax/libs/handlebars.js/3.0.3/handlebars.min.js"><\u002fscript>

			<script>
				function eventReceived(evt) {
					APP.eventReceiver(evt);
				}
			<\u002fscript>
			<style type="text/css">
				html, body{
					font-family: "Helvetica";
					color: #666;
				}
				h1 {
					font-size: 16px;
					margin-top: 12px;
				}
				table {
					margin: 0;
					padding: 0;
				}
				td {
					font-size: 14px;
					padding: 0 0 8px 0;
				}

				.st-icon-cell{
					vertical-align: middle;
				}

				.st-icon{
					height:32px;
					width:32px;
					display:inline-block;
					vertical-align: middle;
					margin-left: 3px;
					margin-right: 5px;
				}

				.st-name {
					font-weight: bold;
					vertical-align: middle;
				}

				.st-value {
					vertical-align: middle;
					text-align: right;
				}

				.st-icon img {
					margin: -6px 0 0 -10px;
					width: 52px;
					height: 52px;
				}

				.st-icon-closed{
					border-radius: 16px;
					background: #79b821;
				}

				.st-icon-open{
					border-radius: 16px;
					background: #ffa81e;
				}

				.st-icon-active{
					border-radius: 16px;
					background: #53a7c0;
				}

				.st-icon-inactive{
					border-radius: 16px;
					background: #dddddd;
				}

			<\u002fstyle>
		"""
		}
		body {
			"""
			<div class="container">
				<h1>${title}<\u002fh1>
				<table><tbody id="device-list" class="container-border"><\u002ftbody><\u002ftable>
			<\u002fdiv>
			<script>
				var APP = {
					init: function() {
						ST.request("${callback}")
						.success(function(data){
							//console.log(data);
							APP.render(data.html);
							APP.addBindings();
						})
						.GET();

					},

					render: function(html){
						\$("#device-list").html(html);
					},

					addBindings: function(){

					},

					eventReceiver: function(evt){
						switch(evt.name){
							case "motionOrContactUpdated":
								APP.init();
							break;
						}	
					}
				}

				\$(document).ready(function(){
					APP.init();
				});

			<\u002fscript>
		"""
		}
	}
}

private resource(path) {
	//buildResourceUrl(path)
	"$appSettings.s3Url/$path"
}

/****[ include-start: ../includes/shm/incidents.groovy ]****/
/**
 * Sends push and SMS messages
 */
private sendMessage(defaultMessage) {
	def message = defaultMessage //textMessage ? textMessage : defaultMessage
	//log.trace "Sending Message: $message"
	//addMessage("Notifications sent", "contacts")

	if (recipients) {
		sendNotificationToContacts(message, recipients, [view: [name: "SOLUTION", data: [moduleName: "Smart_Home_Monitor", moduleId: app.moduleId, card: 0]]])
	} else {
		//log.trace "sending via $pushNotification & $phone1 book: $message"
		def options = [
				method: (pushNotification != false && phone1) ? "both" : (pushNotification != false ? "push" : "sms"),
				phone: phone1,
				view: [name: "SOLUTION", data: [moduleName: "Smart_Home_Monitor", moduleId: app.moduleId, card: 0]]
		]
		sendNotification(message, options)
	}
}

/**
 * Returns behavior enabled map
 */
private getBehaviorEnabled() {
	if (state.behaviorEnabled == null) {
		state.behaviorEnabled = [:]
	}
	state.behaviorEnabled
}

/**
 * Timer to remind of open incidents
 */
def openIncidentReminder() {
	log.debug "openIncidentReminder"
	def incident = activeIncident
	log.trace "incident: $incident.date"
	if (incident) {
		def value = Math.round((now() - incident.data.time) / 3600000)
		def hours = value > 1 ? "hours" : "hour"
		def msg = "Reminder, $state.intrusionDescriptionText $value $hours ago"
		log.debug msg
		sendMessage(msg)

		// TODO - remove when we can use runPeriodically and not have to do this re-scheduling nonsense
		runIn(3600, openIncidentReminder)
	}
}

/****[ include-end: ../includes/shm/incidents.groovy ]****/

/****[ include-start: ../includes/shm/cameras.groovy ]****/
/**
 * Sets clip started time for each camera to 0
 */
private initializeCameras() {
	cameraDevices.each {
		sendEvent(name: it.id, value: 0)
	}
}

/**
 * Determines if previous clip triggered by this app is completed
 */
private shouldStartClip(deviceId) {
	def result
	def clipEndTime = app.currentState(deviceId)?.longValue
	log.debug "shouldStartClip($deviceId), currentTime: $currentTime, clipEndTime: $clipEndTime"
	if (clipEndTime) {
		def currentTime = now()
		def difference = currentTime - clipEndTime
		result = difference >= 0
		log.trace "shouldStartClip($deviceId) == $result, currentTime: $currentTime, clipEndTime: $clipEndTime, difference: $difference"
	}
	else {
		result = 1
		log.trace "shouldStartClip($deviceId) == $result, clipEndTime: $clipEndTime"
	}
	result
}

/**
 * Initiates clip capture on all cameras
 */
private captureClips(incident) {
	if(cameraDevices){
		def duration = clipLength as Integer ?: 60000
		def pre = duration <= 30000 ? 4999 : 9999
		log.debug "capturing clip, duration: $duration, pre: $pre"

		cameraDevices.each {
			def clipCardinality = shouldStartClip(it.id)
			if (clipCardinality) {
				def incidentMessage = addMessage("$it.displayName ${clipCardinality == 1 ? 'began' : 'resumed'} recording", "camera", incident)
				incidentMessage.captureClip(it, duration, pre)
				sendEvent(name: it.id, value: now() + duration + 1000)
			}
		}

	}
}
/****[ include-end: ../includes/shm/cameras.groovy ]****/
