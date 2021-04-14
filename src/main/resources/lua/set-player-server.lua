local call = redis.call

local uuid = ARGV[1]
local newServer = ARGV[2]

local servers = call("SCAN", "0", "MATCH", "toastr:server:*")[2]
if (servers == nil) then
    return
end

for _, srv in ipairs(servers) do
    call("SREM", srv, uuid)
end

call("HSET", "toastr:player:" .. uuid, "server", newServer)
call("SADD", "toastr:server:" .. newServer, uuid)
