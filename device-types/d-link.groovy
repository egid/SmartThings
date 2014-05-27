/** D-Link Camera 
 * Supports (at least) the D-Link DCS-930L IP webcam.
 *
 * Author: danny@smartthings.com
 * Author: brian@bevey.org
 * 
 * Author: http://github.com/egid - cleanup, better match the D-Link url format
 * Date: 5/21/14
 */

preferences
{
	input("username",	"text",		title: "Username",	description: "Camera username")
	input("password",	"password",	title: "Password",	description: "Camera password")
	input("url",		"text",		title: "IP or URL",	description: "URL or IP of camera")
	input("port",		"text",		title: "Port",		description: "Port")
}

metadata {
	definition (name: "D-Link Camera") {
		capability "Image Capture"
	}

	tiles {
		carouselTile("cameraDetails", "device.image", width: 3, height: 2) { }

		standardTile("camera", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: false, canChangeBackground: true) {
		  state "default", label: '', action: "Image Capture.take", icon: "st.camera.camera", backgroundColor: "#FFFFFF"
		}

		standardTile("take", "device.image", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false, decoration: "flat") {
		  state "take", label: 'Take Photo', action: "Image Capture.take", icon: "st.camera.take-photo", nextState:"taking"
		}

		main "camera"
		details(["cameraDetails","take"])
	}
}

def parseCameraResponse(def response) {
	if(response.headers.'Content-Type'.contains("image/jpeg")) {
		def imageBytes = response.data

		if(imageBytes) {
			storeImage(getPictureName(), imageBytes)
		}
	} else {
		log.error("${device.label} could not capture an image.")
	}
}

private getPictureName() {
	def pictureUuid = java.util.UUID.randomUUID().toString().replaceAll('-', '')
	"image" + "_$pictureUuid" + ".jpg"
}

private take() {
	log.info("${device.label} taking photo")

	httpGet("http://${username}:${password}@${url}:${port}/image/jpeg.cgi"){
		response -> log.info("${device.label} image captured")
		parseCameraResponse(response)
	}
}