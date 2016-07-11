/**
 *  D-Link Camera
 *
 *  Copyright 2015 SmartThings
 *
 *  Author: Jack Chi - jack@smartthings.com
 *          Todd Wackford - todd@smartthings.com
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
 ****************************************************************************************
 *
 * Change Log:
 *
 * 1.	20150601	Jack Chi & Todd Wackford
 *		* Initial builds.
 *
 * 2.	20150901	Todd Wackford
 *		* Synchronized video events with Samsung Cam
 *		* Cleaned up log calls
 *		* Added change log in header
 *		* Moved Profile to multi tile only
 *		* Added method to call parent app for profile update
 *
 * 3.	20150913	Todd Wackford
 *		* Fixed bug which would not update camera if user picked profile 3
 */

metadata {

    definition(name: "D-Link Camera", namespace: "d-link", author: "SmartThings") {

        capability "Configuration"
        capability "Video Camera"
        capability "Video Capture"
        capability "Refresh"
        capability "Switch"

        //custom commands
        command "record20"
        command "start"
        command "stop"
        command "setProfile"
        command "setProfileHD"
        command "setProfileSDH"
        command "setProfileSDL"

        // custom attributes
        attribute "profile", "number"
        attribute "supportedProfiles", "json_object"
        attribute "firmwareVersion", "string"
        attribute "manufacturer", "string"
        attribute "model", "string"
        attribute "nipcaVersion", "string"

        input "cameraPassword", "password", title: "Camera Password", required: true
        input "cameraAudio", "enum", title: "Mic Gain", options: ["Mute","Low","Med","High"] , defaultValue: "${cameraAudio}" , multiple: false
        input "cameraImage", "enum", title: "Flip and Mirror", options: ["No Flip - No Mirror", "Mirror", "Flip" , "Flip - Mirror"], defaultValue: "${cameraImage}", multiple: false
        input "cameraBrightness", "enum", title: "Brightness", options: ["0","1","2","3","4","5","6","7","8"], defaultValue: "${cameraBrightness}" , multiple: false
        input "cameraOverlay", "enum", title: "Date-Label Overlay", options: ["On","Off"], defaultValue: "${cameraOverlay}",  multiple: false
    }

    tiles (scale: 2) {

        multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 6, height: 4){
            tileAttribute("device.switch", key: "CAMERA_STATUS") {
                attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", action: "switch.off", backgroundColor: "#79b821", defaultState: true)
                attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", action: "switch.on",  backgroundColor: "#ffffff")
                attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
                attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", action: "refresh.refresh", backgroundColor: "#F22000")
            }

            tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
                attributeState("on", label: "Active", icon: "st.camera.dlink-indoor", backgroundColor: "#79b821", defaultState: true)
                attributeState("off", label: "Inactive", icon: "st.camera.dlink-indoor", backgroundColor: "#ffffff")
                attributeState("restarting", label: "Connecting", icon: "st.camera.dlink-indoor", backgroundColor: "#53a7c0")
                attributeState("unavailable", label: "Unavailable", icon: "st.camera.dlink-indoor", backgroundColor: "#F22000")
            }

            tileAttribute("device.startLive", key: "START_LIVE") {
                attributeState("live", action: "start", defaultState: true)
            }

            tileAttribute("device.stream", key: "STREAM_URL") {
                attributeState("activeURL", defaultState: true)
            }

            tileAttribute("device.profile", key: "STREAM_QUALITY") {
                attributeState("1", label: "720p",  action: "setProfileHD",  defaultState: true)
                attributeState("2", label: "h360p", action: "setProfileSDH", defaultState: true)
                attributeState("3", label: "l360p", action: "setProfileSDL", defaultState: true)
            }

            tileAttribute("device.betaLogo", key: "BETA_LOGO") {
                attributeState("betaLogo", label: "", value: "", defaultState: true)
            }
        }

        standardTile("tileRecord", "device.record", width: 2, height: 2,) {
            state "record", label: "Record", action: "record20", icon: "st.camera.camera", backgroundColor: "#FFFFFF", nextState: "recording"
            state "recording", label: "Recording", icon: "st.camera.camera", backgroundColor: "#53a7c0"
            state "unavailable", label: "OFF", icon: "st.camera.camera", backgroundColor: "#F22000"
        }

        standardTile("tileRefresh", "device.refresh", width: 2, height: 2, decoration: "flat") {
            state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        htmlTile(name: "tileBalloon", action: "getBalloon", width: 6, height: 1)

        main "videoPlayer"
        details(["videoPlayer", "tileBalloon", "tileRefresh"])
    }
}

mappings {
    path("/getBalloon") {
        action: [ GET: "getBalloon" ]
    }
}

def getBalloon() {
    renderHTML {
        head {
            """ <style type="text/css">
                    body {
                        background-color: #A0A0A0;
                        font-family: arial;
                    }
                     #beta {
                        background-color: #FFFFFF;
                        -moz-border-radius: 25px;
                        -webkit-border-radius: 25px;
                        padding: 1px 10px;
                        color: #A0A0A0;
                        font-family: arial;
                    }
                    .beta-div {
                        width: 20%;
                        float: left;
                        padding-top: 3%;
                        padding-left: 2%
                    }
                    .word-div {
                        width: 75%;
                        float: right;
                        margin-left: 2px;
                        font-size: 85%;
                    }
                    .full-div {
                        width: 100%;
                        display: inline;
                    }
                    .white {
                        color: #FFFFFF;
                    }
                <\u002fstyle> """
        }
        body {
            """
            <div style="full-div">
                <div class="beta-div">
                    <span id="beta">BETA<\u002fspan>
                <\u002fdiv>
                <div class="word-div white">
                    Having difficulties seeing Live Video? Turn off Wi-Fi to switch to cellular data and try again.
                <\u002fdiv>
            <\u002fdiv>
            """
        }
    }
}

/**
 * Device Type Lifecycle Declarations
 * */
def parse(String description) {

    def results = [] // accumulate events to send out

    try {
        def msg = parseLanMessage(description)
        // log.trace "parse(msg) $msg"

        // CAMERA REDISCOVERY
        if (msg?.ssdpTerm?.contains(parent.getDeviceType())){
            log.trace "parse(SSDP): $msg "
            parent.locationHandler(description)
        }
        // CAMERA MESSAGES
        else if (!msg.headers?."content-location"?.startsWith("/dev/")) {
            //log.trace "parse(CAMERA): $description"
            if ( msg.body?.contains("model") &&
                 msg.body?.contains("version") &&
                 msg.body?.contains("nipca") ) {
                 
            	def cameraInfo = getListFromBody(msg.body)

                sendEvent(name: "firmwareVersion", 	value: cameraInfo.version, 		displayed: false)
                sendEvent(name: "manufacturer", 	value: "D-Link Corporation", 		displayed: false)
                sendEvent(name: "model", 		value: cameraInfo.model, 		displayed: false)
                sendEvent(name: "nipcaVersion", 	value: cameraInfo.nipca, 		displayed: false)
            } 
        }
        // VIDEO-CORE MESSAGES
        else {
            log.trace "parse(VIDEO-CORE):"
            if (!msg.body) {
                def status = msg.status ?: "none"
                switch(status){
                // stopping the livestream
                    case "204":
                        //log.trace "parse(stopping livestream): ${msg.headers}"
                        break
                }
            }
            else {
                def body = parseJson(msg.body)

                // LIVESTREAM CALLBACKS
                if (body.streamClass) {
                    //log.trace "parse(live): $body"

                    def cookies = body?.cookies
                    def awsCookie = cookies.find { it?.key == "AWSELB" }

                    def dataLiveVideo = [
                            OutHomeURL : body?.src,
                            InHomeURL : state.rtspURL,
                            ThumbnailURL: body?.thumbnail?.src ?: "http://cdn.device-icons.smartthings.com/camera/dlink-indoor@2x.png",
                            cookie: awsCookie
                    ]

                    def eventLiveVideo = [name        : "stream",
                                          value        : toJson(dataLiveVideo).toString(),
                                          data         : toJson(dataLiveVideo),
                                          descriptionText : "Starting the livestream",
                                          eventType    : "VIDEO",
                                          displayed    : false,
                                          isStateChange: true
                    ]

                    sendEvent(eventLiveVideo)
                }
                // CLIP CREATED
                else if (body.clip) {
                    //log.debug "parse(sync-clip): $body"
                    if (body.clip.status == 'Created') {
                        //log.trace "Clip: ${body.clip.status}"
                    }
                }
                // CLIP CALLBACKS
                else if (body.captureTime) {
                    //log.debug "parse(clip): $body"

                    if (body.status == 'initialized') { // CLIPS STREAMING
                        //log.trace "Clip Initialized: ${body.status}"

                        def dataS3URLStreaming = [
                                hubId               : device.hub.id,
                                deviceid            : device.id,
                                correlationId       : body.correlationId,
                                captureTime         : body.captureTime,
                                startTime           : body.startTime,
                                endTime             : body.endTime,
                                streamPath          : body.url,
                                thumbnailPath       : body.thumbnail?.url,
                                status     : "INITIATED"
                        ]

                        def eventUpdateClipStreaming = [
                                name           : "updateClip",
                                value          : "streaming",
                                descriptionText: "Clip is streaming",
                                data           : toJson(dataS3URLStreaming),
                                eventType      : "VIDEO",
                                displayed      : false,
                                isStateChange  : true
                        ]

                        def dataProxyURL = [
                                hubId               : device.hub.id,
                                deviceid            : device.id,
                                correlationId       : body.correlationId,
                                captureTime         : body.captureTime,
                                startTime           : body.startTime,
                                endTime             : body.endTime,
                                clipPath            : rewriteS3Url(body.url),
                                thumbnailPath       : rewriteS3Url(body.thumbnail?.url),
                                status     : "INITIATED"
                        ]

                        def eventClipStreaming = [
                                name           : "clip",
                                value          : "streaming",
                                descriptionText: "Clip is streaming",
                                data           : toJson(dataProxyURL),
                                eventType      : "VIDEO",
                                displayed      : false,
                                isStateChange  : true
                        ]

                        sendEvent(eventUpdateClipStreaming)
                        sendEvent(eventClipStreaming)
                        state.currentClipData = dataProxyURL
                    }
                    else if (body.status == 'ready') { // MP4 READY

                        //log.trace "Clip Complete: ${body.status}"

                        // COMPLETE CLIP Event
                        def dataS3URLComplete= [
                                hubId           : device.hub.id,
                                deviceId        : device.id,
                                correlationId   : body.correlationId,
                                captureTime     : body.captureTime,
                                startTime       : body.startTime,
                                endTime         : body.endTime,
                                clipPath        : body.url,
                                thumbnailPath   : body.thumbnail?.url,
                                status          : "COMPLETE"
                        ]

                        def eventUpdateClipComplete = [
                                name           : "updateClip",
                                value          : "completed",
                                descriptionText: "Clip is completed",
                                data           : toJson(dataS3URLComplete),
                                eventType      : "VIDEO",
                                displayed      : false,
                                isStateChange  : true
                        ]


                        def dataProxyURLComplete= [
                                hubId           : device.hub.id,
                                deviceId        : device.id,
                                correlationId   : body.correlationId,
                                captureTime     : body.captureTime,
                                startTime       : body.startTime,
                                endTime         : body.endTime,
                                clipPath        : rewriteS3Url(body.url),
                                thumbnailPath   : rewriteS3Url(body.thumbnail?.url),
                                status          : "COMPLETE"
                        ]

                        def eventClipComplete = [
                                name           : "clip",
                                value          : "completed",
                                descriptionText: "Clip is completed",
                                data           : toJson(dataProxyURLComplete),
                                eventType      : "VIDEO",
                                displayed      : false,
                                isStateChange  : true
                        ]

                        sendEvent(name: "record", value: "record", displayed: false, isStateChange: true)

                        sendEvent(eventUpdateClipComplete)
                        sendEvent(eventClipComplete)
                        state.currentClipData = dataProxyURLComplete
                    }
                    else if (body.status == 'failed') { // CLIP failed

                        log.trace "Clip Failed: ${body.statusMessage}"

                        def dataClipFailed = [
                                hubId           : device.hub.id,
                                deviceId        : device.id,
                                correlationId   : body.correlationId,
                                captureTime     : body.captureTime,
                                startTime       : body.startTime,
                                endTime         : body.endTime,
                                thumbnailPath   : body.thumbnail?.url == "ready" ? rewriteS3Url(body.thumbnail?.url) : "http://cdn.device-icons.smartthings.com/camera/dlink-indoor@2x.png",
                                status          : "FAILED",
                                statusMessage : body.statusMessage?: "Clip Failed",
                        ]

                        def eventClipFailed = [
                                name           : "clip",
                                value          : "failed",
                                descriptionText: "Clip has failed",
                                data           : toJson(dataClipFailed),
                                eventType      : "VIDEO",
                                displayed      : false,
                                isStateChange  : true
                        ]

                        sendEvent(name: "record", value: "record", displayed: false, isStateChange: true)
                        sendEvent(eventClipFailed)
                        state.currentClipData = dataClipFailed
                    }
                }
                else if (body.bufferSize) { // CAMERA CALLBACK
                    // log.trace "parse(camera): $body"

                    sendEvent(name: "switch", value: body.status, isStateChange: true, displayed: false)

                    switch (body.status) {
                        case "on":
                            log.trace "on..."
                            sendEvent(name: "camera", value: body.status, isStateChange: true)
                            sendEvent(name: "profile", value: state.cameraProfile, displayed: false)
                            sendEvent(name: "record", value: "record", displayed: false, isStateChange: true)
                            sendEvent(name: "statusMessage", value: body.statusMessage?: "Active", displayed: false, isStateChange: true)
                            break
                        case "off":
                            log.trace "off..."
                            sendEvent(name: "camera", value: body.status, isStateChange: true)
                            sendEvent(name: "record", value: "unavailable", displayed: false, isStateChange: true)
                            sendEvent(name: "statusMessage", value: body.statusMessage?: "Inactive", displayed: false, isStateChange: true)
                            break
                        case "restarting":
                            log.trace "restarting..."
                            sendEvent(name: "camera", value: body.status, isStateChange: true, displayed: false)
                            sendEvent(name: "record", value: "unavailable", displayed: false, isStateChange: true)
                            sendEvent(name: "statusMessage", value: body.statusMessage?: "", displayed: false, isStateChange: true)
                            break
                        case "unavailable":
                            log.trace "unavailable..."
                            results << new physicalgraph.device.HubAction("lan discovery ${parent.getDeviceType()}", physicalgraph.device.Protocol.LAN)
                            schedule(now() + 15000, refresh)
                            sendEvent(name: "camera", value: body.status, isStateChange: true)
                            sendEvent(name: "record", value: "unavailable", displayed: false, isStateChange: true)
                            sendEvent(name: "statusMessage", value: body.statusMessage?: "", displayed: false, isStateChange: true)
                            break
                    }
                }
                else {
                    //log.trace "parse(etc): $body"
                }
            }
        }
    } catch (Throwable t) {
        sendEvent(name: "parseError", value: "$t", description: description, displayed: false)
        throw t
    }

    results

}

def getCameraProfile() {
    def profile = device.currentValue("profile")?: 2
    return profile
}

def uninstalled() {
    log.trace "D-Link Camera(uninstall): ${device.displayName}"
    deleteCamera(device.hub, device.id)
}

def updated() {
    log.trace "updated(): $device"
    parent.syncronizeCamTimes()
    configure()
}

def configure() {

    def action
    if (state.cameraSet){
        log.debug "configure(patch) "
        action = updateCamera()
    }
    else {
        log.debug "configure(post) "
        state.cameraSet = true
        action = setCamera()
    }
    action
}

def refresh() {
    def cameraState = "${device.currentValue("camera")}"
    log.trace "refresh(): camera= $cameraState"
    def actions = []

    actions << new physicalgraph.device.HubAction(
            method: "GET",
            path: "/cameras/${cameraId}",
            headers: [
                    "Content-Type": "application/json",
                    Connection    : "Close",
                    Host          : videoCoreUrl
            ]
    )

    switch(cameraState) {

        case "on":
            break
        case "off":
            break
        case "restarting":
            break
        case "unavailable":
            actions << new physicalgraph.device.HubAction("lan discovery ${parent.getDeviceType()}", physicalgraph.device.Protocol.LAN)
            schedule(now() + 30000, refresh)
            break
    }

    // log.trace actions

    actions
}

/**
 * Device Type Commands
 * */
def record20() {

    def time = now()
    def startTime = time - 10000
    def endTime = time + 10000

    log.debug "record(20): start: $startTime , capture: $time, end: $endTime"

    def ct = new Date(time)
    def st = new Date(startTime)
    def et = new Date(endTime)

    capture(st, ct, et)
}

def capture(startTime, captureTime, endTime, correlationId = UUID.randomUUID()) {

    // Disable simultaneous clip recordings
    if (startTime.getTime() > state.thresholdTime?:0) {
        state.thresholdTime = endTime.getTime()
        captureNaive(startTime, captureTime, endTime, correlationId)
    }
    else {
        log.warn "capture(ignored): startTime: $startTime is before thresholdTime: $state.thresholdTime"
        sendEvent(name: "record", value: "record", displayed: false, isStateChange: true)

        def data = state.currentClipData ?: [:]
        data.requestedCorrelationId = correlationId.toString()
        sendEvent(
                name           : "clip",
                value          : "ignored",
                descriptionText: "Clip recording is already happening",
                eventType      : "VIDEO",
                displayed      : false,
                isStateChange  : true,
                data           : data
        )
    }
}

def captureNaive(startTime, captureTime, endTime, correlationId = UUID.randomUUID()){

    // Request Clip Event
    def ct = convertToISO(captureTime)
    def st = convertToISO(startTime)
    def et = convertToISO(endTime)

    def dataInitiated = [
            hubId: device.hub.id,
            deviceId: device.id,
            correlationId : correlationId,
            captureTime: ct,
            startTime  : st,
            endTime    : et,
            imagePath: "http://s3.amazonaws.com/smartthings-device-icons/camera/loading.png",
            status     : "INITIATED"
    ]

    def eventInitiated  = [
            name           : "updateClip",
            value          : "initiated",
            descriptionText: "Clip is initialized",
            data           : toJson(dataInitiated),
            eventType      : "VIDEO",
            displayed      : false,
            isStateChange  : true
    ]

    //log.trace "updateClip initated(): $updateClipInitiated"
    sendEvent(eventInitiated)

    def clipInitiated = [
            name           : "clip",
            value          : "initiated",
            descriptionText: "Clip is initialized",
            data           : toJson(dataInitiated),
            eventType      : "VIDEO",
            displayed      : false,
            isStateChange  : true
    ]

    log.trace "clip initated(): $clipInitiated"
    sendEvent(clipInitiated)
    state.currentClipData = dataInitiated

    def body = [
            captureTime: ct,
            startTime  : st,
            endTime    : et,
            correlationId: correlationId,
            callbackUrl: hubCallbackUrl,
            oauthToken: parent.state.accessToken
    ]

    createClip(device.hub, device.id, body)
}

def on() {
    log.trace "on(camera): $cameraDni"

    def action = new physicalgraph.device.HubAction(
            method: "PATCH",
            path: "/cameras/${cameraId}",
            headers: [
                    "Content-Type": "application/json",
                    Connection    : "Close",
                    Host          : videoCoreUrl
            ],
            body: [
                    state : "on"
            ]
    )

    //log.debug action
    action
}

def off() {
    log.trace "off(camera): $cameraDni"

    def action = new physicalgraph.device.HubAction(
            method: "PATCH",
            path: "/cameras/$cameraId",
            headers: [
                    "Content-Type": "application/json",
                    Connection    : "Close",
                    Host          : videoCoreUrl
            ],
            body: [
                    state : "off"
            ]
    )

    //log.debug action
    action
}

def start() {
    log.trace "start(livestream): ${cameraDni}"

    def body = [streamClass: "hls2segs",
                oauthToken: "Bearer ${parent.state.accessToken}",
                inArgs: [hlsController: cameraId],
    ]

    def action = new physicalgraph.device.HubAction([
            method: "PUT",
            path: "/cameras/${cameraId}/streams/hls1080p",
            headers: [
                    "Content-Type": "application/json",
                    Connection    : "Close",
                    Host          : videoCoreUrl
            ],
            body: toJson(body)]
    )

    //log.debug action
    action
}

def stop() {
    log.trace "stop(livestream): ${cameraDni}"

    def action = new physicalgraph.device.HubAction([
            method: "DELETE",
            path: "/cameras/${cameraId}/streams/hls1080p",
            headers: [
                    "Content-Type": "application/json",
                    Connection    : "Close",
                    Host          : videoCoreUrl],
    ]

    )

    //log.debug action
    action
}

def setCamera() {

    //log.trace "setCamera(${cameraDni}): Password is ${cameraPassword} Profile is ${cameraProfile}"

    state.oldPassword = cameraPassword
    state.cameraPassword = cameraPassword
    state.cameraProfile = cameraProfile

    /*** New ***/
    state.rtspURL = getCameraRTSP(cameraAddressIP, cameraPassword, cameraProfile)
    state.cameraAudio =         cameraAudio
    state.cameraImage =         cameraImage
    state.cameraBrightness =    cameraBrightness
    state.cameraOverlay =       cameraOverlay

    sendEvent(name: "supportedProfiles", value: parent.getProfileOptions(), displayed: false)
    sendEvent(name: "profile", value: cameraProfile, displayed: false)

    def action = new physicalgraph.device.HubAction([
            method: "POST",
            path: "/cameras",
            headers: [
                    Host: videoCoreUrl,
                    "Content-Type": "application/json",
                    Connection: "Close"],
            body: [
                    state: "on",
                    cameraId: cameraId,
                    locationId: location.id,
                    dni: cameraDni,
                    url: getCameraRTSP(cameraAddressIP, cameraPassword, cameraProfile),
                    hlsTime: getHlsTime(),
                    seekAtStart: getSeekAtStart()
            ]
    ])
    log.debug "Adding camera: $cameraId"

    action
}

def updateCamera(camIP = cameraAddressIP, camProfile = settings.cameraProfile, isUpdatable = false) {

    state.cameraPassword= cameraPassword ?: state.cameraPassword
    state.cameraProfile = camProfile ?: cameraProfile
    state.rtspURL = getCameraRTSP(camIP, state.cameraPassword, state.cameraProfile)

    if (state.oldPassword != state.cameraPassword) {
        parent.setCameraPassword(camIP, state.cameraPassword, state.oldPassword) // password on camera
        isUpdatable = true
    }
    if (state.oldName != device.displayName) {
        parent.updateLabelFromChild(state.cameraPassword, camIP, device.displayName)
    }
    if (state.oldProfile != state.cameraProfile) {
        isUpdatable = true
    }
    if (state.cameraAudio != cameraAudio) {
        //log.debug 'Audio changed'
        parent.setCameraAudio(state.cameraPassword, camIP, cameraAudio)
    }
    if (state.cameraImage != cameraImage) {
        //log.debug 'Image changed'
        parent.setCameraImage(state.cameraPassword, camIP, cameraImage)
    }
    if (state.cameraBrightness != cameraBrightness) {
        //log.debug 'Brightness changed'
        parent.setCameraBrightness(state.cameraPassword, camIP, cameraBrightness)
    }
    if (state.cameraOverlay != cameraOverlay) {
        //log.debug 'Overlay changed'
        parent.setCameraOverlay(state.cameraPassword, camIP, cameraOverlay)
        parent.syncCameraDateTime(state.cameraPassword, camIP)
    }
    if (isUpdatable){
        log.debug "patching..."

        def body = [
                url : getCameraRTSP(camIP, state.cameraPassword, state.cameraProfile),
                state: "on"
        ]

        patchCamera(device.hub, cameraId, body)

    }

    // Update Previous States
    state.oldProfile        = state.cameraProfile
    state.oldPassword       = state.cameraPassword
    state.oldName           = settings.name



    state.cameraAudio       = cameraAudio
    state.cameraImage       = cameraImage
    state.cameraBrightness  = cameraBrightness
    state.cameraOverlay     = cameraOverlay

}

def setProfile(profile) {
    if (0 < profile && profile < 4){
        updateCamera(cameraAddressIP, profile, true)
    }
}

def setProfileHD() {
    setProfile(1)
}

def setProfileSDH() {
    setProfile(2)
}

def setProfileSDL() {
    setProfile(3)
}

/***
 * D-Link Camera Specific Methods
 */
def getCameraRTSP(cameraIP, cameraPassword, profile) {
    if (profile == 'null' || profile == null) {
        profile = 2
        sendEvent(name: "profile", value: profile, displayed: false)
    }
    return "rtsp://admin:${cameraPassword}@${cameraIP}/live${profile}.sdp"
}

def getSeekAtStart() {
    '5'
}

/**
 * Device Common Section
 * */

def getHlsTime() {
    '2'
}

def getHubLoopbackIP() {
    "127.0.0.1"
}

def getHubSrvPort() {
    device.hub.getDataValue('localSrvPortTCP') ?: "39500"
}

def getHubCallbackUrl() {
    "http://$hubLoopbackIP:$hubSrvPort/notifications"
}

def getCamSyncInfo() {
    settings.cameraPassword
}

def getCameraId() {
    device.id
}

def getCameraDni() {
    device.deviceNetworkId
}

def getCameraAddressIP() {
    device.getDataValue('ip')
}

Map getListFromBody(body) {
    def data = []
    def results = [:]
    def lines = body.readLines() //dlink returns data with lines
    lines.each() {
        data = it.split("=")    //get the name value pairs on each line
        if ( data.size() > 1 ) {
            def key = data[0].trim()
            def value = data[1].trim()
            results["${key}"] = value   //create the map entry
        } else {
            def key = data[0].trim()
            results["${key}"] = "" // incase there is no value for key
        }
    }
    results
}

/**
 * Device Utility Section
 * */
def convertToISO(time) {
    def t = time.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))
    return t
}

def toJson(Map m) {
    groovy.json.JsonOutput.toJson(m)
}

def rewriteS3Url(url) {
    if (url == null){
        return ""
    }
    else {
        URI vp = new URI(videoHostUrl)
        URI s3 = new URI(url)
        def s3Path = s3.getPath().split('/')
        URI st = new URI(vp.scheme, vp.userInfo, vp.host, 8400, '/' + s3Path[2..s3Path.size()-1].join('/'), s3.query, s3.fragment)
        st.toString()
    }
}

/**
 * Privilleged SmartApps can call methods on Devices
 */
def getAccessToken(){
    return parent.state.accessToken
}
