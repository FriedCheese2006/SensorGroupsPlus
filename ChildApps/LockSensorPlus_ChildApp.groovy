/**
 *
 * Sensor Groups+_Lock
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
 * v1.0		RLE		See parent for changelog
 */

definition(
        name: "Sensor Groups+_Lock",
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
		section(getFormat("header","App Name")) {
            label title: getFormat("importantBold","Enter a name for this child app.")+
            getFormat("lessImportant","<br>This will create a virtual lock which reports the locked/unlocked status based on the devices you select."), required:true,width:4
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph getFormat("importantBold","Please choose which sensors to include in this group.")

			input "lockSensors", "capability.lock", title: getFormat("lessImportant","Devices to monitor"), multiple:true, required:true,width:4
        }

		section(getFormat("header","<b>Options</b>")) {            
            input "activeThreshold", "number", title: getFormat("importantBold","How many sensors must be unlocked before the group is unlocked?")+
				getFormat("lessImportant","<br>Leave set to one if and device being unlocked should change the group to locked."), required:false, defaultValue: 1,width:4
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
    subscribe(lockSensors, "lock", lockHandler)
    createOrUpdateChildDevice()
    lockHandler()
    def device = getChildDevice(state.lockDevice)
    device.sendEvent(name: "TotalCount", value: lockSensors.size())
    device.sendEvent(name: "LocksUnlockedThreshold", value: activeThreshold)
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def lockHandler(evt) {
    log.info "Lock status changed, checking status count..."
    getCurrentCount()
    def device = getChildDevice(state.lockDevice)
    if (state.totalUnlocked >= activeThreshold)
    {
        log.info "Unlocked threshold met; setting group device as unlocked"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "lock", value: "unlocked", descriptionText: "The unlocked devices are ${state.lockedList}")
    }
    else
    {
        log.info "Unlocked threshold not met; setting virtual device as locked"
        logDebug "Current threshold value is ${activeThreshold}"
        device.sendEvent(name: "lock", value: "locked")
    }
}

def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("lockgroup:" + app.getId())
    if (!childDevice || state.lockDevice == null) {
        logDebug "Creating child device"
        state.lockDevice = "lockgroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "lockgroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
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
    def device = getChildDevice(state.lockDevice)
    def totalLocked = 0
    def totalUnlocked = 0
    def unlockedList = []
    lockSensors.each { it ->
        if (it.currentValue("lock") == "unlocked")
        {
            totalUnlocked++
            unlockedList.add(it.displayName)
            }
        else if (it.currentValue("lock") == "locked")
        {
            totalLocked++
        }
    }
    state.totalLocked = totalLocked
    if (unlockedList.size() == 0) {
        unlockedList.add("None")
    }
    unlockedList = unlockedList.sort()
    state.unlockedList = unlockedList
    logDebug "There are ${totalLocked} devices locked."
    logDebug "There are ${totalUnlocked} devices unlocked."
    device.sendEvent(name: "TotalLocked", value: totalLocked)
    device.sendEvent(name: "TotalUnlocked", value: totalUnlocked)
    device.sendEvent(name: "UnlockedList", value: state.unlockedList)

    //Create display list
    String displayList = "<ul style='list-style-type: none; margin: 0;padding: 0'>"
	unlockedList.each {it ->
	displayList += "<li>${it}</li>"
	}
	displayList += "</ul>"
	device.sendEvent(name: "displayList", value: displayList)
}

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
	if(type == "red") return "<div style='color:#660000'>${myText}</div>"
	if(type == "importantBold") return "<div style='color:#32a4be;font-weight: bold'>${myText}</div>"
	if(type == "important") return "<div style='color:#32a4be'>${myText}</div>"
	if(type == "important2") return "<div style='color:#5a8200'>${myText}</div>"
	if(type == "important2Bold") return "<div style='color:#5a8200;font-weight: bold'>${myText}</div>"
	if(type == "lessImportant") return "<div style='color:green'>${myText}</div>"
	if(type == "rateDisplay") return "<div style='color:green; text-align: center;font-weight: bold'>${myText}</div>"
}