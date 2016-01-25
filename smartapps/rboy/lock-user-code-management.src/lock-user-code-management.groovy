/**
 * Add and remove user codes for locks
 *
 * Copyright 2014 Ramit Bhalla
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 *
 * UPDATED: 2014-11-27
 *
 */
definition(
		name: "Lock user code management",
		namespace: "rboy",
		author: "Ramit Bhalla",
		description: "Add and Delete User Codes for Locks",
		category: "My Apps",
		iconUrl: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave.png",
		iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Allstate/lock_it_when_i_leave@2x.png"
	  )


preferences {
	page(name: "setupApp")
}

def setupApp() {
    dynamicPage(name: "setupApp", title: "Lock User Management", install: true, uninstall: true) {    
        section("Select Lock(s)") {
            input "locks","capability.lock", title: "Lock", multiple: true
        }
        section("User Management") {
            input "action", "enum", title: "Add/Update/Delete User?", required: true, metadata: [values: ["Add/Update","Delete"]],  refreshAfterSelection: true
            input "user", "number", title: "User Slot Number", description: "This is the user slot number on the lock and not the user passcode"
        }

		if (action == "Add/Update") {
            section("Add/Update User Code") {
                input "code", "text", title: "User Passcode (check your lock passcode length)", defaultValue: "X", description: "The user passcode for adding/updating a new user (enter X for deleting user)"
            }
        }
    }
}

def installed()
{
	runIn(1, appTouch)
}

def updated()
{
	runIn(1, appTouch)
}

def appTouch() {
  	for (lock in locks) {
        if (action == "Delete") {
            lock.deleteCode(user)
            log.info "$lock deleted user: $user"
            sendNotificationEvent("$lock deleted user: $user")
            sendPush "$lock deleted user: $user"
        } else {
            lock.setCode(user, code)
            log.info "$lock added user: $user, code: $code"
            sendNotificationEvent("$lock added user: $user")
            sendPush "$lock added user: $user"
        }
    }
}