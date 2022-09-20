/**
 *
 * Sensor Groups+_Virtual CO Detector
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
 */

metadata {
    definition(
            name: "Sensor Groups+_Virtual CO Detector",
            namespace: "rle.sg+",
            author: "Ryan Elliott")
            {
                capability "Carbon Monoxide Detector"
                capability "Actuator"

                attribute "carbonMonoxide", "enum", ["clear", "detected"]
                attribute "TotalCount", "number"
                attribute "TotalDetected", "number"
                attribute "TotalClear", "number"
                attribute "CODetectedList", "string"
            }
}

def installed() {
    log.warn "installed..."
}

def updated() {
    log.info "updated..."
}
