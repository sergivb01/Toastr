local call = redis.call

local username = ARGV[1]

local servers = call("KEYS", "server:*")
if (servers == nil) then
    return
end

for _, srv in ipairs(servers) do
    call("SREM", srv, username)
end
