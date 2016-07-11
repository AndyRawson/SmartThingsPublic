/**
 *  FrankenSwitch Device Type for SmartThings
 *
 *  Copyright (c) 2014 Brandon Gordon (https://github.com/notoriousbdg)
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
 *  Overview
 *  ----------------
 *  The FrankenSwitch Device Type allows for easier integration between devices that expect 
 *  contact, presence, or motion sensors when the only input available is a switch.  There is a toggle on the device screen that can 
 *  be used to change the behaviour of the sensors between normal, reversed, and disabled.  The first use case is with Tasker 
 *  integration on Android to allow commands to be sent to a switch but SmartApps can subscribe to the status of contact, presence, 
 *  or motion sensors.  The second is with Harmony Remote integration that will allow a switch to be changed while simultaneously 
 *  changing the status of contact, presence, or motion sensors.
 *  
 *  Install Steps
 *  ----------------
 *  1. Create a new Device Type at https://graph.api.smartthings.com/ide/devices using the SmartApps at https://github.com/notoriousbdg/SmartThings.FrankenSwitch.
 *  2. Create a new device at https://graph.api.smartthings.com/device/list using the Device Type created in step 1.
 *
 *  Revision History
 *  ----------------
 *  2014-10-14  v0.0.0  Forked from http://thinkmakelab.com/2014/06/11/how-to-create-a-virtual-switch-in-smartthings/
 *  2014-10-13  v0.0.1  Initial release
 *  2014-10-13  v0.0.2  Added polling and actuator capability
						Made main tile icon changeable
						Cleaned up tile definition
 *
 *  The latest version of this file can be found at:
 *    https://github.com/notoriousbdg/SmartThings.FrankenSwitch
 *
 */

metadata {
		definition(
			name: "FrankenSwitch",
			namespace: "notoriousbdg",
			author: "Brandon Gordon") {

			capability "Actuator"
			capability "Switch"
			capability "Polling"
			capability "Refresh"        
			capability "Contact Sensor"
			capability "Motion Sensor"
			capability "Presence Sensor"
			capability "Sensor"

	        command "normal"
	        command "reverse"
	        command "disable"
            
			attribute "invert", "string"
		}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: true, canChangeBackground:true) {
			state "off", label: 'Off', action: "switch.on", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "on"
			state "on", label: 'On', action: "switch.off", icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "off"
		}
        standardTile("contact", "device.contact", canChangeIcon: true, canChangeBackground:true) {
            state "open", label: '${name}', icon: "st.contact.contact.open", backgroundColor: "#ffa81e"
            state "closed", label: '${name}', icon: "st.contact.contact.closed", backgroundColor: "#79b821"
        }
        standardTile("presence", "device.presence", canChangeIcon: true, canChangeBackground:true) {
            state "present", labelIcon:"st.presence.tile.present", backgroundColor:"#53a7c0"
            state "not present", labelIcon:"st.presence.tile.not-present", backgroundColor:"#ffffff"
        }
	    standardTile("motion", "device.motion", canChangeIcon: true, canChangeBackground:true) {
		    state "active", label:'motion', icon:"st.motion.motion.active", backgroundColor:"#53a7c0"
		    state "inactive", label:'no motion', icon:"st.motion.motion.inactive", backgroundColor:"#ffffff"
	    }
		standardTile("invert", "device.invert") {
			state "normal",  label: '${name}', action: "reverse", icon: "st.Home.home30", backgroundColor: "#79b821", nextState: "reverse"
			state "reverse", label: '${name}', action: "disable", icon: "st.Home.home30", backgroundColor: "#ffa81e", nextState: "disable"
			state "disable",  label: '${name}', action: "normal", icon: "st.Home.home30", backgroundColor: "#ffffff", nextState: "normal"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:'', action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		main(["switch", "contact", "presence", "motion"])
		details(["switch", "contact", "presence", "motion", "invert", "refresh"])
	}

	// simulator metadata
	simulator {
        status "on"             : "switch:open"
        status "off"            : "switch:closed"
	}
}

def parse(String description) {
}

def on() {
	sendEvent(name: "switch",   value: "on")

	if (device.latestValue('invert') == null) {
    	sendEvent(name: "invert",   value: "normal")
    }
    
    if (device.latestValue('invert') == "normal") {
    	sendEvent(name: "contact",  value: "open")
        sendEvent(name: "presence", value: "present")
        sendEvent(name: "motion",   value: "active")
    } else if (device.latestValue('invert') == "reverse") {
        sendEvent(name: "contact",  value: "closed")
        sendEvent(name: "presence", value: "not present")
        sendEvent(name: "motion",   value: "inactive")
    } else {
    	sendEvent(name: "invert",   value: "normal")
    }
}

def off() {
	sendEvent(name: "switch",   value: "off")

	if (device.latestValue('invert') == null) {
    	sendEvent(name: "invert",   value: "normal")
    }
    
	if (device.latestValue('invert') == "normal") {
        sendEvent(name: "contact",  value: "closed")
        sendEvent(name: "presence", value: "not present")
        sendEvent(name: "motion",   value: "inactive")
    } else if (device.latestValue('invert') == "reverse") {
        sendEvent(name: "contact",  value: "open")
        sendEvent(name: "presence", value: "present")
        sendEvent(name: "motion",   value: "active")
    } else {
    	sendEvent(name: "invert",   value: "normal")
    }
}

def poll() {
	if (device.latestValue('switch') == null) {
    	off
    }
	if (device.latestValue('invert') == null) {
    	sendEvent(name: "invert",   value: "normal")
    }
}

def refresh() {
	poll
}

def normal() {
    def invert = "normal"
    sendEvent(name: "invert",   value: invert)
    
    if (device.latestValue('switch') == "on") {
    	sendEvent(name: "contact",  value: "open")
        sendEvent(name: "presence", value: "present")
        sendEvent(name: "motion",   value: "active")
    } else {
        sendEvent(name: "contact",  value: "closed")
        sendEvent(name: "presence", value: "not present")
        sendEvent(name: "motion",   value: "inactive")
    }
}

def reverse() {
    def invert = "reverse"
    sendEvent(name: "invert",   value: invert)

	if (device.latestValue('switch') == "on") {
        sendEvent(name: "contact",  value: "closed")
        sendEvent(name: "presence", value: "not present")
        sendEvent(name: "motion",   value: "inactive")
    } else {
    	sendEvent(name: "contact",  value: "open")
        sendEvent(name: "presence", value: "present")
        sendEvent(name: "motion",   value: "active")
    }
}

def disable() {
    def invert = "disable"
    sendEvent(name: "invert",   value: invert)

	sendEvent(name: "contact",  value: "closed")
    sendEvent(name: "presence", value: "not present")
    sendEvent(name: "motion",   value: "inactive")
}