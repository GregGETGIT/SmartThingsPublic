/**
 *  Power Failure
 *
 *  Copyright 2014 Scottin Pollock
 *
 */
 
definition(
    name: "Power Failure",
    namespace: "HAM Bridge",
    author: "Scottin Pollock",
    description: "Notify me of power failure and restoration using motion detector's change from wired-power to battery-power, and optionally manipulate lights and send a HAM Bridge Command. ",
    category: "My Apps",
    iconUrl: "http://solutionsetcetera.com/stuff/STIcons/powerFailure.png",
    iconX2Url: "http://solutionsetcetera.com/stuff/STIcons/powerFailure@2x.png"
)


preferences {
	section("When there is wired-power loss on...") {
			input "motion1", "capability.motionSensor", title: "Where?"
	}
	section("Via a push notification and a text message(optional)"){
    	input "pushAndPhone", "enum", title: "Send Text?", required: false, metadata: [values: ["Yes","No"]]
		input "phone1", "phone", title: "Phone Number (for Text, optional)", required: false
	}
    section("Make changes to the following when powered is restored..."){
    	input "offSwitches", "capability.switch", title: "Turn these off", required: false, multiple: true
    	input "onSwitches", "capability.switch", title: "Turn these on if after sunset", required: false, multiple: true
    }
    section("Send this command to HAM Bridge when powered is restored..."){
		input "HAMBcommand", "text", title: "Command to send", required: false
		input "server", "text", title: "Server IP", description: "Your HAM Bridger Server IP", required: false
		input "port", "number", title: "Port", description: "Port Number", required: false
	}
}

def installed() {
	init()
}

def updated() {
	unsubscribe()
	init()
}

def init() {
	subscribe(motion1, "powerSource.battery", powerOff)
    subscribe(motion1, "powerSource.powered", powerOn)
}

def powerOff(evt) {
	def msg = "A Power Failure has Just Occurred!"
    
	log.debug "sending push for power is out"
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
   		sendSms(phone1, msg)
	}
}

def powerOn(evt) {
	def msg = "The Power has Been Restored!"
    
	log.debug "sending push for power is back on"
	sendPush(msg)
    
    if ( phone1 && pushAndPhone ) {
    	log.debug "sending SMS to ${phone1}"
    	sendSms(phone1, msg)
	}
    
    if ( offSwitches ) {
    	log.debug "killing Hues"
    	offSwitches.off()
	}
    
    if ( onSwitches ) {
    	log.debug "restoring Hues"
        def ss = getSunriseAndSunset()
        def now = new Date()
		def dark = ss.sunset
        if ( dark.before(now) ) {
    		onSwitches.on()
        }    
	}
    
    if ( HAMBcommand ) {
        sendHAMB()   
	}
}

def sendHAMB() {
	def ip = "${settings.server}:${settings.port}"
	def deviceNetworkId = "1234"
	sendHubCommand(new physicalgraph.device.HubAction("""GET /?${settings.HAMBcommand} HTTP/1.1\r\nHOST: $ip\r\n\r\n""", physicalgraph.device.Protocol.LAN, "${deviceNetworkId}"))
}
