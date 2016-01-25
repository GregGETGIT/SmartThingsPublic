/**
 *  I'm on Vacation
 *
 *  Copyright 2014 Scottin Pollock
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
    name: "I'm on Vacation",
    namespace: "soletc.com",
    author: "Scottin Pollock",
    description: "Randomizes switches and schedules to make it look like you are home.",
    category: "My Apps",
    iconUrl: "http://solutionsetcetera.com/stuff/STIcons/vacation.png",
    iconX2Url: "http://solutionsetcetera.com/stuff/STIcons/vacation@2x.png")


preferences {
	section("Choose lights that will always come on...") {
		input "alwaysSwitches", "capability.switch", multiple: true, required: true
    }
   	
    section("Choose lights from main living areas...") {
        input "randomSwitches", "capability.switch", multiple: true, required: true
		input "randomSwitchesChangeInterval", "number", title: "Scene change interval (minutes)...", required: true

	}
   	section("Choose lights from satellite areas...") {
		input "reallyRandomSwitches", "capability.switch", multiple: true, required: true
		input "reallyRandomSwitchesChangeInterval", "number", title: "Scene change interval (minutes)...", required: true
	}
    
    section("Times to start/stop & random offset...") {
		input "startTime", "time", title: "Time to start activity...", required: true
		input "stopTime", "time", title: "Time to stop activity...", required: true
		input "randomTime", "number", title: "Apply random offset (minutes)...", required: true
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unschedule()
	initialize()
}

def initialize() {
	schedule(startTime, startActivity)
}


def startActivity() {
        log.debug "in startactivity"

        def ASOffset = new Random().nextInt(randomTime)
        def ROffset = new Random().nextInt(randomTime)
        def RROffset = new Random().nextInt(randomTime)
        log.debug "asoffset is $ASOffset and ROffset $ROffset and RROffset is $RROffset"
    	runIn(ASOffset*60, alwaysAction)
    	runIn(ROffset*60, randomAction)
    	runIn(RROffset*60, reallyRandomAction)
}

def shouldWeContinue() {
	def result = true
    def onlyAfter = timeToday(stopTime)
    if (now() > onlyAfter.time) {
        unschedule()
        alwaysSwitches.off()
        randomSwitches.off()
        reallyRandomSwitches.off()
        result = false
    } 
    result
}

def alwaysAction() {
	alwaysSwitches.on()
}

def randomAction() {
	if (shouldWeContinue()) {
		log.debug "We should continue randomAction."
		def switchToChange = theRandomSwitch()
    	log.debug switchToChange.latestValue("switch")
    	log.debug switchToChange
    	if (switchToChange.latestValue("switch") == "on") { 
    		log.debug "$switchToChange is on and we should be turning it off"
    		switchToChange.off()
   		} else {
    		log.debug "$switchToChange is off and we should be turning it on"
			switchToChange.on()
    	}
    	runIn(randomSwitchesChangeInterval*60, randomAction)
    }
}

def reallyRandomAction() {
	if (shouldWeContinue()) {
		log.debug "We should continue reallyRandomAction."
		def switchToChange = theReallyRandomSwitch()
    	log.debug switchToChange.latestValue("switch")
    	log.debug switchToChange
    	if (switchToChange.latestValue("switch") == "on") { 
    		log.debug "$switchToChange is on and we should be turning it off"
    		switchToChange.off()
   		} else {
    		log.debug "$switchToChange is off and we should be turning it on"
			switchToChange.on()
    	}
    	runIn(reallyRandomSwitchesChangeInterval*60, reallyRandomAction)
    }
}

def theRandomSwitch() {
	def allRandoms = settings.randomSwitches
    def result = allRandoms[new Random().nextInt(randomSwitches.size())]
}

def theReallyRandomSwitch() {
	def allReallyRandoms = settings.reallyRandomSwitches
    def result = allReallyRandoms[new Random().nextInt(reallyRandomSwitches.size())]
}