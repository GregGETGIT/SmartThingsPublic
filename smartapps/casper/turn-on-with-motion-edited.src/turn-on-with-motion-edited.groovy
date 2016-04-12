/**
 *  Turn On With Motion - This is a simple SmartApp that turns on a device if motion is detected
 *
 */
definition(
    name: "Turn On With Motion Edited",
    namespace: "Casper",
    author: "Casper",
    description: "Turns on a device if there is motion",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_motion-outlet@2x.png")



preferences {
	section("Turn on when there is movement..."){
		input "motions", "capability.motionSensor", title: "Where?", multiple: true
	}
	section("And off when there has been no movement for..."){
		input "minutes1", "number", title: "Minutes?"
	}
	section("Turn on simulated motion sensor..."){
		input "motion", "capability.motionSensor", multiple: false
	}
	section("Alert threshold before triggering"){
		input "alertThreshold", "number", title: "Number of Alert?"
	}
	section("Alert threshold timeout"){
		input "waitSeconds", "number", title: "Timeout of alert threshold in Seconds?"
	}
}


def installed() {
   initialized()
}

def updated() {
	unsubscribe()
    initialized()
}

def initialized() {
    state.alertCount = 0
    subscribe(motions, "motion", motionHandler)
}

def startTimer(seconds) {
	log.debug "in start timer"

    def now = new Date()
	def runTime = new Date(now.getTime() + (seconds * 1000))
	runOnce(runTime, myTimerEventHandler)
}

def myTimerEventHandler() {
     //do the things that I want delayed in this function
     log.debug "resetting alertThreshold because timeout"
     state.alertCount = 0
}

def motionHandler(evt) {
	// log.debug "$evt.name: $evt.value"
    if (evt.value == "active") {
	state.alertCount = state.alertCount + 1
        log.debug "Alert threshold: ${state.alertCount}"
        log.debug "waitSeconds = ${waitSeconds}"
        startTimer(waitSeconds)
    }
    if (state.alertCount >= alertThreshold) {
        if (evt.value == "active") {
            log.debug "turning on motion"
            switches.on()
            state.inactiveAt = null
        } else if (evt.value == "inactive") {
            if (!state.inactiveAt) {
                state.inactiveAt = now()
                runIn(minutes1 * 60, "scheduleCheck", [overwrite: false])
            }
        }
    }
}

def scheduleCheck() {
	//log.debug "schedule check, ts = ${state.inactiveAt}"
	if (state.inactiveAt) {
		def elapsed = now() - state.inactiveAt
		def threshold = 1000 * 60 * minutes1 - 3000
		if (elapsed >= threshold) {
			log.debug "turning off motion"
			switches.on()
			state.inactiveAt = null
            state.alertCount = 0
		}
		else {
			log.debug "${elapsed / 1000} sec since motion stopped"
		}
	}
}