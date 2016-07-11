/**
 *  Samsung SmartCam HD Pro
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

metadata {

    definition(name: "Samsung SmartCam", namespace: "samsung", author: "SmartThings") {

        capability "Configuration"
        capability "Video Camera"
        capability "Video Capture"
        capability "Refresh"
        capability "Switch"

        // custom commands
        command "record20"
        command "start"
        command "stop"
        command "setProfile360"
        command "setProfile720"
        command "setProfile1080"

        // custom attributes
        attribute "profile", "string"
        attribute "supportedProfiles", "json_object"
        attribute "record", "string"
        attribute "cameraFirmware", "string"
        attribute "manufacturer", "string"
        attribute "model", "string"
    }

    preferences {
        input "cameraPassword", "password", title: "Camera Password", required: true
        input "wlanSite", "text", title: "SSID", defaultValue: "",  multiple: false
        input "wlanPassword", "password", title: "SSID Password", defaultValue: ""
        input "wlanMode", "enum", title: "Secuirity Mode", options: ["None","WEP","PSK"] , multiple: false
        input "cameraTimeZone", "enum", options: timeZonesOptions, title: "Choose your time zone", multiple: false
        input "cameraAudio", "enum", title: "Mic Gain", options: ["Mute","Low/Med","Med/High","High"] , defaultValue: "${cameraAudio}" , multiple: false
        input "cameraImage", "enum", title: "Flip and Mirror", options: ["Mirror","Flip", "No Flip - No Mirror", "Flip - Mirror"], defaultValue: "${cameraImage}", multiple: false
        input "cameraBrightness", "enum", title: "Brightness", options: ["Low","Med","High"], defaultValue: "${cameraBrightness}" , multiple: false
        input "cameraDayNightMode", "enum", title: "Night Vision", options: ["Auto","Color"], defaultValue: "${cameraDayNightMode}", multiple: false
        input "cameraWDR", "enum", title: "Backlight Compensation", options: ["Off","WDR"], defaultValue: "${cameraWDR}",  multiple: false
        input "cameraOverlay", "enum", title: "Date Overlay", options: ["On","Off"], defaultValue: "${cameraOverlay}",  multiple: false
    }

    tiles(scale: 2) {

        multiAttributeTile(name: "videoPlayer", type: "videoPlayer", width: 6, height: 4){
            tileAttribute("device.switch", key: "CAMERA_STATUS") {
                attributeState("on", label: "Camera Active", icon: "st.camera.techwin", action: "switch.off", backgroundColor: "#79b821", defaultState: true)
                attributeState("off", label: "Camera Inactive", icon: "st.camera.techwin", action: "switch.on",  backgroundColor: "#ffffff")
                attributeState("restarting", label: "Connecting", icon: "st.camera.techwin", backgroundColor: "#53a7c0")
                attributeState("unavailable", label: "Unavailable", icon: "st.camera.techwin", action: "refresh.refresh", backgroundColor: "#F22000")
            }

            tileAttribute("device.camera", key: "PRIMARY_CONTROL") {
                attributeState("on", label: "Active", icon: "st.camera.techwin", backgroundColor: "#79b821", defaultState: true)
                attributeState("off", label: "Inactive", icon: "st.camera.techwin", backgroundColor: "#ffffff")
                attributeState("restarting", label: "Connecting", icon: "st.camera.techwin", backgroundColor: "#53a7c0")
                attributeState("unavailable", label: "Unavailable", icon: "st.camera.techwin", backgroundColor: "#F22000")
            }

            tileAttribute("device.startLive", key: "START_LIVE"){
                attributeState("live", action: "start", defaultState: true)
            }

            tileAttribute("device.stream", key: "STREAM_URL") {
                attributeState("activeURL", defaultState: true)
            }

            tileAttribute("device.profile", key: "STREAM_QUALITY"){
                attributeState("5", label: "1080p", action: "setProfile1080")
                attributeState("4", label: "720p", action: "setProfile720")
                attributeState("2", label: "360p", action: "setProfile360")
            }

            tileAttribute("device.betaLogo", key: "BETA_LOGO"){
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

        main "videoPlayer"
        details(["videoPlayer"])
    }
}

def parse(String description) {

    def results = [] // accumulate events to send out

    try {
        def msg = parseLanMessage(description)

        // CAMERA REDISCOVERY
        if (parent.isDeviceDiscoverable(msg?.ssdpTerm)){
            log.trace "parse(SSDP): $msg "
            parent.locationHandler(description)
        }
        // CAMERA MESSAGES
        else if (!msg.headers?."content-location"?.startsWith("/dev/")) {

            def status = msg.status
            if (status == 401) {
                log.info "parse(CAMERA): $msg.status"
                def auth = (msg.headers?."WWW-Authenticate".split(','))[1].split('=')[1].replace('"','')
                log.debug "Auth: ${auth}"
                state.nonce = auth
            }

            else if (status == 200) {
                log.info "parse(CAMERA): $msg.status"
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
                        log.trace "parse(stopping livestream): ${msg.headers}"
                        break
                }
            }
            else {
                def body = parseJson(msg.body)

                // LIVESTREAM CALLBACKS
                if (body.streamClass) {
                    log.trace "parse(live): $body"

                    def cookies = body?.cookies
                    def awsCookie = cookies.find { it?.key == "AWSELB" }

                    def dataLiveVideo = [
                            OutHomeURL : body?.src,
                            InHomeURL : body?.src, // Quick Fix for iOS 2.0.0 Bonjour bug
                            //InHomeURL : state.rtspURL,
                            ThumbnailURL: body?.thumbnail?.src ?: "http://cdn.device-icons.smartthings.com/camera/techwin@2x.png",

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
                    log.debug "parse(sync-clip): $body"
                    if (body.clip.status == 'Created') {
                        log.trace "Clip: ${body.clip.status}"
                    }
                }
                // CLIP CALLBACKS
                else if (body.captureTime) {
                    log.debug "parse(clip): $body"

                    if (body.status == 'initialized') { // CLIPS STREAMING
                        log.trace "Clip Initialized: ${body.status}"

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

                    }
                    else if (body.status == 'ready') { // MP4 READY

                        log.trace "Clip Complete: ${body.status}"

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
                    }
                }
                else if (body.bufferSize) { // CAMERA CALLBACK

                    sendEvent(name: "camera", value: body.status, isStateChange: true)
                    sendEvent(name: "switch", value: body.status, isStateChange: true, displayed: false)


                    switch (body.status) {
                        case "on":
                            log.trace "on..."
                            state.rtspURL = body.url
                            sendEvent(name: "profile", value: state.cameraProfile, displayed: false)
                            sendEvent(name: "record", value: "record", displayed: false, isStateChange: true)
                            break
                        case "off":
                            log.trace "off..."
                            sendEvent(name: "record", value: "unavailable", displayed: false, isStateChange: true)
                            break
                        case "restarting":
                            log.trace "restarting..."
                            sendEvent(name: "record", value: "unavailable", displayed: false, isStateChange: true)
                            break
                        case "unavailable":
                            log.trace "unavailable..."
                            results << new physicalgraph.device.HubAction("lan discovery ${parent.getDeviceTypes()[0]}", physicalgraph.device.Protocol.LAN)
                            results << new physicalgraph.device.HubAction("lan discovery ${parent.getDeviceTypes()[1]}", physicalgraph.device.Protocol.LAN)
                            schedule(now() + 30000, refresh)
                            sendEvent(name: "record", value: "unavailable", displayed: false, isStateChange: true)
                            break
                    }
                }
                else {
                    log.trace "parse(etc): $body"
                }
            }
        }

    } catch (Throwable t) {
        sendEvent(name: "parseError", value: "$t", description: description, displayed: false)
        throw t
    }

    results

}

def uninstalled() {
    log.trace "SmartCam Camera(uninstall): ${device.displayName}"
    deleteCamera(device.hub, device.id)
}


def updated() {
    log.trace "updated(): $device"
    configure()
}

def configure() {

    def action = []
    if (state.cameraSet){
        updateCamera()

    }
    else {
        state.cameraSet = true // TODO: Parse callback from video-core confirming camera is added
        setCamera()
    }

}

def refresh() {
    def cameraState = "${device.currentValue("camera")}"
    def actions= []
    log.trace "refresh: state= $cameraState"

    switch(cameraState) {

        case "on":
            log.debug "on"
            break
        case "off":
            log.debug "off"
            actions << on()
            break
        case "restarting":
            break
        case "unavailable":
            log.debug "unavailable"
            actions << new physicalgraph.device.HubAction("lan discovery ${parent.getDeviceTypes()[0]}", physicalgraph.device.Protocol.LAN)
            actions << new physicalgraph.device.HubAction("lan discovery ${parent.getDeviceTypes()[1]}", physicalgraph.device.Protocol.LAN)
            schedule(now() + 30000, refresh)
            break
    }

    log.trace actions
    actions
}

def query() {

    log.info "query()"


    def action = new physicalgraph.device.HubAction(
            method: "POST",
            path: "/device/detection/eventsubscription",
            headers: [
                    Host      : "${getCameraAddressIP()}:80",
                    Connection: "Close"],
            body: [
                    subscription_id: 7
            ]
    )

    action
}

/**
 * Video-Core Methods
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
        sendEvent(
                name           : "clip",
                value          : "ignored",
                descriptionText: "Clip recording is already happening",
                eventType      : "VIDEO",
                displayed      : false,
                isStateChange  : false
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

    def updateClipInitiated = [
            name           : "updateClip",
            value          : "initiated",
            descriptionText: "Clip is initialized",
            data           : toJson(dataInitiated),
            eventType      : "VIDEO",
            displayed      : false,
            isStateChange  : true
    ]

    log.trace "updateClip initated(): $updateClipInitiated"
    sendEvent(updateClipInitiated)

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

    log.debug action
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

    log.debug action
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

    log.debug action
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
            body: data]

    )

    log.debug action
    action
}

def setCamera() {

    log.trace "setCamera(${cameraDni}): Profile is ${cameraProfile}"

    state.oldPassword = cameraPassword
    state.cameraPassword = cameraPassword
    state.cameraProfile = cameraProfile

    /*** New ***/
    state.rtspURL = getCameraRTSP(cameraAddressIP, cameraPassword, cameraProfile)
    state.cameraNetwork = cameraNetwork
    state.cameraAudio = cameraAudio
    state.cameraTimeZone = cameraTimeZone
    state.cameraImage =  cameraImage
    state.cameraBrightness =  cameraBrightness
    state.cameraOverlay =  cameraOverlay
    state.cameraDayNightMode = cameraDayNightMode
    state.cameraWDR = cameraWDR
    state.wlanSite = wlanSite
    state.wlanPassword = wlanPassword
    state.wlanMode = wlanMode

    sendEvent(name: "supportedProfiles", value: parent.getProfileOptions(), displayed: false)
    sendEvent(name: "cameraFirmware", value: parent.getCameraFirmware(), displayed: false)
    sendEvent(name: "manufacturer", value: parent.getCameraManufacturer(), displayed: false)
    sendEvent(name: "model", value: parent.getCameraModel(), displayed: false)
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

    parent.checkCameraForPassword(cameraAddressIP, state.oldPassword, state.nonce)
    action
}

def updateCamera(camIP = cameraAddressIP, camProfile = cameraProfile, isUpdatable = false) {

    state.cameraPassword = cameraPassword ?: state.cameraPassword
    state.cameraProfile = camProfile ?: cameraProfile
    state.rtspURL = getCameraRTSP(camIP, state.cameraPassword, state.cameraProfile)


    if (state.oldPassword != state.cameraPassword) {
        isUpdatable = true
        def flag = true
        parent.setCameraPassword(state.cameraPassword, camIP, state.oldPassword, flag, state.nonce )
    }
    if (state.cameraAudio != cameraAudio) {
        log.debug 'Audio changed'
        parent.setCameraAudio(state.cameraPassword, camIP, cameraAudio, state.nonce)
    }
    if (state.cameraTimeZone != cameraTimeZone) {
        log.debug 'Time zone changed'
        parent.setTimeZones(state.cameraPassword, camIP, cameraTimeZone, state.nonce)
    }
    if (state.cameraImage != cameraImage) {
        log.debug 'Image changed'
        parent.setCameraImage(state.cameraPassword, camIP, cameraImage, state.nonce)
    }
    if (state.cameraBrightness != cameraBrightness) {
        log.debug 'Brightness changed'
        parent.setCameraBrightness(state.cameraPassword, camIP, cameraBrightness, state.nonce)
    }
    if (state.cameraOverlay != cameraOverlay) {
        log.debug 'Overlay changed'
        parent.setCameraOverlay(state.cameraPassword, camIP, cameraOverlay, state.nonce)

    }
    if (state.cameraDayNightMode != cameraDayNightMode || state.cameraWDR != cameraWDR) {
        log.debug 'WDR or Night Vision changed'
        parent.setCameraWDR(state.cameraPassword, camIP, cameraWDR, cameraDayNightMode, state.nonce)
    }
    if (state.wlanSite != wlanSite || state.wlanPassword != wlanPassword || state.wlanMode != wlanMode) {
        log.debug 'Network Changed'
        parent.setWirelessNetwork(settings.cameraPassword, camIP, wlanSite, wlanPassword, wlanMode, state.nonce)
    }

    if (isUpdatable){
        log.debug "patching..."

        def body = [
                url : getCameraRTSP(camIP, state.cameraPassword, state.cameraProfile),
                state: "on"
        ]

        patchCamera(device.hub, cameraId, body)

    }

    state.oldPassword = state.cameraPassword


    /*** New ***/
    state.cameraNetwork = cameraNetwork
    state.cameraAudio = cameraAudio
    state.cameraTimeZone = cameraTimeZone
    state.cameraImage =  cameraImage
    state.cameraBrightness =  cameraBrightness
    state.cameraDayNightMode = cameraDayNightMode
    state.cameraWDR = cameraWDR
    state.cameraOverlay = cameraOverlay
    state.wlanSite = wlanSite
    state.wlanPassword = wlanPassword
    state.wlanMode = wlanMode

    sendEvent(name: "cameraFirmware", value: parent.getCameraFirmware(), displayed: false)
    sendEvent(name: "profile", value: state.cameraProfile, displayed: false)

}

def getCameraRTSP(cameraIP, cameraPassword, profile) {
    return "rtsp://admin:${cameraPassword}@${cameraIP}:554/profile${profile}/media.smp"
}

def setProfile360() {
    updateCamera(cameraAddressIP, 2, true)
}

def setProfile720() {
    updateCamera(cameraAddressIP, 4, true)
}

def setProfile1080() {
    updateCamera(cameraAddressIP, 5, true)
}

def getSeekAtStart() {
    null
}

/**
 * CAMERA COMMON SECTION
 * Helper methods that are common to all camera device-types
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

def getCameraId() {
    device.id
}

def getCameraDni() {
    device.deviceNetworkId
}

def getCameraAddressIP() {
    device.getDataValue('ip')
}

def getCameraProfile() {
    def profile =  device.currentValue("profile")?: 2
    return profile
}

def convertToISO(time) {
    def t = time.format("yyyy-MM-dd'T'HH:mm:ss'Z'", TimeZone.getTimeZone("UTC"))
    return t
}

def rewriteS3Url(url = "") {
    def tokens = url.tokenize('/')
    if (tokens.size() != 7) {
        log.warn "Unknown format for url: $url"
        return ""
    }
    def newUrl = videoHostUrl + tokens[3] + "/" + tokens[4] + "/" + tokens[5] + "/" + tokens[6];
    newUrl
}
/**
 * Privilleged SmartApps can call methods on Devices
 */
def getAccessToken(){
    return parent.state.accessToken
}
def getVideoHostUrl() {
    def dmServerUrl = getApiServerUrl()
    if (dmServerUrl.startsWith("https://dgraph")) {
        return "https://dvh.connect.smartthings.com:8400/"
    } else if (dmServerUrl.startsWith("https://sgraph")) {
        return "https://svh.connect.smartthings.com:8400/"
    } else {
        return "https://vh.connect.smartthings.com:8400/"
    }
}

/**
 * Device Utility Section
 * */

def toJson(Map m) {
    groovy.json.JsonOutput.toJson(m)
}

def getTimeZonesOptions() {
    return [1: "(GMT-11:00) Coordinated Universal Time-11",
            2: "(GMT-10:00) Hawaii",
            3: "(GMT-09:00) Alaska",
            4: "(GMT-08:00) Pacific Time (US & Canada)",
            7: "(GMT-07:00) Mountain Time (US & Canada)",
            8: "(GMT-07:00) Arizona",
            11: "(GMT-06:00) Central Time (US & Canada)",
            13: "(GMT-05:00) Eastern Time (US & Canada)",
            17: "(GMT-04:00) Atlantic Time (Canada)",
            22: "(GMT-03:30) Newfoundland",
            33: "(GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London"]

}
