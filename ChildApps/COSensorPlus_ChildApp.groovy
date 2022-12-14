/**
 *
 * Sensor Groups+_CO
 *
 * Copyright 2022 Ryan Elliott
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * v1.0		RLE		Creation
 * v1.1     RLE     Added list attribute to show triggered devices
 * v1.2     RLE     Added threshold input and associated logic
 * v1.3		RLE		UI update
 */

definition(
        name: "Sensor Groups+_CO",
        namespace: "rle.sg+",
        author: "Ryan Elliott",
        description: "Creates a virtual device to track a group of carbon monoxide sensors.",
        category: "Convenience",
        parent: "rle.sg+:Sensor Groups+",
        iconUrl: "",
        iconX2Url: "")

preferences {
    page(name: "mainPage")
}

def mainPage() {
	return dynamicPage(name: "mainPage", uninstall:true, install: true) {
		section(getFormat("header","<b>App Name</b>")) {
            label title: "<b>Enter a name for this child app.</b>"+
            "<br>This will create a virtual CO sensor which reports the detected/clear status based on the sensors you select.", required:true,width:6
        }

        section(getFormat("header","<b>Device Selection</b>")) {
            paragraph "<b>Please choose which sensors to include in this group.</b>"+
            "<br>The virtual device will report status based on the configured threshold."
            input "carbonMonoxideSensors", "capability.carbonMonoxideDetector", title: "CO sensors to monitor", multiple:true, required:true,width:6
        }

        section(getFormat("header","<b>Options</b>")) {
            input "activeThreshold", "number", title: "<b>Threshold: How many sensors must detect CO before the group is active?</b><br>Leave set to one if CO detected by any sensor should change the group to detected.", required:false, defaultValue: 1,width:6
            paragraph ""
            input "debugOutput", "bool", title: "Enable debug logging?", defaultValue: true, displayDuringSetup: false, required: false
        }
    }
}

def installed() {
    initialize()
}

def uninstalled() {
    logDebug "uninstalling app"
    for (device in getChildDevices())
    {
        deleteChildDevice(device.deviceNetworkId)
    }
}

def updated() {
    logDebug "Updated with settings: ${settings}"
    unschedule()
    unsubscribe()
    initialize()
}

def initialize() {
    subscribe(carbonMonoxideSensors, "carbonMonoxide", carbonMonoxideHandler)
    createOrUpdateChildDevice()
    carbonMonoxideHandler()
    def device = getChildDevice(state.carbonMonoxideDevice)
    device.sendEvent(name: "TotalCount", value: carbonMonoxideSensors.size())
    device.sendEvent(name: "CODetectedThreshold", value: activeThreshold)
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def carbonMonoxideHandler(evt) {
    log.info "Carbon monoxide status changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.carbonMonoxideDevice)
    if (state.totalDetected >= activeThreshold)
    {
        log.info "Detected threshold met; setting group device as detected"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "carbonMonoxide", value: "detected", descriptionText: "The detected devices are ${state.coDetectedList}")
    }
    else
    {
        log.info "Detected threshold not met; setting virtual device as clear"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "carbonMonoxide", value: "clear")
    }
}

def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("carbonMonoxidegroup:" + app.getId())
    if (!childDevice || state.carbonMonoxideDevice == null) {
        logDebug "Creating child device"
        state.carbonMonoxideDevice = "carbonMonoxidegroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "carbonMonoxidegroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
    }
}

def logDebug(msg) {
    if (settings?.debugOutput) {
        log.debug msg
    }
}

def logsOff(){
    log.warn "debug logging disabled..."
    app.updateSetting("debugOutput",[value:"false",type:"bool"])
}

def getCurrentCount() {
    def device = getChildDevice(state.carbonMonoxideDevice)
    def totalDetected = 0
    def totalClear = 0
    def coDetectedList = []
    carbonMonoxideSensors.each { it ->
        if (it.currentValue("carbonMonoxide") == "detected")
        {
            totalDetected++
            coDetectedList.add(it.displayName)
            }
        else if (it.currentValue("carbonMonoxide") == "clear")
        {
            totalClear++
        }
    }
    state.totalDetected = totalDetected
    if (coDetectedList.size() == 0) {
        coDetectedList.add("None")
    }
    coDetectedList = coDetectedList.sort()
    coDetectedList = groovy.json.JsonOutput.toJson(coDetectedList)
    state.coDetectedList = coDetectedList
    logDebug "There are ${totalDetected} sensors detecting carbon monoxide"
    logDebug "There are ${totalClear} sensors that are clear"
    device.sendEvent(name: "TotalDetected", value: totalDetected)
    device.sendEvent(name: "TotalClear", value: totalClear)
    device.sendEvent(name: "CODetectedList", value: state.coDetectedList)
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
}