# Toastr

Should probably make a good description

* [ ] Replace "return null" to Optional

# Redis docs:

* Proxy list `toastr:proxies`. Contains a map of <String, Long>(name, heartbeat)
* Proxy data `toastr:proxy:_proxy:onlines`. Contains a map of <UUID, String>(PlayerID, Username).
* Server data `toastr:server:_server_`. Contains a set of <UUID>(Username).
* Player data `toastr:player:_uuid_`. Contains a map of <String, Object> of player data
* Resolver data `toastr:resolver:_username_`. Contains a map of <String, Object> of the resolver data