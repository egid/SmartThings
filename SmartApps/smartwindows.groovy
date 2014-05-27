/**
 *  Smart Windows
 *
 *  Copyright 2014 Eric Gideon
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
	name: "Smart Windows",
	namespace: "egid",
	author: "Eric Gideon",
	description: "Compares two temperatures – indoor vs outdoor, for example – then sends an alert if windows are open (or closed!).",
	category: "My Apps",
	iconUrl: "https://s3.amazonaws.com/smartapp-icons/Home/home9-icn.png",
	iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Home/home9-icn@2x.png"
)


preferences {
	section( "Temperatures..." ){
		input "maxTemp", "number", title: "Max temp:"
		input "minTemp", "number", title: "Min temp:"
	}
	section( "Windows to check..." ){
		input "sensors", "capability.contactSensor", multiple: true
	}
	section( "Temperatures to monitor..." ){
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor temperature:"
		input "inTemp", "capability.temperatureMeasurement", title: "Indoor temperature:"
	}
	section( "Notifications" ) {
		input "sendPushMessage", "enum", title: "Send a push notification?", metadata:[values:["Yes","No"]], required:false
		input "retryPeriod", "number", title: "Minutes between notifications:"
	}
}


def installed() {
	log.debug "Installed: $settings"
	subscribe( inTemp, "temperature", temperatureHandler )
}

def updated() {
	log.debug "Updated: $settings"
	unsubscribe()
	subscribe( inTemp, "temperature", temperatureHandler )
}

def initialize() {
	//
}

def temperatureHandler(evt) {
	log.trace "temperature: $evt.value, $evt"

	def currentInTemp = evt.doubleValue
	def currentOutTemp = outTemp.latestValue("temperature")
	def openWindows = sensors.findAll { it?.latestValue("contact") == 'open' }

	// Don't spam notifications
	if (!retryPeriod) {
		def retryPeriod = 30
	}
	def timeAgo = new Date(now() - (1000 * 60 * retryPeriod).toLong())
	def recentEvents = inTemp.eventsSince(timeAgo)

	// Test against maximum specified temperature
	if ( currentOutTemp < maxTemp ) {
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $retryPeriod minutes"
		def alreadyNotified = recentEvents.count { it.doubleValue > currentOutTemp } > 1

		log.debug "Outside is colder than the max of ${maxTemp}, and can be used to cool the house."
		if ( alreadyNotified ) {
			log.debug "Already notified!"
		} else {
			log.debug "Sending notification"
			if( currentOutTemp <= currentInTemp && !openWindows ) {
				send( "Open some windows - it's colder outside! Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			}
			if( currentOutTemp > currentInTemp && openWindows ) {
				send( "It's warmer outside! You should close these windows: ${openWindows.join(', ')}. Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			}
		}
	// Otherwise, check against minimum temperature
	} else if ( currentOutTemp > minTemp ) {
		log.trace "Found ${recentEvents?.size() ?: 0} events in the last $retryPeriod minutes"
		def alreadyNotified = recentEvents.count { it.doubleValue < currentOutTemp } > 1

		log.debug "Outside is warmer than the minimum of ${minTemp}, and can be used to heat the house."
		if ( alreadyNotified ) {
			log.debug "Already notified!"
		} else {
			log.debug "Sending notification"
			if( currentOutTemp > currentInTemp && !openWindows ) {
				send( "Open some windows - it's warmer outside! Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			}
			if( currentOutTemp < currentInTemp && openWindows ) {
				send( "It's colder outside! You should close these windows: ${openWindows.join(', ')}. Currently ${currentInTemp}°F inside and ${currentOutTemp}°F outside." )
			}
		}
	}
}
private send(msg) {
	if ( sendPushMessage != "No" ) {
		log.debug( "sending push message" )
		sendPush( msg )
	}

	if ( phone1 ) {
		log.debug( "sending text message" )
		sendSms( phone1, msg )
	}

	log.debug msg
}