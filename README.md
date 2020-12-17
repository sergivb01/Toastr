# Toastr

Should probably make a good description

* [ ] Replace "return null" to Optional

# TODO Features/commands

* [X] migrate to pidgin
* [ ] two players can have the same nickname in the Database (remove unique index). If a premium user logs in with a cracked nickname, unregister cracked user. When checking for different namecase, ignore "invalid" accounts.
* [ ] add thread limit
* [ ] add queue to retry failed queries
* [ ] global /msg
* [ ] stafflist
* [ ] maintenance (proxy instance)
* [ ] maintenance (global)
* [ ] MOTD System (or use MiniMOTD/PistonMOTD)
* [ ] AntiVPN
* [ ] sub-servers managers: add/remove/edit servers
* [ ] Auto announcer

# TODO Commands

* Command framework
* Auth
    * [ ] clear cache from user (in-memory and redis -> invoke pubsub event)
    * [ ] register other
    * [ ] unregister other
    * [ ] changepassword other
* Essentials (with cross-proxy support!)
    * [ ] send
