# H2GO
Our application directs users towards the nearest suitable source of drinking water on the UCSB campus. The app should use the device's location to locate the nearest source, and display its location in words, times the source can be accessed, and a picture. The user should also be able to access water source data through a map and filter different source types (drinking fountains/hydration stations)

# Firebase Layout
### Collection: filling_locations
Each document in filling_locations is a pin on the map.
##### Fields
lat: the latitude value of the filling location
long: the longitude value of the filling location
title: the label of the pin