/**
 *  Smart Home Monitor - Home security and monitoring system
 *
 *  Copyright 2015 SmartThings
 *
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
        name: "Smart Home Monitor",
        namespace: "SmartSolutionsV2/SmartHomeMonitor",
        author: "SmartThings",
        description: "Home security and monitoring system",
        category: "SmartSolutions",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/SafetyAndSecurity/Cat-SafetyAndSecurity@2x.png"
) {
    appSetting "s3Url"
}

preferences {
    page(name: "mainPage", title: "Monitor your home for intrusion, fire, carbon monoxide, leaks, and much more.", install: true, uninstall: app?.id && userHasTag("internal")) {
        section {
            app "intruderStatus", "SmartSolutionsV2/SmartHomeMonitor", "Security", title: "Security", page: "awayPage", multiple: false, install: true, uninstall: true
            app "smokeStatus", "SmartSolutionsV2/SmartHomeMonitor", "Smoke", title: "Smoke", page: "mainPage", multiple: false, install: true
            app "waterStatus", "SmartSolutionsV2/SmartHomeMonitor", "Leaks", title: "Leaks", page: "mainPage", multiple: false, install: true
            app "monitoringStatus", "SmartSolutionsV2/SmartHomeMonitor", "Custom", title: "Custom", page: "mainPage", multiple: false, install: true
        }
    }

    page(name: "accessSchedulesPage", title: "Access Schedules") {
        section("Grant limited access to you home and it's devices") {
            app "door", "SmartSolutionsV2/SmartHomeMonitor", "Access Schedule", title: "Add an access schedule", page: "mainPage", childTitle: "", childPage: "mainPage", install: true, multiple: true
        }
    }
}

cards {
    card(name: "Home", type: "html", action: "home", whitelist:whitelist()) {}

    if (app?.id) {
        def modules = getChildApps()
        def isaId = modules.find{it.name == "Security"}?.id
        if (isaId) {
            card(name: "Right Now", type: "html", action: "rightNow", whitelist:whitelist(), installedSmartAppId: isaId) {}
        }
        if (modules) {
            card(name: "Recently", type: "html", action: "pastIncidents", whitelist:whitelist()) {}
        }
    }
}

mappings {
    path("/incident") {
        action: [
                GET: "incident"
        ]
    }
    path("/home") {
        action: [
                GET: "home"
        ]
    }
    path("/dismissIncident") {
        action: [
                GET: "dismissIncident"
        ]
    }
    path("/dismissSirens") {
        action: [
                GET: "dismissSirens"
        ]
    }
    path("/pastIncidents") {
        action: [
                GET: "pastIncidents"
        ]
    }
    path("/getInitialData") {
        action:[
                GET: "getInitialData"
        ]
    }
    path("/pastIncidentData") {
        action:[
                GET: "pastIncidentData"
        ]
    }
    path("/pastIncidentDataForId") {
        action:[
            GET: "pastIncidentDataForId"
        ]
    }
    path("/intrusionState") {
        action: [
                GET: "intrusionState"
        ]
    }
    path("/clearIntrusion") {
        action: [
                GET: "clearIntrusion"
        ]
    }
    path("/playClip") {
        action: [
                GET: "playClip"
        ]
    }
    path("/deleteClip") {
        action: [
                GET: "deleteClip"
        ]
    }
    path("/tutorial") {
        action: [
                GET: "tutorial"
        ]
    }
    path("/video") {
        action: [
                GET: "video"
        ]
    }
}

def video() {
    //log.info "video() params: $params"
    sendEvent(name: "debug", value: "video()", descriptionText: "params: $params")
}

/*
 *  API endpoint that deletes clip from past incident
 */
def deleteClip() {
    def incidentId = params.incidentId
    def clipId = params.clipId

    def clip = app.getIncidentClip(UUID.fromString(params.clipId))
    if (clip) {
        log.info "Deleting clip hub: $clip.hubId, device: $clip.deviceId, clip: $clip.id"
        //deleteClip(clip.hubId, clip.deviceId, clip.id.toString())
        //clip.delete()
        deleteClip(clip)
        //log.trace "Clip deleted"
        return [clipId: params.clipId]
    }
    else {
        log.warn "Clip $params.clipId not found"
        return [error: [message:"Failed to find Incident"]]
    }
}

/**
 * API endpoint called in response to tapping the play button on a video element pointing to and HLS URL. Starts streaming to the cloud.
 */
def playClip() {
    //log.debug "PLAY $params.appId, $params.deviceId"
    def children = getChildApps()
    def child = children.find{it.id == params.appId}
    def player = child?.findCamera(params.deviceId)
    player?.start()
}

/**
 * API endpoint that returns module page
 */
def home() {
    //log.debug "/home?$params"
    def videoWidth = 285;
    def videoHeight = 175;
    def v = UUID.randomUUID()
    renderHTML("test", true) {
        head {
            """
            <script src="${resource('javascript/jquery.min.js')}"><\u002fscript>
            <script src="${resource('javascript/jquery-ui.min.js')}"><\u002fscript>
            <script src="${resource('javascript/touchpunch.js')}"><\u002fscript>
            <script src="${resource('javascript/handlebars.min.js')}"><\u002fscript>
            <script src="${resource('javascript/js.cookie.js')}"><\u002fscript>
            <link rel="stylesheet" href="${resource('css/app.css')}?v=${v}"/>
            
            ${extraCSS()}
            
            <script>
                function eventReceived(evt) {
                    APP.eventReceived(evt);
                }
            <\u002fscript>
        """
        }
        body {
            """
            <div id="container"><\u002fdiv>
            
            <div id="modal">
                <div id="modal-content"><\u002fdiv>
                <div id="close-btn">X<\u002fdiv>
            <\u002fdiv>
            
            <div id="confirm-modal" class="black-modal">
                <div id="confirm-container">
                    <p>Are you sure you want to dismiss this incident?<\u002fp>
                    
                    <a class="confirm-btn btn" data-answer="yes">Yes<\u002fa>
                    <a class="confirm-btn btn" data-answer="no">No<\u002fa>
                <\u002fdiv>
            <\u002fdiv>
            
            <script id="apps-template" type="text/x-handlebars-template">
                <ul id="watch-types">
                    <li>
                        <div id="hero" class="hero-{{hero.status}} {{hero.className}}"  data-id="{{hero.id}}">
                            <div class="hero-message">
                                {{hero.message}}
                            <\u002fdiv>
                        <\u002fdiv>
                    <\u002fli>
                    {{#each apps}}
                    <li>
                        <div class="app" data-id="{{id}}">
                            <span class="watch-type"><img src="{{icon}}" class="apps-icon" />{{label}}<\u002fspan>
                            <span class="watch-status">{{{statusMessage}}}<\u002fspan>
                            <span class="clear"><\u002fspan>
                        <\u002fdiv>
                        {{#if intrusion}}
                        <div>
                            <ul id="arm-states">
                                <li data-state="armAway" id="away">
                                    <div class="status-icon status-away-icon"><\u002fdiv>
                                    Arm (Away)
                                <\u002fli>
                                <li data-state="armStay" id="stay">
                                    <div class="status-icon status-stay-icon"><\u002fdiv>
                                    Arm (Stay)
                                <\u002fli>
                                <li data-state="disarm" id="off">
                                    <div class="status-icon status-off-icon"><\u002fdiv>
                                    Disarm
                                <\u002fli>
                                <span class="clear"><\u002fspan>
                            <\u002ful>
                        <\u002fdiv>
                        {{/if}}
                    <\u002fli>
                    {{/each}}
                <\u002ful>
                {{#if help}}
                <div id="security-help">
                    <img src="${resource('images/intrusion-tips.png')}"/>
                <\u002fdiv>
                {{/if}}
            <\u002fscript>
            
            <script id="video-clip-template" type="text/x-handlebars-template">
                <span id="{{correlationId}}" class="clip-container" data-instance="single">
                {{#if show}}
                    <video class="{{status}}" data-app="{{appId}}" data-device="{{deviceId}}" data-clip="{{correlationId}}" width="$videoWidth" height="$videoHeight" poster="{{thumbnailPath}}">
                        <source src="{{clipPath}}" type="video/mp4"">
                        Your browser does not support the video tag.
                    <\u002fvideo>
                    <!--
                    <div class="video-controls">
                        <img src="${resource('images/play.png')}" class="play-pause" />
                    <\u002fdiv>
                    -->
                    <span class="video-beta-msg">Clip Player (Beta)<\u002fspan>
                    <div class="clear"><\u002fdiv>
                {{else}}
                    <span class="clip-placeholder"><img src="${resource('images/clip_generating.jpg')}"/><\u002fspan>
                {{/if}} 
                 <\u002fspan>
             <\u002fscript>
            
            <script id="incident-template" type="text/x-handlebars-template">
                <div class="incident-details-container">
                    <h1><img src="${resource('images/incident-ind.png')}" class="incident-indicator" /> {{app.label}}<\u002fh1>
                    
                    <ul class="incident-details" id="incident-message-list">
                        <li>
                            <span class="action-icon action-{{incident.type}}"><\u002fspan> 
                            <span class="incident-message">
                                {{incident.message}}
                            <\u002fspan>
                            <span class="incident-time">{{incident.date}}<\u002fspan>
                            <span class="clear"><\u002fspan>
                        <\u002fli>
                    <\u002ful>
                    {{#if app.isMuteable}}
                        <a class="device-btn btn" data-name="{{app.name}}">Mute Sirens & Audio<\u002fa>
                    {{/if}}
                <\u002fdiv>
            <\u002fscript>

            <script id="incident-messages-template" type="text/x-handlebars-template">
                {{#each incident.messages}}
                <li class="message-list-element">
                    <span class="action-icon action-{{type}}"><\u002fspan> 
                    <span class="incident-message">
                        {{text}}
                    <\u002fspan>
                    <span class="incident-time">{{date}}<\u002fspan>
                    <span class="clear"><\u002fspan>
                    <div id="video-clips">
                        {{#each clips}}
                            <span id="{{correlationId}}" class="clip-container" data-instance="msglist">
                                {{#if show}}
                                    <video class="{{status}}" data-app="{{appId}}" data-device="{{deviceId}}" data-clip="{{correlationId}}" width="$videoWidth" height="$videoHeight" poster="{{thumbnailPath}}" controls>
                                    <source src="{{clipPath}}" type="video/mp4">
                                    Your browser does not support the video tag.
                                    <\u002fvideo>
                                    <!--
                                    <div class="video-controls">
                                        <img src="${resource('images/play.png')}" class="play-pause" />
                                    <\u002fdiv>
                                    -->
                                    <span class="video-beta-msg">Clip Player (Beta)<\u002fspan>
                                    <div class="clear"><\u002fdiv>
                                {{else}}
                                    <span class="clip-placeholder"><img src="${resource('images/clip_generating.jpg')}"/><\u002fspan>
                                {{/if}}
                            <\u002fspan>
                        {{/each}}
                    <\u002fdiv>
                <\u002fli>
                {{/each}}
            <\u002fscript>

            <script id="incident-buttons-template" type="text/x-handlebars-template">
                <div class="incident-buttons-container">
                    <a class="dismiss-btn btn" data-name="{{name}}">Dismiss<\u002fa>
                <\u002fdiv>
            <\u002fscript>
            
            <script id="incident-incidator-template" type="text/x-handlebars-template">
                Incident at {{date}}
            <\u002fscript>
            
            <script id="unconfigured-template" type="text/x-handlebars-template">
                <div id="unconfigured">
                    <div class="headline">Keep your home safe and sound with Smart Home Monitor.<\u002fdiv>
                    <div class="description-container">
                        <p>Like an alarm system, you can arm Smart Home Monitor with your phone.  We will send you a notification when we find something wrong.<\u002fp>
                        <p>Tap the gear above to configure.<\u002fp>
                    <\u002fdiv>
                <\u002fdiv>
            <\u002fscript>

            <script id="cached-apps-template" type="text/x-handlebars-template">
                <ul id="watch-types">
                    <li>
                        <div id="hero">
                            <div class="hero-message">
                                Loading...
                            <\u002fdiv>
                        <\u002fdiv>
                    <\u002fli>
                    {{#each apps}}
                    <li>
                        <div class="app">
                            <span class="watch-type"><img src="{{icon}}" class="apps-icon" />{{label}}<\u002fspan>
                            <span class="watch-status">Loading<\u002fspan>
                            <span class="clear"><\u002fspan>
                        <\u002fdiv>
                    <\u002fli>
                    {{/each}}
                <\u002ful>
            <\u002fscript>
            
            
            <script src="${resource('javascript/app.js')}?v=${v}"><\u002fscript>
        """
        }
    }
}

/**
 * API endpoint that returns page content data for populating the main page
 */
def getInitialData() {
    //log.debug "/getInitialData?$params"
    setSummary([:])
    def cA = []
    def includesIntrusion = false
    def heroClass = ""
    def heroId = ""
    def heroMessage = "Everything OK"

    def totalIncidents = 0
    def order = [
            "Security": 1,
            "Smoke": 2,
            "Leaks": 3,
            "Custom": 4
    ]

    def labels = [
            "Security": "Security",
            "Smoke": "Smoke",
            "Leaks": "Leaks",
            "Custom": "Custom"
    ]

    def icons = [
            "Security": "list_intrusion.png",
            "Smoke": "list_flame.png",
            "Leaks": "list_leak.png"
    ]

    def authorization = null
    def cookie = null
    def cameras = [:]

    if (app.id) {
        getChildApps().each {currentChildInstance ->

            if(currentChildInstance.getName() != "Custom"){
                def statusMessage
                def status
                def activeIncidents = currentChildInstance.getActiveIncidents()

                if (activeIncidents.size > 0){
                    statusMessage = '<span class="list-icon incident-icon"><\u002fspan> View Alert'
                    status = 1
                    totalIncidents++
                }else{
                    statusMessage = '<span class="list-icon ok-icon"><\u002fspan> OK'
                    status = 0
                }

                def appId = currentChildInstance.getId()

                def c = [
                        id: appId,
                        name: currentChildInstance.name,
                        label: labels[currentChildInstance.name],
                        namespace: currentChildInstance.getNamespace(),
                        status: status,
                        icon: resource("images/${icons[currentChildInstance.getName()]}"),
                        statusMessage: statusMessage,
                        isMutable: currentChildInstance.getIsMuteable(),
                        incidents: activeIncidents.collect {
                            heroMessage = it.title
                            heroId = appId

                            [
                                    title: it.title,
                                    message: it.message,
                                    date: tf.format(it.getDate()),
                                    disposition: it.getDisposition(),
                                    type: it.sourceType,
                                    messages: it.getMessages()?.collect {
                                        [
                                                text: it.text,
                                                date: tf.format(it.getDate()),
                                                type: it.sourceType,
                                                clips: it.clips.collect {
                                                    def clipStatus = it.status != null ? it.status : (it.clipPath?.contains(".mp4") ? "completed" : "initiated")
                                                    def eventData = [
                                                            appId   : appId,
                                                            deviceId: it.deviceId,
                                                            clipPath: it.clipPath,
                                                            thumbnailPath: it.thumbnailPath,
                                                            correlationId: it.id.toString(),
                                                            show    : clipStatus in ["completed"],
                                                            status  : clipStatus
                                                    ]
                                                    eventData
                                                }
                                        ]
                                    }
                            ]
                        }
                ]

                if(c.name == "Security"){
                    includesIntrusion = true
                    c['intrusion'] = [
                            currentState: currentChildInstance.currentState("systemStatus")?.value
                    ]
                    cA.add(0, c)
                }else{
                    cA << c
                }
            }
        }
    }

    def heroStatus = "ok"

    if(totalIncidents > 0){
        heroStatus = "incident"
        if(totalIncidents == 1){
            heroClass = "app"
        }else{
            heroId = ""
            heroMessage = "${totalIncidents} Total Incidents"
        }
    }

    return [
            name: "initialData",
            heroStatus: heroStatus,
            heroMessage: heroMessage,
            heroClass: heroClass,
            heroId: heroId,
            currentApps: cA,
            includesIntrusion: includesIntrusion,
            summaryData: state.summaryData,
            cameras: [], // TODO - remove
            help: state.kgse && cA.size() == 1 && location.currentState("alarmSystemStatus")?.value == "off"
    ]
}

/**
 * Called by child apps to start an incident. Event is sent to the main page to update status UI
 */
def newIncident(type) {
    sendEvent(name: "newIncident", value: type, isStateChange: true, displayed: false)
}

/**
 * Called by child apps to indicate a new message. Event is sent to the main page to update incident details UI
 */
def newMessage(type) {
    sendEvent(name: "newMessage", value: type, isStateChange: true, displayed: false)
}

/**
 * API endpoint to dismiss an incident
 */
def dismissIncident() {
    //log.info "/dismissIncident?$params"
    sendEvent(name: "newIncident", value: "clear", isStateChange: true, displayed: false)
    def cA = findChildAppByName(params.name)
    cA.dismissIncident()

    return [yippy:"this worked"]
}

/**
 * API endpoint to silence sirens
 */
def dismissSirens() {
    def cA = findChildAppByName(params.name)
    cA.dismissSirens()

    return [yippy:"this worked"]
}

/**
 * Returns the Intrusion child smart app
 */
def getIntrusionApp() {
    findChildAppByName("Security")
}

def getSmokeAndCoApp() {
    findChildAppByName("Smoke")
}

def getLeaksAndFloodsApp() {
    findChildAppByName("Leaks")
}

def installInitialModules() {
    log.trace "installInitialModules()"
    if (findAllDevicesByCapability("contactSensor") || findAllDevicesByCapability("motionSensor")) {
        if (!intrusionApp) {
            addChildApp("Security", "Security", [settings: [pushNotification: true, toggle1: true, toggle2: true]])
        }
    }
    else {
        subscribe(location, "contact", securityInstaller)
        subscribe(location, "motion", securityInstaller)
    }

    if (findAllDevicesByCapability("smokeDetector")) {
        if (!smokeAndCoApp) {
            addChildApp("Smoke", "Smoke", [settings: [pushNotification: true, toggle1: true]])
        }
    }
    else {
        subscribe(location, "smoke", smokeInstaller)
    }

    if (findAllDevicesByCapability("waterSensor")) {
        if (!leaksAndFloodsApp) {
            addChildApp("Leaks", "Leaks", [settings: [pushNotification: true, toggle1: true]])
        }
    }
    else {
        subscribe(location, "water", leaksInstaller)
    }
}

def securityInstaller(evt) {
    if (!intrusionApp) {
        addChildApp("Security", "Security", [settings: [pushNotification: true, toggle1: true, toggle2: true]])
    }
    unsubscribe("securityInstaller")
}

def smokeInstaller(evt) {
    if (!smokeAndCoApp) {
        addChildApp("Smoke", "Smoke", [settings: [pushNotification: true, toggle1: true]])
    }
    unsubscribe("smokeInstaller")
}

def leaksInstaller(evt) {
    if (!leaksAndFloodsApp) {
        addChildApp("Leaks", "Leaks", [settings: [pushNotification: true, toggle1: true]])
    }
    unsubscribe("leaksInstaller")
}

/**
 * API endpoint to set the intrustion alarm system state
 */
def intrusionState(){
    //log.info "/intrusionState?$params"
    switch(params.state) {
        case "armAway":
            //intrusion.armAway()
            sendLocationEvent(name: "alarmSystemStatus", value: "away")
            return [currentState: "away"]
            break
        case "armStay":
            //intrusion.armStay()
            sendLocationEvent(name: "alarmSystemStatus", value: "stay")
            return [currentState: "stay"]
            break
        case "disarm":
            //intrusion.disarm()
            sendLocationEvent(name: "alarmSystemStatus", value: "off")
            return [currentState: "off"]
            break
    }

    return [currentState: intrusion.currentState("systemStatus")?.value]
}

/**
 * API endpoint to return data for past incidents
 */
def pastIncidentData() {
    //log.info "/pastIncidentData?$params"
    def icons = [
            "intrusion": "list_intrusion.png",
            "smoke": "list_flame.png",
            "water": "list_leak.png"
    ]

    def incidents
    if(params?.lastIncidentId && params.lastIncidentId != "0"){
        try{
            incidents = app.moduleIncidents(max: 10, startAfter: UUID.fromString(params.lastIncidentId))
        }catch(e){
            return [
                    incidents: []
            ]
        }
    }else{
        incidents = app.moduleIncidents(max: 10)
    }

    return [
            incidents: incidents.collect {
                [
                        id: it.getEncodedId(),
                        title: it.title,
                        type: it.sourceType,
                        icon: resource("images/${icons[it.sourceType] ?: 'list_general.png'}"),
                        message: it.message,
                        date: df.format(it.getDate()),
                        time: tf.format(it.getDate()),
                        disposition: it.getDisposition()
                ]
            }
    ]
}

def pastIncidentDataForId() {
    def incidentId = params.incidentId
    def incident = app.getIncident(incidentId)
    def clipIds = []
    [
        id: incident.getEncodedId(),
        title: incident.title,
        type: incident.sourceType,
        message: incident.message,
        date: df.format(incident.getDate()),
        time: tf.format(incident.getDate()),
        disposition: incident.getDisposition(),
        clips: [],  // TODO - remove
        messages: incident.getMessages()?.collect {
            def messageClips = it.clips.collect{
                def cid = it.id.toString()
                clipIds << cid
                [id: cid, clipPath: it.clipPath, thumbnailPath: it.thumbnailPath]
            }

            [
                text: it.text,
                date: tf.format(it.getDate()),
                type: it.sourceType,
                clips: messageClips
            ]
        }
    ]
}

def pastIncidents() {
    //log.info "/newPastIncidents?$params"
    def videoWidth = 285;
    def videoHeight = 175;
    def v = UUID.randomUUID()

    renderHTML("test", true) {
        head {
            """
            <script src="${resource('javascript/jquery.min.js')}"><\u002fscript>
            <script src="${resource('javascript/jquery-ui.min.js')}"><\u002fscript>
            <script src="${resource('javascript/touchpunch.js')}"><\u002fscript>
            <script src="${resource('javascript/handlebars.min.js')}"><\u002fscript>
            <script src="${resource('javascript/js.cookie.js')}"><\u002fscript>
            <link rel="stylesheet" href="${resource('css/app.css')}?v=${v}"/>
            <link rel="stylesheet" href="${resource('css/past.css')}?v=${v}"/>
            
            ${extraCSS()}
            
            <script>
                function eventReceived(evt) {
                    APP.eventReceived(evt);
                }
            <\u002fscript>
        """
        }
        body {
            """
            <div id="past-incidents">
                <h3>Recently<\u002fh3>
                <div id="container" class="past-incidents"><\u002fdiv>
            <\u002fdiv>
            
            <div id="modal">
                <div id="modal-content"><\u002fdiv>
                <div id="close-btn">X<\u002fdiv>
            <\u002fdiv>

            <div id="confirm-modal" class="black-modal">
                <div id="confirm-container">
                    <p>Are you sure you want to dismiss this clip?<\u002fp>
                    
                    <a class="confirm-btn btn" data-answer="yes">Yes<\u002fa>
                    <a class="confirm-btn btn" data-answer="no">No<\u002fa>
                <\u002fdiv>
            <\u002fdiv>

            <script id="no-incidents-template" type="text/x-handlebars-template">
                <div id="no-incidents">
                    <p>You have no past incidents to report at this time.<\u002fp>
                <\u002fdiv>
            <\u002fscript>
            
            <script id="incident-list-template" type="text/x-handlebars-template">
                <ul id="watch-types">
                    {{#each incidents}}
                    <li>
                        <div class="app" data-id="{{id}}">
                            <span class="watch-type"><img src="{{icon}}" class="apps-icon" />{{title}}<\u002fspan>
                            <span class="watch-status">{{date}} {{time}}<\u002fspan>
                        <\u002fdiv>
                    <\u002fli>
                    {{/each}}
                    <a class="device-btn btn" id="load-more">Load More<\u002fa>
                <\u002ful>
            <\u002fscript>
            
            <script id="incident-template" type="text/x-handlebars-template">
                
                    <div class="past-incident-container" data-incident-id="{{incident.id}}">
                        <div class="incident-message-header">
                            <div class="incident-title">{{incident.title}}<\u002fdiv>
                        <\u002fdiv>
                    
                        <div class="incident-date-message-container">
                            <strong>{{incident.date}} {{incident.time}}<\u002fstrong><br />
                            {{incident.message}}
                        <\u002fdiv>

                        <div class="incident-details-container">
                            <ul class="incident-details">
                                {{#each incident.messages}}
                                <li>
                                    <span class="action-icon action-{{type}}"><\u002fspan> 
                                    <span class="incident-message">
                                        {{text}}
                                    <\u002fspan>
                                    <span class="incident-time">{{date}}<\u002fspan>
                                    <span class="clear"><\u002fspan>
                                        {{#each clips}}
                                            <div id="clip-{{id}}" class="clip-container">
                                                <video width="$videoWidth" height="$videoHeight" poster="{{thumbnailPath}}">
                                                <source src="{{clipPath}}" type="video/mp4">
                                                Your browser does not support the video tag.
                                                <\u002fvideo>
                                                <!--
                                                <div class="video-controls">
                                                    <img src="${resource('images/play.png')}" class="play-pause" />
                                                <\u002fdiv>
                                                -->
                                                <span class="video-beta-msg">Clip Player (Beta)<\u002fspan>
                                                <a class="btn-delete delete-clip" data-clip-id="{{id}}">Delete Clip <img src="${resource("images/trashcan.png")}" class="trashcan" /><\u002fa>
                                				<div class="clear"><\u002fdiv>
                                            <\u002fdiv>
                                        {{/each}}
                                <\u002fli>
                                {{/each}}
                            <\u002ful>
                        <\u002fdiv>
                    <\u002fdiv>
                
            <\u002fscript>
            
            <script src="${resource('javascript/past.js')}?v=1"><\u002fscript>
        """
        }
    }
}

/**
 * Returns list of permitted sites for JavaScript, CSS, images, and other resources
 */
def whitelist() {
    return [
            "ajax.googleapis.com",
            "d102a5bcjkdlos.cloudfront.net",
            "*.wikimedia.org",
            "cdnjs.cloudflare.com",
            "*.amazonaws.com",
            "*.s3.amazonaws.com",
            "s3.amazonaws.com",
            "misc-bob-stuff.s3.amazonaws.com",
            "svh.connect.smartthings.com",
            "vh.connect.smartthings.com",
            "www.w3.org",
            "127.0.0.1",
    ]
}

/**
 * Called on app installed
 */
def installed() {
    //log.debug "Installed with settings: ${settings}"
    
    initialize()
}

/**
 * Called on app uninstalled
 */
def uninstalled() {
    revokeAccessToken()
}

/**
 * Called every time app preferences are updated
 */
def updated() {
    //log.debug "Updated with settings: ${settings}"
    unsubscribe("clipHandler")
    unsubscribe("streamHandler")
    initialize()
}

/**
 * Executed on install and update
 */
def initialize() {
    setSummary(alarmState: "clear")
    
    if (!state.accessToken) {
    	createAccessToken()
    }

    // TODO - legacy cleanup, remove after all apps are updated
    state.remove("cameraStream")

    subscribe(location, "clip", clipHandler)
    subscribe(location, "stream", streamHandler)
    subscribe(location, null, hubActiveHandler, [filterEvents: false])

    log.debug "https://graph.api.smartthings.com/api/token/$state.accessToken/smartapps/installations/$app.id/getInitialData"
    log.debug "videoHostUrl = $videoHostUrl"
}

/**
 *  Called whenever a child is uninstalled
 */
def childUninstalled(){

}

/**
 * Returns the location alarm system mode value
 */
def getIntruderMode() {
    location.currentState("alarmSystemMode")?.value
}

/**
 * Called by
 */
def setSummary(params=[:]) {
    //log.trace "setSummary($params)"
    def systemStatus = params.systemStatus ?: intrusionApp?.currentState("systemStatus")?.value

    sendEvent(name: "intrusionModeChange", value: systemStatus, isStateChange: true, displayed: false)

    def alarmState = params.alarmState ?: intrusionApp?.currentState("intrusion")?.value ?: "clear"
    def smokeState = params.smoke ?: smokeAndCoApp?.currentState("smoke")?.value ?: "clear"
    def waterState = params.water ?: leaksAndFloodsApp?.currentState("water")?.value ?: "dry"
    def installedapps = getChildApps().size()

    def value = "${systemStatus}/${alarmState}/${smokeState}/${waterState}"

    def summaryData = []
    def bgColor
    def stateValue

    if (installedapps > 0) {
        if (intrusionApp != null) {
            switch(systemStatus) {
                case "stay":
                    bgColor = "#90ee90"
                    stateValue = "Armed (stay)"
                    summaryData = [[icon:"indicator-dot-green",iconColor:"#90ee90",value:stateValue]]
                    break
                case "away":
                    bgColor = "#79b821"
                    stateValue = "Armed (away)"
                    summaryData = [[icon:"indicator-dot-green",iconColor:"#79b821",value:stateValue]]
                    break
                case "off":
                    bgColor = "#ffffff"
                    stateValue = "Disarmed"
                    summaryData = [[icon:"indicator-dot-gray",iconColor:"#878787",value:stateValue]]
                    break
                default:
                    summaryData = []
                    break
            }
            switch (alarmState) {
                case "alarm":
                    summaryData = [[icon:"indicator-dot-red",iconColor:"#e86d13",value:"Intruder Detected!"]]
                    break;
                case "clear":
                    summaryData << [icon:"indicator-dot-green",iconColor:"#79b821",value:"Everything OK"]
                    break
                default:
                    summaryData << [icon:"indicator-dot-gray",iconColor:"#878787",value:alarmState]
                    break
            }
        } else {
            bgColor = "#79b821"
            stateValue = "Everything OK"
            summaryData = [[icon:"indicator-dot-green",iconColor:"#79b821",value:stateValue]]
        }
    } else {
        bgColor = "#ffffff"
        stateValue = "Unconfigured"
        summaryData = [[icon:"indicator-dot-gray",iconColor:"#878787",value:stateValue]]
    }

    switch (waterState) {
        case "wet":
            summaryData = [[icon:"indicator-dot-red",iconColor:"#e86d13",value:"Water Detected!"]] // TODO - rework
            break;
        default:
            break
    }

    switch (smokeState) {
        case "detected":
            summaryData = [[icon:"indicator-dot-red",iconColor:"#e86d13",value:"Smoke Detected!"]] // TODO - rework
            break;
        default:
            break
    }

    //log.debug "summaryData: $summaryData - $atomicState.summaryData"
    atomicState.summaryData = summaryData[-1]
    sendEvent(name: "summary", value: value, isStateChange: true, displayed: false, descriptionText: "Alarm system is $value", eventType:"SOLUTION_SUMMARY", data: summaryData)
}

private getTf() {
    def f = new java.text.SimpleDateFormat("h:mm a")
    if (location.timeZone) {
        f.setTimeZone(location.timeZone)
    }
    f
}

private getDf() {
    def f = new java.text.SimpleDateFormat("MMMM d, YYYY")
    if (location.timeZone) {
        f.setTimeZone(location.timeZone)
    }
    f
}

private resource(path) {
    //buildResourceUrl(path)
    "$appSettings.s3Url/$path"
}

private extraCSS() {
    def videoWidth = 285;
    def videoHeight = 175;

    """
        <style>
            html, body{
                -webkit-user-select: none;
            }

            #unconfigured{
                background-image:url('${resource('images/smh_bg.jpg')}');
                background-size: cover;
                background-position: center;
                height:100%;
            }

            .black-modal{
                background-image:url('${resource('images/modal.png')}');
            }
        
            .clear{
                background:#1fb345;
                background-image:url('${resource('images/ok.png')}');
                background-size:35%;
                background-repeat:no-repeat;
                background-position: center;
            }

            .intrusion{
                background:#ec525f;
                background-image:url('${resource('images/disarm.png')}');
                background-size:35%;
                background-repeat:no-repeat;
                background-position: center;
            }

            .smoke {
                background:#ec525f;
                background-image:url('${resource('images/smoke.png')}');
                background-size:35%;
                background-repeat:no-repeat;
                background-position: center;
            }

            .water {
                background:#ec525f;
                background-image:url('${resource('images/water.png')}');
                background-size:35%;
                background-repeat:no-repeat;
                background-position: center;
            }

            .action-icon{
                background-color:#ec525f;
                height:30px;
                width:30px;
                border-radius:50%;
                display:inline-block;
                float:left;
                margin-right:5px;
                background-repeat:no-repeat;
                background-position: center;
                background-size: 50%;
            }

            .action-alarm{
                background-image:url('${resource('images/alarm.png')}');
            }

            .action-intrusion{
                background-image:url('${resource('images/alarm.png')}');
            }
            
            .action-audio{
                background-image:url('${resource('images/audio.png')}');
            }
            
            .action-camera{
                background-image:url('${resource('images/camera.png')}');
            }
            
            .action-contacts{
                background-image:url('${resource('images/contacts.png')}');
                background-color:#016fbc !important;
            }
            
            .action-door{
                background-image:url('${resource('images/door.png')}');
            }
            
            .action-light{
                background-image:url('${resource('images/light.png')}');
            }
            
            .action-motion{
                background-image:url('${resource('images/motion.png')}');
            }
            
            .action-smoke{
                background-image:url('${resource('images/smoke.png')}');
            }
            
            .action-water{
                background-image:url('${resource('images/water.png')}');
            }

            .action-acceleration{
                background-image:url('${resource('images/acceleration_white.png')}');
            }

            .action-humidity{
                background-image:url('${resource('images/humidity_white.png')}');
            }

            .action-contact{
                background-image:url('${resource('images/open_close_white.png')}');
            }

            .action-presence{
                background-image:url('${resource('images/presence_white.png')}');
            }

            .action-temperature{
                background-image:url('${resource('images/temp_white.png')}');
            }

            .action-valve{
                background-image:url('${resource('images/water-valve.png')}');
            }
 
            .action-clear{
                background-image:url('${resource('images/ok.png')}');
                background-color:#1fb345 !important;
            }    

            .status-icon{
                height:60px;
                width:60px;
                margin:0 auto;
            }
          
            .status-away-icon{
                background:url('${resource('images/btn_arm_away.png')}');
                background-size:60px;
            }
            
            .status-stay-icon{
                background:url('${resource('images/btn_arm_stay.png')}');
                background-size:60px;
            }
            
            .status-off-icon{
                background:url('${resource('images/btn_disarm.png')}');
                background-size:60px;
            }

            .status-on{
                background-position-y:60px;
            }

            .hero-ok{
                background:url('${resource('images/hero_ok.jpg')}');
            }

            .hero-incident{
                background:url('${resource('images/hero_generic.gif')}');
            }
            
            #hero{
                height:200px;
                border-bottom:solid 1px #d4d6dc;
                background-size: cover;
                background-position: center;
                background-color:#c6c7cc;
            }

            .hero-message{
                text-align: center;
                color:#fff;
                line-height: normal;
                padding-top:150px;
            }

            #security-help {
                margin: 0;
                padding: 0;
                border-style: none;
            }

            #security-help img {
                width: 100%;
            }
            
        <\u002fstyle>
    """
}

def tutorial() {
    log.debug "tutorial"
    // TODO - temporary	
    if (!state.accessToken) {
    	installed()
    }
    if (!atomicState.kgse) {
        log.debug "kgse = true"
        atomicState.kgse = true
        installInitialModules()
    }

    renderHTML("Tutorial", true) {
        head {
            """
            <link rel="stylesheet" href="${resource('css/owl.carousel.css')}"/>
            <link rel="stylesheet" href="${resource('css/owl.theme.css')}"/>
            <script src="${resource('javascript/jquery.min.js')}"><\u002fscript>
            <script src="${resource('javascript/owl.carousel.min.js')}"><\u002fscript>
            
            <style type="text/css">
                html, body{
                    background:#ffffff;
                }
                
                body{
                    margin: 0 0 0 0;
                }
                #owl-demo .item{
                    margin: 0 0 0 0;
                }
                #owl-demo .item img {
                    display: block;
                    width: 320px;
                    height: auto;
                    margin:auto;
                }
                
                .owl-prev{
                    margin-right:150px !important;
                }
                
                .owl-controls{
                    position: absolute;
                    top:400px;
                    margin-left: auto;
                    margin-right: auto;
                    left: 0;
                    right: 0;
                }

                .owl-theme .owl-controls .owl-buttons div{
                    background: #26a4db;
                    color: #ffffff;
                }
            <\u002fstyle>
            """
        }
        body {
            """
                <div id="owl-demo">
                    <div class="item">
                        <img src="${resource('images/01-SMH-Away.jpg')}" alt="Smart Home Monitor Step 1">
                    <\u002fdiv>
                    <div class="item">
                        <img src="${resource('images/02-SMH-Stay.jpg')}" alt="Smart Home Monitor Step 2">
                    <\u002fdiv>
                    <div class="item">
                        <img src="${resource('images/03-SMH-Disarm.jpg')}" alt="Smart Home Monitor Step 3">
                    <\u002fdiv>
                    <div class="item">
                        <img src="${resource('images/04-SMH-Incident.jpg')}" alt="Smart Home Monitor Step 4">
                    <\u002fdiv>
                <\u002fdiv>
                <script>
                    \$(document).ready(function() {
                        \$("#owl-demo").owlCarousel({
                            navigation: true,
                            autoPlay: false, //Set AutoPlay to 3 second
                            items : 1,
                            pagination: false,
                            singleItem: true
                        });
                    });
                <\u002fscript>   
            """
        }
    }
}

/**
 * Handles Camera Clip events and dispatches them to the main dashboard
 */
def clipHandler(evt) {
    log.debug "clipHandler($evt.name: $evt.value)"
    def data = evt.jsonData
    log.debug "data = $data"
    def clip = app.getIncidentClip(data.requestedCorrelationId ?: data.correlationId)
    log.debug "clip = ${clip?.id}"
    if (clip) {
        def message = clip.incidentMessage
        def incident = message.incident
        log.debug "incident = ${incident?.title} (${incident?.id}), incident.rootSmartAppId: ${incident?.rootSmartAppId}, app.id: ${app.id}"
        if (incident.rootSmartAppId == app.id) {
            def incidentApp = childApps.find { it.id == incident.installedSmartAppId }
            def device = evt.device
            def token = device.getAccessToken()
            def eventValue = evt.value
            def eventData = [
                    appId   : incidentApp?.id,
                    deviceId: evt.deviceId,
                    clipPath: addToken(data.clipPath, token),
                    thumbnailPath: addToken(data.thumbnailPath, token),
                    correlationId: clip.id.toString()
            ]

            switch (evt.value) {
                case "initiated":
                    log.debug "processing initiated"
                    clip.correlationId = eventData.correlationId
                    message.show = true
                    message.save()
                    break

                case "ignored":
                    def primaryClip = app.getIncidentClip(data.correlationId)
                    clip.correlationId = data.correlationId
                    log.debug "primaryClip: ${primaryClip?.id}, data.requestedCorrelationId: $data.requestedCorrelationId, data.correlationId: $data.correlationId"
                    if (primaryClip) {
                        eventValue = primaryClip.status
                        clip.primaryClip = primaryClip
                        clip.clipPath = primaryClip.clipPath
                        clip.thumbnailPath = primaryClip.thumbnailPath
                        message.show = true
                        message.text = "${device.displayName} already recording"
                        message.save()

                        if (data.endTime) {
                            incidentApp.sendEvent(name: evt.deviceId, value: data.endTime.fromSystemFormat().time + 1000)
                        }
                    }
                    break

                case "streaming":
                    eventValue = "streamAvailable"
                    clip.clipPath = eventData.clipPath
                    clip.thumbnailPath = eventData.thumbnailPath

                    eventData.status = eventValue
                    eventData.show = false

                    clip.referencingClips.each {
                        def thisEventData = eventData + [correlationId: it.id.toString()]
                        it.clipPath = eventData.clipPath
                        it.thumbnailPath = eventData.thumbnailPath
                        it.status = eventValue
                        it.authorization = token
                        it.save()
                        sendEvent(name: "clipAvailable", value:eventValue, data: thisEventData, eventType: "VIDEO", isStateChange: true, displayed: false)
                    }
                    break

                case "completed":
                    log.debug "processing completed"
                    clip.clipPath = eventData.clipPath
                    clip.thumbnailPath = eventData.thumbnailPath

                    eventData.status = eventValue
                    eventData.show = true

                    clip.referencingClips.each {
                        def thisEventData = eventData + [correlationId: it.id.toString()]
                        it.clipPath = eventData.clipPath
                        it.thumbnailPath = eventData.thumbnailPath
                        it.status = "completed"
                        it.authorization = token
                        it.save()

                        sendEvent(name: "clipAvailable", value:eventValue, data: thisEventData, eventType: "VIDEO", isStateChange: true, displayed: false)
                    }
                    break

                case "failed":
                    message.show = true
                    message.text = "${device.displayName} failed to record clip"
                    message.save()
                    break

                default:
                    log.error "Unexpected clip status of '$evt.value' from $device.displayName ($evt.deviceid)"
            }

            clip.authorization = token
            clip.status = eventValue
            clip.save()

            // Event sent to HTML card
            log.debug "sending clipAvailable event"
            eventData.status = eventValue
            eventData.show = eventValue in ["completed"]
            sendEvent(name: "clipAvailable", value: eventValue, data: eventData, eventType: "VIDEO", isStateChange: true, displayed: false)
        }
    }
}

/**
 * Stores streaming information from cameras
 */
def streamHandler(evt) {
    log.debug "streamHandler($evt.name: $evt.id), deviceId: $evt.deviceId"
    def data = evt.jsonData
    def clip = app.getLatestIncidentClipByDeviceIdAndStatus(evt.deviceId, "streamAvailable")
    if (clip) {
        def incident = clip.incident
        if (incident?.rootSmartAppId == app.id) {
            clip.status = "streamActive"
            clip.clipPath = addToken(data.OutHomeURL, clip.authorization)
            clip.cookieName = data.cookie.key
            clip.cookieValue = data.cookie.value
            clip.save()

            def incidentApp = childApps.find { it.id == incident.installedSmartAppId }
            def eventData = [
                    appId   : incidentApp?.id,
                    deviceId: evt.deviceId,
                    clipPath: clip.clipPath,
                    thumbnailPath: clip.thumbnailPath,
                    correlationId: clip.id.toString(),
                    status: "streamActive",
                    show: false
            ]

            sendEvent(name: "clipAvailable", value: "streamActive", data: eventData, eventType: "VIDEO", isStateChange: true, displayed: false)

            clip.referencingClips.each {
                def thisEventData = eventData + [correlationId: it.id]
                it.clipPath = eventData.clipPath
                it.thumbnailPath = eventData.thumbnailPath
                it.status = "streamActive"
                it.authorization = token
                it.save()
                sendEvent(name: "clipAvailable", value:"streamActive", data: thisEventData, eventType: "VIDEO", isStateChange: true, displayed: false)
            }
        }
    }
}

/**
 * Called when hub becomes active again. Resets any behavior apps, just in case
 */
def hubActiveHandler(evt) {
    log.debug "hubActiveHandler($evt.name)"
    switch (evt.name) {
    	case "hub has disconnected":
        	log.trace "resetting triggers"
        	childApps*.resetTrigger()
            break
    }
}

private addToken(url, token) {
    def result = url
    if (url) {
        if (result.contains("?")) {
            result += "&"
        }
        else {
            result += "?"
        }
        result += "Authorization=Bearer+${token}"
    }
    //log.trace "addToken($url, $token) = $result"
    result
}

