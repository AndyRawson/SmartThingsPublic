/**
 *
 *
 * Based on "SmartThings example Code for GroveStreams" by Jason Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 */

definition(
		name: "io.adafruit.com Logger",
		namespace: "rhworkshop",
		author: "Andy Rawson",
		description: "Log things to io.adafruit.com",
		category: "My Apps",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
		iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")

preferences {
	section("Log these things") {
            input "temperatures", "capability.temperatureMeasurement", title: "Temperatures", required:false, multiple: true
            input "humidities", "capability.relativeHumidityMeasurement", title: "Humidities", required: false, multiple: true
            input "contacts", "capability.contactSensor", title: "Doors open/close", required: false, multiple: true
            input "accelerations", "capability.accelerationSensor", title: "Accelerations", required: false, multiple: true
            input "motions", "capability.motionSensor", title: "Motions", required: false, multiple: true
            input "presence", "capability.presenceSensor", title: "Presence", required: false, multiple: true
            input "switches", "capability.switch", title: "Switches", required: false, multiple: true
            input "waterSensors", "capability.waterSensor", title: "Water sensors", required: false, multiple: true
            input "batteries", "capability.battery", title: "Batteries", required:false, multiple: true
            input "powers", "capability.powerMeter", title: "Power Meters", required:false, multiple: true
            input "energies", "capability.energyMeter", title: "Energy Meters", required:false, multiple: true

	}

	section ("adafruit.io AIO Key") {
		input "aioKey", "text", title: "AIO key"
	}
}

def installed() {
	initialize()
    createFeed()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
        subscribe(temperatures, "temperature", handleTemperatureEvent)
        subscribe(waterSensors, "water", handleWaterEvent)
        subscribe(humidities, "humidity", handleHumidityEvent)
        subscribe(contacts, "contact", handleContactEvent)
        subscribe(accelerations, "acceleration", handleAccelerationEvent)
        subscribe(motions, "motion", handleMotionEvent)
        subscribe(presence, "presence", handlePresenceEvent)
        subscribe(switches, "switch", handleSwitchEvent)
        subscribe(batteries, "battery", handleBatteryEvent)
        subscribe(powers, "power", handlePowerEvent)
        subscribe(energies, "energy", handleEnergyEvent)
}

def handleTemperatureEvent(evt) {
        sendValue(evt) { it.toString() }
}
 
def handleWaterEvent(evt) {
        sendValue(evt) { it == "wet" ? "1" : "0" }
}
 
def handleHumidityEvent(evt) {
        sendValue(evt) { it.toString() }
}
 
def handleContactEvent(evt) {
        sendValue(evt) { it == "open" ? "1" : "0" }
}
 
def handleAccelerationEvent(evt) {
        sendValue(evt) { it == "active" ? "1" : "0" }
}
 
def handleMotionEvent(evt) {
        sendValue(evt) { it == "active" ? "1" : "0" }
}
 
def handlePresenceEvent(evt) {
        sendValue(evt) { it == "present" ? "1" : "0" }
}
 
def handleSwitchEvent(evt) {
        sendValue(evt) { it == "on" ? "1" : "0" }
}
 
def handleBatteryEvent(evt) {
        sendValue(evt) { it.toString() }
}
 
def handlePowerEvent(evt) {
        sendValue(evt) { it.toString() }
}
 
def handleEnergyEvent(evt) {
        sendValue(evt) { it.toString() }
}

private sendValue(evt, Closure convert) {
	log.debug "Logging to adafruit.io ${evt.displayName.trim()}, ${evt.name} = ${convert(evt.value)}"

    try {         
		httpPost(uri: "https://io.adafruit.com/api/feeds/smartthings/data",
      			body: ["value": "{\"deviceid\": \"${evt.displayName.trim()}\",\"datatype\": \"${evt.name}\",\"value\": ${convert(evt.value)}}"
        		       ],
                        headers: ["X-AIO-Key": aioKey]
      			) {response -> log.debug (response.data)}
    }
 	catch (e) {
   		log.error "error: $e"
    }        
                
}

void createFeed() {
	log.debug "Creating the adafruit.io smartthings feed"
    try {         
		httpPostJson(uri: "https://io.adafruit.com/api/feeds.json",
      			body: ["name": "smartthings"
        		       ],
                        headers: ["X-AIO-Key": aioKey]
      			) {response -> log.debug (response.data)}
    }
 	catch (e) {
   		log.error "error: $e"
    }
}