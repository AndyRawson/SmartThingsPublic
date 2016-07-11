/**
 *  Raspberry Pi GPIO
 *
 *  Copyright 2015 Andy Rawson
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
	definition (name: "Raspberry Pi GPIO", namespace: "rhworkshop", author: "Andy Rawson") {
		capability "Motion Sensor"
        capability "Refresh"
        capability "Polling"

		command "testCommand"
        command "subscribe"
        command "refresh"
	}
    
    preferences {
    input "rpiIP", "text", title: "IP Address", description: "The Raspberry Pi IP Address.", defaultValue: "192.168.0.100", required: true, displayDuringSetup: true
	input "rpiPort", "text", title: "Port", description: "The Raspberry Pi Port.", defaultValue: "1880", required: true, displayDuringSetup: true
}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
        standardTile("motion", "device.motion", width: 2, height: 2) {
			state("active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0")
			state("inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff")
		}
		standardTile("refresh", "device.motion", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}

		main "motion"
		details (["motion", "refresh"])
        
	}
}

// parse events into attributes
def parse(description) {
	log.debug "Parsing '${description}'"
	def msg = parseLanMessage(description)
    def body = msg.body
    log.debug "msg.body '${body}'"
    //subscribeAction("api/rpi/236/pins?pin=36")

}

def poll() {

    refresh()
}

mappings {
      path("/test") {
        action: [
          GET: "listMethod",
          PUT: "updateMethod"
        ]
      }
}


def listMethod() {
log.debug "test listMethod"
}

// handle commands
def testCommand() {
	log.debug "Executing 'testCommand'"
	// TODO: handle 'testCommand' command
}


def refresh() {
    def host = rpiIP 
    def hosthex = convertIPtoHex(host)
    def porthex = convertPortToHex(rpiPort)
    device.deviceNetworkId = "$hosthex:$porthex" 
log.debug "Refresh"
subscribe()
getPinStatus()
}

private subscribeAction(path, callbackPath="") {

    def address = device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
    log.debug "address: '${address}'"
    def parts = device.deviceNetworkId.split(":")
    def ip = convertHexToIP(parts[0])
    def port = convertHexToInt(parts[1])
    ip = ip + ":" + port

	log.debug address

	def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: path,
        headers: [
            HOST: ip
            ])
    return result
}


def getPinStatus() {
    def result = new physicalgraph.device.HubAction(
        method: "GET",
        path: "/api/subscribe",
        headers: [
            HOST: getHostAddress()
        ]
    )
    return result
}


// gets the address of the hub
private getCallBackAddress() {
    return device.hub.getDataValue("localIP") + ":" + device.hub.getDataValue("localSrvPortTCP")
}


def subscribe() {
log.debug "Subscribing..."
    subscribeAction("/api/subscribe")
    log.debug "Subscribe ran"
}



private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address entered is $ipAddress and the converted hex code is $hex"
    return hex

}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04x', port.toInteger() )
    log.debug hexport
    return hexport
}



private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


private String convertHexToIP(hex) {
	log.debug("Convert hex to ip: $hex") 
	[convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}

private getHostAddress() {
	def parts = device.deviceNetworkId.split(":")
    log.debug device.deviceNetworkId
	def ip = convertHexToIP(parts[0])
	def port = convertHexToInt(parts[1])
	return ip + ":" + port
}