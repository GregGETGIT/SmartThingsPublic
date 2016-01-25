/**
 *  BatteryMonitor SmartApp for SmartThings
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
 *  This SmartApp helps you monitor the status of your SmartThings devices with batteries.
 *
 *  Install Steps
 *  ----------------
 *  1. Create new SmartApps at https://graph.api.smartthings.com/ide/apps using the SmartApps at https://github.com/notoriousbdg/SmartThings.BatteryMonitor.
 *  2. Install the newly created SmartApp in the SmartThings mobile application.
 *  3. Follow the prompts to configure.
 *  4. Tap Status to view battery level for all devices.
 *
 *
 *  The latest version of this file can be found at:
 *    https://github.com/notoriousbdg/SmartThings.BatteryMonitor
 *
 */

definition(
    name: "Temperature Monitor",
    namespace: "Harper",
    author: "me",
    description: "SmartApp to monitor temperature levels.",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
    page name:"pageStatus"
    page name:"pageConfigure"
}

// Show Status page
def pageStatus() {
    def pageProperties = [
        name:       "pageStatus",
        title:      "TemperatureMonitor Status",
        nextPage:   null,
        install:    true,
        uninstall:  true
    ]

    if (settings.devices == null) {
        return pageConfigure()
    }
    
	def listLevel0 = ""
    def listLevel1 = ""
    def listLevel2 = ""
    def listLevel3 = ""
    def listLevel4 = ""

	if (settings.level1 == null) { settings.level1 = 33 }
	if (settings.level3 == null) { settings.level3 = 67 }
	if (settings.pushMessage) { settings.pushMessage = true }
    
	return dynamicPage(pageProperties) {
		settings.devices.each() {
			try {
                if (it.currentTemperature == null) {
                    listLevel0 += "$it.displayName\n"
                } else if (it.currentTemperature >= 0 && it.currentTemperature <  settings.level1.toInteger()) {
                    listLevel1 += "$it.currentTemperature  $it.displayName\n"
                } else if (it.currentTemperature >= settings.level1.toInteger() && it.currentTemperature <= settings.level3.toInteger()) {
                    listLevel2 += "$it.currentTemperature  $it.displayName\n"
                } else if (it.currentTemperature >  settings.level3.toInteger() && it.currentTemperature < 100) {
                    listLevel3 += "$it.currentTemperature  $it.displayName\n"
                } else if (it.currentTemperature == 100) {
                    listLevel4 += "$it.displayName\n"
                } else {
                    listLevel0 += "$it.currentTemperature  $it.displayName\n"
                }
            } catch (e) {
            	log.trace "Caught error checking temperature status."
                log.trace e
                listLevel0 += "$it.displayName\n"
            }
        }

        if (listLevel0) {
            section("Devices with errors or no status") {
                paragraph listLevel0.trim()
            }
		}
        
        if (listLevel1) {
        	section("Devices with low temperature (less than $settings.level1)") {
            	paragraph listLevel1.trim()
            }
        }

        if (listLevel2) {
            section("Devices with medium temperature (between $settings.level1 and $settings.level3)") {
                paragraph listLevel2.trim()
            }
        }

        if (listLevel3) {
            section("Devices with high temperature (more than $settings.level3)") {
                paragraph listLevel3.trim()
            }
        }

        if (listLevel4) {
            section("Devices on fire") {
                paragraph listLevel4.trim()
            }
        }

        section("Menu") {
            href "pageStatus", title:"Refresh", description:"Tap to refresh"
            href "pageConfigure", title:"Configure", description:"Tap to open"
        }
    }
}

// Show Configure Page
def pageConfigure() {
    def helpPage =
        "Select devices that show temperature that you wish to monitor."

    def inputTemperature = [
        name:           "devices",
        type:           "capability.temperatureMeasurement",
        title:          "Which devices with temperature?",
        multiple:       true,
        required:       true
    ]

    def inputLevel1 = [
        name:           "level1",
        type:           "number",
        title:          "Low temperature threshold?",
        defaultValue:   "33",
        required:       true
    ]

	def inputLevel3 = [
        name:           "level3",
        type:           "number",
        title:          "Medium temperature threshold?",
        defaultValue:   "67",
        required:       true
    ]

    def pageProperties = [
        name:           "pageConfigure",
        title:          "TemperatureMonitor Configuration",
        nextPage:       "pageStatus",
        uninstall:      true
    ]

	return dynamicPage(pageProperties) {
        section("About") {
            paragraph helpPage
        }

		section("Devices") {
            input inputTemperature
        }
        
        section("Settings") {
            input inputLevel1
            input inputLevel3
        }

        section("Notification") {
        	input("recipients", "contact", title: "Send notifications to") {
            	input(name: "sms", type: "phone", title: "Send A Text To", description: null, required: false)
            	input(name: "pushNotification", type: "bool", title: "Send a push notification", description: null, defaultValue: false)
        	}
    	}

		section("Minimum time between messages (optional)") {
			input "frequency", "decimal", title: "Minutes", required: false
		}

        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

def installed() {
    initialize()
}

def updated() {
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(devices, "temperature", temperatureHandler)
	state.lowTempNoticeSent = [:]

	runIn(60, updateTemperatureStatus)
}

def send(msg) {
	if (frequency) {
	    if (location.contactBookEnabled) {
	        sendNotificationToContacts(msg, recipients)
	    }
	    else {
	        if (sms) {
	            sendSms(sms, msg)
	        }
	        if (pushNotification) {
	            sendPush(msg)
	        }
	    }
    }
    else {
	    if (location.contactBookEnabled) {
	        sendNotificationToContacts(msg, recipients)
	    }
	    else {
	        if (sms) {
	            sendSms(sms, msg)
	        }
	        if (pushNotification) {
	            sendPush(msg)
	        }
	    }
    }
}

def updateTemperatureStatus() {
    settings.devices.each() {
        try {
            if (it.currentTemperature == null) {
                if (!state.lowTempNoticeSent.containsKey(it.id)) {
                    send("${it.displayName} temperature is not reporting.")
                    state.lowTempNoticeSent[(it.id)] = true
                }
            } else if (it.currentTemperature > 100) {
                if (!state.lowTempNoticeSent.containsKey(it.id)) {
                    send("${it.displayName} temperature is ${it.currentTemperature}, which is over 100.")
                    state.lowTempNoticeSent[(it.id)] = true
                }
            } else if (it.currentTemperature < settings.level1) {
                if (!state.lowTempNoticeSent.containsKey(it.id)) {
                    send("${it.displayName} temperature is ${it.currentTemperature} (threshold ${settings.level1}.)")
                    state.lowTempNoticeSent[(it.id)] = true
                }
            } else {
                if (state.lowTempNoticeSent.containsKey(it.id)) {
                    state.lowTempNoticeSent.remove(it.id)
                }
            }
        } catch (e) {
            log.trace "Caught error checking temperature status."
            log.trace e
            if (!state.lowTempNoticeSent.containsKey(it.id)) {
                    send("${it.displayName} temperature reported a non-integer level.")
                    state.lowTempNoticeSent[(it.id)] = true
            }
        }
    }
}


def temperatureHandler(evt) {
	updateTemperatureStatus()
}