local call = redis.call

local proxy = KEYS[1]
local curr_time = call("TIME")[1]

local servers = call("KEYS", "server:*")
local usernames = call("HVALS", "proxy:" .. proxy .. ":onlines")
local uuids = call("HKEYS", "proxy:" .. proxy .. ":onlines")

if (servers == nil or usernames == nil or uuids == nil) then
    return
end

for _, user in ipairs(usernames) do
    -- TODO: do not loop over call usernames, instead:
    -- call("SREM", server, usernames)
    for _, server in ipairs(servers) do
        call("SREM", server, user)
    end
end

for _, uuid in ipairs(uuids) do
    call("HSET", "player:" .. uuid, "lastOnline", curr_time)
end

call("HDEL", "proxies", proxy)
call("DEL", "proxy:" .. proxy .. ":onlines")