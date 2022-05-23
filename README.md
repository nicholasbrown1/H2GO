# H2GO
Our application directs users towards the nearest suitable source of drinking water on the UCSB campus. The app should use the device's location to locate the nearest source, and display its location in words, times the source can be accessed, and a picture. The user should also be able to access water source data through a map and filter different source types (drinking fountains/hydration stations)

# Firebase Layout
### Collection: filling_locations
Each document in filling_locations is a pin on the map.
##### Fields
* lat: the latitude value of the filling location  
* long: the longitude value of the filling location  
* title: the label of the pin  
* type: the type of water source (i.e. Water fountain, Hydration Station, etc.)
* floor: what floor the water source is located on (stored as a string to account for things like basements)

### Collection: user_filling_locations
Each document in user_filling_locations is a pin on the map uploaded by a user
##### Fields
* lat: the latitude value of the filling location
* long: the longitude value of the filling location
* title: the label of the pin
* type: the type of water source (i.e. Water fountain, Hydration Station, etc.)
* floor: what floor the water source is located on (stored as a string to account for things like basements)
* approved: whether or not an admin has approved the pin

