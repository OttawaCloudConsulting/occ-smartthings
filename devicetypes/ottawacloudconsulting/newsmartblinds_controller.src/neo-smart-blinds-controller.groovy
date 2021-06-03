/**
 *  New Smart Connector for Blinds
 *  API v 1.3/1.4
 *  Makes API calls across public internet & requires port forwarding to connector
 *
 *  Copyright 2021 Christian Turner
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
  definition (name: "Neo Smart Controller 1.3", namespace: "OttawaCoudConsulting", author: "Christian Turner", cstHandler: true) {
    capability "Window Shade Preset"
    capability "Window Shade"
    // capability "Window Shade Level"
  }

  preferences {
      section("URIs") {
      input "blindCode", "text", title: "Blind or Room Code (from Neo App)", required: true
      input "controllerID", "text", title: "Controller ID (from Neo App)", required: true
      input "controllerIP", "text", title: "Controller IP Address (from Neo App)", required: true
      input "timeToClose", "number", title: "Time in seconds it takes to close the blinds completely", required: true
      input "timeToFav", "number", title: "Time in seconds it takes to reach your favorite setting when closing the blinds", required: true
      input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: true
      }
  }

  simulator {
    // TODO: define status and reply messages here
    }

  tiles {
    // TODO: define your main and details tiles here
    }
}

// parse events into attributes
def date() {
	//def date = new Date().getTime().toString().drop(6)
    def origdate = new Date().getTime().toString().drop(6)  // API docs say only 7 chars 
    def random = Math.random().toString().reverse().take(4) // get four random #'s
    def date = origdate.toInteger() + random.toInteger()    // add 4 random #'s to millisecs
    if (logEnable) log.debug "Using ${date}"
	return date
}

def get(url,state) {
  try {
        httpGet(url) { resp ->
            if (resp.success) {
                sendEvent(name: "windowShade", value: "${state}", isStateChange: true)
          }
          if (logEnable)
                if (resp.data) log.debug "${resp.data}"
        }
    } catch (Exception e) {
        log.warn "Call to ${url} failed: ${e.message}"
    }
}

def logsOff() {
    log.warn "debug logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}

def installed() {
    log.info "installed..."
  if (!controllerID || !controllerIP || !blindCode || !timeToClose || !timeToFav) {
    log.error "Please make sure controller ID, IP, blind/room code, time to close and time to favorite are configured." 
  }
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def updated() {
    log.info "updated..."
  if (!controllerID || !controllerIP || !blindCode || !timeToClose || !timeToFav) {
    log.error "Please make sure controller ID, IP, blind/room code, time to close and time to favorite are configured." 
  }
    log.warn "debug logging is: ${logEnable == true}"
    if (logEnable) runIn(1800, logsOff)
}

def parse(String description) {
    if (logEnable) log.debug(description)
}

// handle commands
def open() {
	log.debug "Executing 'open'"
  def url = ("http://" + controllerIP + ":8838/neo/v1/transmit?command=" + blindCode + "-up!bf&id=" + controllerID + "&hash=" + date())
  if (logEnable) log.debug "Sending open GET request to ${url}"
  sendEvent(name: "windowShade", value: "opening", isStateChange: true)
  get(url,"open")
  state.level=100
  state.secs=0
  sendEvent(name: "level", value: "${state.level}", isStateChange: true)
}

def close() {
	log.debug "Executing 'close'"
  def url = ("http://" + controllerIP + ":8838/neo/v1/transmit?command=" + blindCode + "-dn&id=" + controllerID + "&hash=" + date())
  if (logEnable) log.debug "Sending close GET request to ${url}"
  sendEvent(name: "windowShade", value: "closing", isStateChange: true)
  get(url,"closed")
  state.level=0
  state.secs=timeToClose
  sendEvent(name: "level", value: "${state.level}", isStateChange: true)
}

def pause() {
	log.debug "Executing 'pause'"
  def url = "http://" + controllerIP + ":8838/neo/v1/transmit?command=" + blindCode + "-sp&id=" + controllerID + "&hash=" + "0000006"
  if (logEnable) log.debug "Sending stop GET request to ${url}"
}
def presetPosition() {
	log.debug "Executing 'presetPosition'"
  def url = "http://" + controllerIP + ":8838/neo/v1/transmit?command=" + blindCode + "-gp&id=" + controllerID + "&hash=" + "0000006"
  if (logEnable) log.debug "Sending favorite GET request to ${url}"
  state.secs=timeToFav
  state.level=100-((timeToFav/timeToClose)*100)
  get(url,"preset position")
  sendEvent(name: "level", value: "${state.level}", isStateChange: true)
}

// def setShadeLevel() {
// 	log.debug "Executing 'setShadeLevel'"
// 	// TODO: handle 'setShadeLevel' command
// }