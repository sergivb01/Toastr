# Toastr

Should probably make a good description

* [ ] Replace "return null" to Optional

# Known issues

* All proxy instances need to have the same registered servers, or else there could be some ghost players in a server.
  Root cause: any part of code that uses `ProxyServer.getAllServers()`.
