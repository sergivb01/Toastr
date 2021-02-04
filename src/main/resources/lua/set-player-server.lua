local call = redis.call

local uuid = ARGV[1]
local username = ARGV[2]
local newServer = ARGV[3]

local servers = call("KEYS", "server:*")
if (servers == nil) then
    return
end

for _, srv in ipairs(servers) do
    call("SREM", srv, username)
end

call("HSET", "player:" .. uuid, "server", newServer)
call("SADD", "server:" .. newServer, username)
