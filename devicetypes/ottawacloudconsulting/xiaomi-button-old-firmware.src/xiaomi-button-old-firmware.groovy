/**
 *  Xiaomi Zigbee Button - model WXKG01LM
 *  OLD Device Handler for SmartThings - Firmware versions 24.x and older ONLY
 *  Version 1.2.1
 *
 *  NOTE: Do NOT use this device handler on a SmartThings v2/v3 hub with firmware 25.20 or newer
 *        Instead use the xiaomi-button.groovy device driver
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *	    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Original device handler code by a4refillpad, adapted for use with Aqara model by bspranger
 *  Additional contributions to code by alecm, alixjg, bspranger, gn0st1c, foz333, jmagnuson, rinkek, ronvandegraaf, snalee, tmleafs, twonk, & veeceeoh
 *
 *  Known issues:
 *  Xiaomi sensors do not seem to respond to refresh requests
 *  Inconsistent rendering of user interface text/graphics between iOS and Android devices - This is due to SmartThings, not this device handler
 *  Pairing Xiaomi sensors can be difficult as they were not designed to use with a SmartThings hub.
 *
 *
 */

metadata {
	definition (name: "Xiaomi Button Old Firmware", namespace: "OttawaCloudConsulting", author: "bspranger") {
		capability "Battery"
		capability "Sensor"
		capability "Button"
		capability "Holdable Button"
		capability "Actuator"
		capability "Momentary"
		capability "Configuration"
		capability "Health Check"

		attribute "lastCheckin", "string"
		attribute "lastCheckinCoRE", "string"
		attribute "lastPressed", "string"
		attribute "lastPressedCoRE", "string"
		attribute "lastReleasedCoRE", "string"
		attribute "lastButtonMssg", "string"
		attribute "batteryRuntime", "string"
		attribute "buttonStatus", "enum", ["pushed", "held", "released"]

		fingerprint endpointId: "01", profileId: "0104", deviceId: "0104", inClusters: "0000,0003,FFFF,0019", outClusters: "0000,0004,0003,0006,0008,0005,0019", manufacturer: "LUMI", model: "lumi.sensor_switch", deviceJoinName: "Original Xiaomi Button"

		command "resetBatteryRuntime"
	}

	simulator {
		status "Press button": "on/off: 0"
		status "Release button": "on/off: 1"
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"buttonStatus", type: "lighting", width: 6, height: 4, canChangeIcon: false) {
			tileAttribute ("device.buttonStatus", key: "PRIMARY_CONTROL") {
				attributeState("default", label:'Pushed', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
				attributeState("pushed", label:'Pushed', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
				attributeState("held", label:'Held', backgroundColor:"#00a0dc", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonPushed.png")
				attributeState("released", label:'Released', action: "momentary.push", backgroundColor:"#ffffff", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/ButtonReleased.png")
			}
			tileAttribute("device.lastPressed", key: "SECONDARY_CONTROL") {
				attributeState "lastPressed", label:'Last Pressed: ${currentValue}'
			}
		}
		valueTile("battery", "device.battery", decoration: "flat", inactiveLabel: false, width: 2, height: 2) {
			state "battery", label:'${currentValue}%', unit:"%", icon:"https://raw.githubusercontent.com/bspranger/Xiaomi/master/images/XiaomiBattery.png",
			backgroundColors:[
				[value: 10, color: "#bc2323"],
				[value: 26, color: "#f1d801"],
				[value: 51, color: "#44b621"]
			]
		}
		valueTile("lastCheckin", "device.lastCheckin", decoration: "flat", inactiveLabel: false, width: 4, height: 1) {
			state "lastCheckin", label:'Last Event:\n${currentValue}'
		}
		valueTile("batteryRuntime", "device.batteryRuntime", inactiveLabel: false, decoration: "flat", width: 4, height: 1) {
			state "batteryRuntime", label:'Battery Changed: ${currentValue}'
		}
		main (["buttonStatus"])
		details(["buttonStatus","battery","lastCheckin","batteryRuntime"])
	}

	preferences {
		//Button Config
		input description: "A button click always sends a 'button 1 pushed' event, but as a default if it is held for at least 2 seconds, a 'button 1 held' event is sent when released. The settings below allow changes to the minimum time needed for held and what type of event should be sent.", type: "paragraph", element: "paragraph", title: "BUTTON CONFIGURATION"
		input name: "waittoHeld", type: "decimal", title: "Minimum hold time required for button held:", description: "Number of seconds (default = 2.0)", range: "0.1..120"
		input name: "holdIsButton2", type: "bool", title: "On button hold, send 'button 2 pushed' event instead of 'button 1 held'?"

		//Date & Time Config
		input description: "", type: "paragraph", element: "paragraph", title: "DATE & CLOCK"
		input name: "dateformat", type: "enum", title: "Set Date Format\nUS (MDY) - UK (DMY) - Other (YMD)", description: "Date Format", options:["US","UK","Other"]
		input name: "clockformat", type: "bool", title: "Use 24 hour clock?"
		//Battery Reset Config
		input description: "If you have installed a new battery, the toggle below will reset the Changed Battery date to help remember when it was changed.", type: "paragraph", element: "paragraph", title: "CHANGED BATTERY DATE RESET"
		input name: "battReset", type: "bool", title: "Battery Changed?"
		//Advanced Settings
		input description: "Only change the settings below if you know what you're doing.", type: "paragraph", element: "paragraph", title: "ADVANCED SETTINGS"
		//Battery Voltage Range
		input description: "", type: "paragraph", element: "paragraph", title: "BATTERY VOLTAGE RANGE"
		input name: "voltsmax", type: "decimal", title: "Max Volts\nA battery is at 100% at __ volts\nRange 2.8 to 3.4", range: "2.8..3.4", defaultValue: 3
		input name: "voltsmin", type: "decimal", title: "Min Volts\nA battery is at 0% (needs replacing) at __ volts\nRange 2.0 to 2.7", range: "2..2.7", defaultValue: 2.5
		//Live Logging Message Display Config
		input description: "These settings affect the display of messages in the Live Logging tab of the SmartThings IDE.", type: "paragraph", element: "paragraph", title: "LIVE LOGGING"
		input name: "infoLogging", type: "bool", title: "Display info log messages?", defaultValue: true
		input name: "debugLogging", type: "bool", title: "Display debug log messages?"
	}
}

//adds functionality to press the centre tile as a virtualApp Button
def push() {
	displayInfoLog(": Virtual App Button Pressed")
	sendEvent(name: "lastPressed", value: formatDate(), displayed: false)
	sendEvent(name: "lastPressedCoRE", value: now(), displayed: false)
	sendEvent(name: "button", value: "pushed", data: [buttonNumber: 1], descriptionText: "$device.displayName app button was pushed", isStateChange: true)
	sendEvent(name: "lastReleasedCoRE", value: now(), displayed: false)
	runIn(1, clearButtonStatus)
}

// Parse incoming device messages to generate events
def parse(String description) {
	displayDebugLog(": Parsing '${description}'")
	def result = [:]

	// Any report - button press & Battery - results in a lastCheckin event and update to Last Checkin tile
	sendEvent(name: "lastCheckin", value: formatDate(), displayed: false)
	sendEvent(name: "lastCheckinCoRE", value: now(), displayed: false)

	// Send message data to appropriate parsing function based on the type of report
	if (description == 'on/off: 0') {
		updateLastPressed("Pressed")
	} else if (description == 'on/off: 1') {
		result = createButtonEvent()
		updateLastPressed("Released")
	} else if (description?.startsWith('catchall:')) {
		result = parseCatchAllMessage(description)
	} else if (description?.startsWith('read attr - raw: ')) {
		result = parseReadAttrMessage(description)
	}
	if (result != [:]) {
		displayDebugLog(": Creating event $result")
		return createEvent(result)
	} else
		return [:]
}

// on any type of button pressed update lastPressed and lastPressedCoRE or lastReleasedCoRE to current date/time
def updateLastPressed(pressType) {
	if (pressType == "Pressed")
		displayInfoLog(": Button press detected")
	sendEvent(name: "lastPressed", value: formatDate(), displayed: false)
	displayDebugLog(": Setting Last $pressType to current date/time")
	sendEvent(name: "last${pressType}CoRE", value: now(), displayed: false)
	sendEvent(name: "lastButtonMssg", value: now(), displayed: false)
}

private createButtonEvent() {
	def timeDif = now() - device.latestState('lastButtonMssg').date.getTime()
	def holdTimeMillisec = Math.round((settings.waittoHeld?:2.0) * 1000)
	displayInfoLog(": Button release detected")
	displayDebugLog(": Comparing time difference between this button release and last button message")
	displayDebugLog(": Time difference = $timeDif ms, Hold time setting = $holdTimeMillisec ms")
	// compare waittoHeld setting with difference between current time and lastButtonMssg
	def buttonHeld = (timeDif >= holdTimeMillisec & timeDif < holdTimeMillisec + 10000) ? true : false
	def eventValue = (buttonHeld & !(holdIsButton2 == true)) ? "held" : "pushed"
	def buttonNum = (buttonHeld & holdIsButton2 == true) ? 2 : 1
	def pressType = buttonHeld ? "held" : "pushed"
    def descText = " was $pressType (button $buttonNum $eventValue)"
	sendEvent(name: "buttonStatus", value: pressType, isStateChange: true, displayed: false)
	runIn(1, clearButtonStatus)
	displayInfoLog(descText)
	return [
		name: 'button',
		value: eventValue,
		data: [buttonNumber: buttonNum],
		descriptionText: "$device.displayName$descText",
		isStateChange: true
	]
}

def clearButtonStatus() {
	sendEvent(name: "buttonStatus", value: "released", isStateChange: true, displayed: false)
}

private Map parseReadAttrMessage(String description) {
	def cluster = description.split(",").find {it.split(":")[0].trim() == "cluster"}?.split(":")[1].trim()
	def attrId = description.split(",").find {it.split(":")[0].trim() == "attrId"}?.split(":")[1].trim()
	def value = description.split(",").find {it.split(":")[0].trim() == "value"}?.split(":")[1].trim()
	def data = ""
	def modelName = ""
	def model = value
	Map resultMap = [:]

	// Process message on short-button press containing model name and battery voltage report
	if (cluster == "0000" && attrId == "0005")	{
		if (value.length() > 45) {
			model = value.split("02FF")[0]
			data = value.split("02FF")[1]
			if (data[4..7] == "0121") {
				def BatteryVoltage = (Integer.parseInt((data[10..11] + data[8..9]),16))
				resultMap = getBatteryResult(BatteryVoltage)
			}
		data = ", data: ${value.split("02FF")[1]}"
		}

		// Parsing the model name
		for (int i = 0; i < model.length(); i+=2) {
			def str = model.substring(i, i+2);
			def NextChar = (char)Integer.parseInt(str, 16);
			modelName = modelName + NextChar
		}
		displayDebugLog(" reported model: $modelName$data")
	}
	return resultMap
}

// Check catchall for battery voltage data to pass to getBatteryResult for conversion to percentage report
private Map parseCatchAllMessage(String description) {
	Map resultMap = [:]
	def catchall = zigbee.parse(description)

	if (catchall.clusterId == 0x0000) {
		def MsgLength = catchall.data.size()
		// Xiaomi CatchAll does not have identifiers, first UINT16 is Battery
		if ((catchall.data.get(0) == 0x01 || catchall.data.get(0) == 0x02) && (catchall.data.get(1) == 0xFF)) {
			for (int i = 4; i < (MsgLength-3); i++) {
				if (catchall.data.get(i) == 0x21) { // check the data ID and data type
					// next two bytes are the battery voltage
					resultMap = getBatteryResult((catchall.data.get(i+2)<<8) + catchall.data.get(i+1))
					break
				}
			}
		}
	}
	return resultMap
}

// Convert raw 4 digit integer voltage value into percentage based on minVolts/maxVolts range
private Map getBatteryResult(rawValue) {
	// raw voltage is normally supplied as a 4 digit integer that needs to be divided by 1000
	// but in the case the final zero is dropped then divide by 100 to get actual voltage value
	def rawVolts = rawValue / 1000
	def minVolts = voltsmin ? voltsmin : 2.5
	def maxVolts = voltsmax ? voltsmax : 3.0
	def pct = (rawVolts - minVolts) / (maxVolts - minVolts)
	def roundedPct = Math.min(100, Math.round(pct * 100))
	def descText = "Battery at ${roundedPct}% (${rawVolts} Volts)"
	displayInfoLog(": $descText")
	return [
		name: 'battery',
		value: roundedPct,
		unit: "%",
		isStateChange:true,
		descriptionText : "$device.displayName $descText"
	]
}

private def displayDebugLog(message) {
	if (debugLogging)
		log.debug "${device.displayName}${message}"
}

private def displayInfoLog(message) {
	if (infoLogging || state.prefsSetCount < 3)
		log.info "${device.displayName}${message}"
}

//Reset the date displayed in Battery Changed tile to current date
def resetBatteryRuntime(paired) {
	def newlyPaired = paired ? " for newly paired sensor" : ""
	sendEvent(name: "batteryRuntime", value: formatDate(true))
	displayInfoLog(": Setting Battery Changed to current date${newlyPaired}")
}

// installed() runs just after a sensor is paired using the "Add a Thing" method in the SmartThings mobile app
def installed() {
	state.prefsSetCount = 0
	displayInfoLog(": Installing")
	if (!device.currentState('batteryRuntime')?.value)
		resetBatteryRuntime(true)
	checkIntervalEvent("")
	sendEvent(name: "numberOfButtons", value: 1)
}

// configure() runs after installed() when a sensor is paired
def configure() {
	def numButtons = (holdIsButton2 == true) ? 2 : 1
	displayInfoLog(": Configuring")
	if (!device.currentState('batteryRuntime')?.value)
		resetBatteryRuntime(true)
	clearButtonStatus()
	checkIntervalEvent("configured")
	sendEvent(name: "numberOfButtons", value: numButtons)
	sendEvent(name: "lastButtonMssg", value: now(), displayed: false)
	return
}

// updated() will run twice every time user presses save in preference settings page
def updated() {
	def numButtons = (holdIsButton2 == true) ? 2 : 1
	displayInfoLog(": Updating preference settings")
	if (!state.prefsSetCount)
		state.prefsSetCount = 1
	else if (state.prefsSetCount < 3)
		state.prefsSetCount = state.prefsSetCount + 1
	if (!device.currentState('batteryRuntime')?.value)
		resetBatteryRuntime()
	if (battReset){
		resetBatteryRuntime()
		device.updateSetting("battReset", false)
	}
	clearButtonStatus()
	sendEvent(name: "numberOfButtons", value: numButtons)
	displayInfoLog(": Number of buttons = $numButtons")
	displayInfoLog(": Info message logging enabled")
	displayDebugLog(": Debug message logging enabled")
	checkIntervalEvent("preferences updated")
}

private checkIntervalEvent(text) {
		// Device wakes up every 1 hours, this interval allows us to miss one wakeup notification before marking offline
		if (text)
			displayInfoLog(": Set health checkInterval when ${text}")
		sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
}

def formatDate(batteryReset) {
	def correctedTimezone = ""
	def timeString = clockformat ? "HH:mm:ss" : "h:mm:ss aa"

	// If user's hub timezone is not set, display error messages in log and events log, and set timezone to GMT to avoid errors
	if (!(location.timeZone)) {
		correctedTimezone = TimeZone.getTimeZone("GMT")
		log.error "${device.displayName}: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app."
		sendEvent(name: "error", value: "", descriptionText: "ERROR: Time Zone not set, so GMT was used. Please set up your location in the SmartThings mobile app.")
	}
	else {
		correctedTimezone = location.timeZone
	}
	if (dateformat == "US" || dateformat == "" || dateformat == null) {
		if (batteryReset)
			return new Date().format("MMM dd yyyy", correctedTimezone)
		else
			return new Date().format("EEE MMM dd yyyy ${timeString}", correctedTimezone)
	}
	else if (dateformat == "UK") {
		if (batteryReset)
			return new Date().format("dd MMM yyyy", correctedTimezone)
		else
			return new Date().format("EEE dd MMM yyyy ${timeString}", correctedTimezone)
	}
	else {
		if (batteryReset)
			return new Date().format("yyyy MMM dd", correctedTimezone)
		else
			return new Date().format("EEE yyyy MMM dd ${timeString}", correctedTimezone)
	}
}
