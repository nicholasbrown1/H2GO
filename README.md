# H2GO
Our application directs users towards the nearest suitable source of drinking water on the UCSB campus. The app should use the device's location to locate the nearest source, and display its location in words, times the source can be accessed, and a picture. The user should also be able to access water source data through a map and filter different source types (drinking fountains/hydration stations)

# Setup
To run this code you must put MAPS_API_KEY=[your maps api key] in the local.properties folder in the main project directory.

You must also add google-services.json in the /app directory. The json file is downloaded from the firebase.

For Google sign in, you need to run ./gradlew signingreport to get a SHA-1 key and put that key in the firebase.

# Firebase Layout
### Collection: filling_locations
Each document in filling_locations is a pin on the map
##### Fields
* lat: the latitude value of the filling location
* long: the longitude value of the filling location
* title: the label of the pin
* drinking_fountain: whether or not the location is a drinking fountain
* hydration_station: whether or not the location is a hydration station
* floor: what floor the water source is located on (stored as a string to account for things like basements)
* rating: the current rating of the water fountain
* num_ratings: the number of ratings
* approved: whether or not an admin has approved the pin

### Collection: user_filling_locations
Each document in user_filling_locations is a pin on the map uploaded by a user
##### Fields
* lat: the latitude value of the filling location
* long: the longitude value of the filling location
* title: the label of the pin
* drinking_fountain: whether or not the location is a drinking fountain
* hydration_station: whether or not the location is a hydration station
* floor: what floor the water source is located on (stored as a string to account for things like basements)
* rating: the current rating of the water fountain
* num_ratings: the number of ratings
* approved: whether or not an admin has approved the pin

### Collection: ratings
Each document in ratings is a rating by a user
##### Fields
* lat: the latitude value of the filling location being rated
* long: the longitude value of the filling location being rated
* rating: the rating value
* user: the user giving the rating

### Collection: comments
Each document in comments is a comment by a user
##### Fields
* lat: the latitude value of the filling location being rated
* long: the longitude value of the filling location being rated
* text: the text of the comment
* user: the user leaving the comment
* time: the time in ms of when the comment was left

### Collection: users
Each document in users is information about a specific user
##### Fields
* admin: whether or not the user is an admin
* email: the user's email
* favorites: a list of the sources marked as favorite by the user
