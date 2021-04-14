redis.replicate_commands()
local call = redis.call

local proxy = KEYS[1]
local curr_time = call("TIME")[1]

local servers = call("SCAN", "0", "MATCH", "toastr:server:*")[2]
local uuids = call("HKEYS", "toastr:proxy:" .. proxy .. ":onlines")
local usernames = call("HVALS", "toastr:proxy:" .. proxy .. ":onlines")

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
    call("HSET", "toastr:player:" .. uuid, "lastOnline", curr_time)
end

call("HDEL", "toastr:proxies", proxy)
call("DEL", "toastr:proxy:" .. proxy .. ":onlines")

redis.log(redis.LOG_NOTICE, "[Toastr] removed proxy \"" .. proxy .. "\" because it has been shutdown")