/**
 *
 * Sensor Groups+_Humidity
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
 * v1.1		RLE		UI update
 */
 
definition(
    name: "Sensor Groups+_Humidity",
    namespace: "rle.sg+",
    author: "Ryan Elliott",
    description: "Creates a virtual device to track a group of humidity sensors.",
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
            "<br>This will create a virtual humidity sensor which reports the high, low, and average humidity based on the sensors you select.", required:true,width:6
		}

		section(getFormat("header","<b>Device Selection</b>")) {
			paragraph "<b>Please choose which sensors to include in this group.</b>"
			input "humiditySensors", "capability.relativeHumidityMeasurement", title: "Humidity sensors to monitor", multiple:true, required:true,width:6
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
	subscribe(humiditySensors, "humidity", humidityHandler)
    createOrUpdateChildDevice()
    humidityHandler()	
	if (debugOutput) {
		runIn(1800,logsOff)
	}
}

def humidityHandler(evt) {
    log.info "Humidity sensor change; checking totals..."
    def totalCount = humiditySensors.size()
    def device = getChildDevice(state.humidityDevice)
    device.sendEvent(name: "TotalCount", value: totalCount)    
    def listHumidity = []
    def avgHumidity = 0
    def total = 0
    humiditySensors.each {it ->
        total = (total + it.currentHumidity)
        listHumidity.add(it.currentHumidity)
    }
    avgHumidity = (total / totalCount).toDouble().round(1)
    logDebug "Average humidity is ${avgHumidity}"
    device.sendEvent(name: "AverageHumidity", value: avgHumidity)
    minHumidity = listHumidity.min()
    logDebug "Min humidity is ${minHumidity}"
    device.sendEvent(name: "MinHumidity", value: minHumidity)
    maxHumidity = listHumidity.max()
    logDebug "Max humidity is ${maxHumidity}"
    device.sendEvent(name: "MaxHumidity", value: maxHumidity)
}


def createOrUpdateChildDevice() {
    def childDevice = getChildDevice("humiditygroup:" + app.getId())
    if (!childDevice || state.humidityDevice == null) {
        logDebug "Creating child device"
        state.humidityDevice = "humiditygroup:" + app.getId()
        addChildDevice("rle.sg+", "Sensor Groups+_OmniSensor", "humiditygroup:" + app.getId(), 1234, [name: app.label, isComponent: false])
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

def getFormat(type, myText="") {
	if(type == "header") return "<div style='color:#660000;font-weight: bold'>${myText}</div>"
}