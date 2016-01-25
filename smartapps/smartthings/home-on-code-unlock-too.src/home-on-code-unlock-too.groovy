/**
 *  Home Mode on Code Unlock Too
 *
 *  Copyright 2014 Barry A. Burke
 * 
 *  Changelog
 *		2014/08/28		Added keyed unlock support, optionally with separate Hello Home! action
 *						Don't run Hello Home! Actions if any specified people are present (fix presence handling also)
 *		2014/08/31		Fixed a typo, added options to send distress/Mayday via SMS, Push and/or NotificationEvent
 * 		2014/09/12		Don't run the "lock" actions if the house is already away (ie, ignore door being locked by the
 * 						Hello Home "Goodbye" action)
 * 		2014/09/20		Changed app icon
 * 		2014/09/23		Added sendPush() and sendSMS() options
 *		2014/09/26		Fixes silent crash caused by SendSms typo
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
    name: "Home on Code Unlock Too",
    namespace: "smartthings",
    author: "Barry A. Burke",
    description: "Change Hello, Home! mode when door is unlocked with a code. Optionally identify the person, send distress message, and/or return to Away mode on departure.",
    category: "Mode Magic",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-LightUpMyWorld@2x.png"
)

import groovy.json.JsonSlurper

preferences {
	page(name: "setupApp")
    page(name: "usersPage")
}

def setupApp() {
    dynamicPage(name: "setupApp", title: "Configure your code and phrases.", install: false, uninstall: true, nextPage: "usersPage") {

		section("What Lock?") {
			input "lock1","capability.lock", title: "Lock"
    	}
        section("How many User Names (1-30)?") {
        	input name: "maxUserNames", title: "# of users", type: "number", required: true, multiple: false,  refreshAfterSelection: true
        }
        section("Don't run Actions if any of these are present:") {
        	input "presence1", "capability.presenceSensor", title: "Who?", multiple: true, required: false
        }
        
      	section("Controls") {
        	input name: "anonymousAllowed", title: "Allow Unspecified Code IDs?", type: "bool", defaultValue: false, refreshAfterSelection: true
         	input name: "autoLock", title: "Auto-lock Unspecified Code IDs?", type: "bool", defaultValue: false
            input name: "manualUnlock", title: "Change Mode on Manual/Keyed Unlock also?", type: "bool", defaultValue: false, refreshAfterSelection: true
            if (manualUnlock) {
            	input name: "manualUnlockException", title: "Use separate Hello Home action for Manual/Keyed Unlock?", type: "bool", defaultValue: false, refreshAfterSelection: true
			}
        }
        section("Distress Alerts") {
			input name: "enableDistressCode", title: "Enable Distress Code?", type: "bool", defaultValue: false, refreshAfterSelection: true
            if (enableDistressCode) {
            	input name: "distressCode", title: "Distress Code ID# (1-${maxUserNames})", type: "number", defaultValue: 0, multiple: false, refreshAfterSelection: true
				if ((distressCode > 0) && (distressCode <= maxUserNames)) {
                	input name: "smsMayday", type: "bool", title: "Send distress message via SMS?", defaultValue: false, refreshAfterSelection: true
                    if (smsMayday) {
    					input name: "phone1", type: "phone", title: "Phone number to send message to", required: true
                    }
                    input name: "pushMayday", type: "bool", title: "Push notification to ST mobile devices?", defaultValue: false
                	input name: "notifyAlso", type: "bool", title: "Send ST Notification also?", defaultValue: false
    				input name: "distressMsg", type: "string", title: "Message to send", defaultValue: "Mayday! at ${location.name} - ${lock1.displayName}"
                }
            }
		}
		
//		section("Smart Alarm integration") {
//			input name: "linkWithSmartAlarm", type: "bool", title: "Link with Smart Alarm?", defaulValuet: false, refreshAfterSelection
//			if (linkWithSmartAlarm) {
//					input name: "saRestEndPoint" title: "Smart Alarm's restEndPoint", type: "text", required: true
//					input name: "saAccessToken", title: "Smart Alarm's accessToken", type: "text", required: true
//					paragraph ""
//					input name: "alarmOnMayday", type: "bool", title: "Alarm on Distress Entry?", defaultValue: true
//					input name: "disarmOnEntry", type: "bool", title: "Disarm on door unlock?", defaultValue: false
//					input name: "armOnExit", type: "bool", title: "Arm on door lock?", defaultValue: false
//			}
//		}
            
    	def phrases = location.helloHome?.getPhrases()*.label
    	if (phrases) {
       		phrases.sort()
			section("Hello Home actions...") {
				input name: "homePhrase", type: "enum", title: "Coded Home Mode Phrase", defaultValue: "I'm Back!", required: true, options: phrases, refreshAfterSelection: true
                if (manualUnlockException) {
                	input name: "manualPhrase", type: "enum", title: "Manual/Keyed Home Mode Phrase", defaultValue: "I'm Back!", required: true, options: phrases, refreshAfterSelection: true
                }
				input name: "awayPhrase", type: "enum", title: "Away Mode Phrase", defaultValue: "Goodbye!", required: true, options: phrases, refreshAfterSelection: true
        	}        
		}
		
		section("Notifications") {
			input name: "hhNotifyOnly", title: "Hello, Home notification ONLY", type: "bool", defaultValue: true, refreshAfterSelection: true
			if (!hhNotifyOnly) {
				input name: "stPush", title: "Send Push notification", type: "bool", defaultValue: false
				if (phone1) {
					input name: "stSMS", title: "Send SMS notification to:", type: "phone", defaultValue: "${phone1}"
				}
				else {
					input name: "stSMS", title: "Send SMS notification to:", type: "phone"
				}
			}	
		}
		
        section([mobileOnly:true]) {
			label title: "Assign a name for this SmartApp", required: false
			mode title: "Set for specific mode(s)", required: false
		}
	}
}

def usersPage() {
	dynamicPage(name:"usersPage", title: "User / Code List", uninstall: true, install: true) {
    
		section("User Names/Identifiers (1-${maxUserNames})") {
        	for (int i = 1; i <= settings.maxUserNames; i++) {
            	def priorName = settings."userNames${i}"
            	if (priorName) {
                	input name: "userNames${i}", description: "${priorName}", title: "Code ID#$i Name", defaultValue: "${priorName}", type: string, multiple: false, required: false
				}
                else {
					input name: "userNames${i}", description: "Tap to set", title: "Code ID#$i Name", type: string, multiple: false, required: false
                }
            }
        }       
	} 
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize()
{
    log.debug "Settings: ${settings}"
    subscribe(lock1, "lock", doorHandler)
    state.lastUser = ""
    state.lastLockStatus = lock1.latestValue('lock')
}


def doorHandler(evt)
{
    log.debug "The ${lock1.displayName} lock is ${lock1.latestValue('lock')}"
	def data = []

	if (evt.name == "lock") {
    
    	if (evt.value == "unlocked") {
        
        	state.lastLockStatus = "unlocked"
            
            def isManual = false
	    	if ((evt.data == "") || (evt.data == null)) {  				// No extended data, must be a manual/keyed unlock
            	isManual = true
            }
            else {														// We have extended data, should be a coded unlock           	
	    		data = new JsonSlurper().parseText(evt.data) 
            	if ((data.usedCode == "") || (data.usedCode == null)) {	// If no usedCode data, treat as manual unlock
                	log.debug "Unknown extended data (${data}), treating as manual unlock"
                	isManual = true
           		 }
            }
            
            if (isManual) {           	
                if (manualUnlock) { 									// We're supposed to handle manual/keyed unlocks also
                	      
             		if (anyoneIsHome() ) {
                		log.debug "Manual/keyed unlock but someone is already present, no Action taken"
                		return
            		}
                    
                    if (manualUnlockException) {
                    	log.debug "${lock1.displayName} was manually unlocked - Someone is Home!"
    	    			notify("Running \"${settings.manualPhrase}\" because ${lock1.displayName} was manually unlocked by someone.")
						location.helloHome.execute(settings.manualPhrase)
                    }
                    else {
                    	log.debug "${lock1.displayName} was manually unlocked - Someone is Home!"
    	    			notify("Running \"${settings.homePhrase}\" because ${lock1.displayName} was manually unlocked by someone.")
						location.helloHome.execute(settings.homePhrase)
                    }
                    state.lastUser = "Someone"
				}
         	}
            else {														// Wasn't manual and we have a usedCode	
            
            	Integer i = data.usedCode as Integer
                log.debug "Unlocked with code ${i}"
                
				if (enableDistressCode) {	
					if(i == distressCode) {
        				log.info "Sending Mayday message(s)"
                        if (smsMayday) { sendSms(phone1, distressMsg) }
                        if (pushMayday) { sendPush(distressMsg) }                    
                   		if (notifyAlso) { sendNotificationEvent(distressMsg) }
                    }
                }
                
                def foundUser = ""
                def userName = settings."userNames${i}"
                
                if (userName != null) { foundUser = userName }
                
                if ((foundUser == "") && settings.anonymousAllowed) { 
                  	foundUser = "Unspecified Person" 
                }
				
                if (foundUser != "") {
                	if (anyoneIsHome() ) {
                		log.debug "Unlocked with code ${data.usedCode} - ${foundUser} is Home but someone is already present, no Action taken"
                		return
            		}
                    
		        	log.debug "Unlocked with code ${data.usedCode} - ${foundUser} is Home!"
    	    		notify("Running \"${settings.homePhrase}\" because ${foundUser} unlocked ${lock1.displayName}.")
                    state.lastUser = foundUser
					location.helloHome.execute(settings.homePhrase)			// Wake up the house - we're HOME!!!
                }
                else {
                	if (anyoneIsHome() ) {
                		log.debug "Unlocked by Unspecified Person (Code ID#${data.usedCode}, but someone is already present, no Action taken"
                		return
            		}
                    
                   	def doorMsg = "Unlocked by Unspecified Person (Code ID#${data.usedCode}), not running \"${homePhrase}\""
                    if (autoLock) {
                        lock1.lock()
                        doorMsg = doorMsg + " and auto-locking"
                    }
                    notify( doorMsg )
                }

            }
        }
        else if (evt.value == "locked") {
        
        	state.lastLockStatus = "locked"
        	if (anyoneIsHome() ) { 
            	log.debug "Someone is already present, no Action taken"
                return 
            }
            
            if (location.mode == "Away") { return } 			// door is probably being locked after everyone left
            
            if (state.lastUser != "") {							// shouldn't ever have a "" lastUser...
            	notify("Running \"${settings.awayPhrase}\" because ${state.lastUser} locked ${lock1.displayName} and nobody else is at home.")
            	state.lastUser = ""
            	location.helloHome.execute(settings.awayPhrase)
            }
        }
        else if (evt.value == "unknown") {						// Happens in testing sometimes
            if (state.lastLockStatus == "locked" ) {			// "probably" was a Keyed unlock attempt
            	state.lastLockStatus = "jammed"					// so we don't get stuck in a loop doing this
            	log.debug "Lock jammed, attempting to unlock"
                settings.lock1.unlock()
            }
            else {
            	state.lastLockStatus = "partial"
            	log.debug "Partially locked, attempting to re-lock"
                settings.lock1.lock()
            }
        }
        else {
        
        	log.debug "Unknown event value: ${evt.value}"
        }
    }
}

private notify( msg ) {
	if (hhNotifyOnly) { 
    	sendNotificationEvent( msg ) 
    }
	else {
		if (stPush) { sendPush( msg ) }			// Note: BOTH will also send to Hello, Home log
		if (stSMS) { sendSms( stSMS, msg ) }
	}
}

private anyoneIsHome() {    
	def result = false

	if(presence1.findAll { it?.currentPresence == "present" }) {
    	result = true
  	}

  	log.debug("anyoneIsHome: ${result}")

  result
}