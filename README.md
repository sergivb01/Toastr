# Toastr

Should probably make a good description

* [ ] Replace "return null" to Optional

# Known issues

* All proxy instances need to have the same registered servers, or else there could be some ghost players in a server.
  Root cause: any part of code that uses `ProxyServer.getAllServers()`.

# TODO Features/commands

* [X] migrate to pidgin
* [X] two players can have the same nickname in the Database (remove unique index). If a premium user logs in with a
  cracked nickname, unregister cracked user. When checking for different namecase, ignore "invalid" accounts.
* [X] add thread limit
* [X] global /msg
* [X] MOTD System (or use MiniMOTD/PistonMOTD)

<hr>

* [ ] add queue to retry failed queries
* [ ] check for auto-reconnect in Redis and MySQL
* [ ] maintenance (proxy instance)
* [ ] maintenance (global)
* [ ] AntiVPN
* [ ] sub-servers managers: add/remove/edit servers
* [ ] Auto announcer

# TODO Commands

* Command framework
* Auth
    * [X] clear cache from user (in-memory and redis -> invoke pubsub event)
    * [X] unregister other
* Essentials (with cross-proxy support!)
    * [ ] send
    * [ ] warn when proxy-name is empty/null
