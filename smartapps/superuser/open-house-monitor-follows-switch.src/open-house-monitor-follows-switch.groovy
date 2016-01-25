/**
 *  Night Mode Toggle
 *
 *  code borrowed from @stevesell
 *
 *  Date: 2014-04-17
 */
preferences {
	section("Select Switch to monitor"){
		input "theSwitch", "capability.switch"
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated(settings) {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
    initialize()
}

def onHandler(evt) {
	log.debug "Received on from ${theSwitch}"
    if(location.mode != "Open House Monitor") {
        setLocationMode("Open House Monitor")
    } else {
    	log.debug "Already Open House Monitor - ignoring"
    }

}

def offHandler(evt) {
	log.debug "Received off from ${theSwitch}"
    if(location.mode != "Home") {
        setLocationMode("Home")
    } else {
    	log.debug "Already Home - ignoring"
    }
}

def initialize() {
	subscribe(theSwitch, "switch.On", onHandler)
    subscribe(theSwitch, "switch.Off", offHandler)
}