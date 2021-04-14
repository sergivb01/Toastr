local call = redis.call

local proxy = KEYS[1]
local curr_time = call("TIME")[1]

local servers = call("SCAN", "0", "MATCH", "toastr:server:*")[2]
local uuids = call("HKEYS", "toastr:proxy:" .. proxy .. ":onlines")

if (servers == nil or uuids == nil) then
    return
end

for _, uuid in ipairs(uuids) do
    -- TODO: do not loop over call usernames, instead:
    -- call("SREM", server, uuids)
    for _, server in ipairs(servers) do
        call("SREM", server, uuid)
    end
end

for _, uuid in ipairs(uuids) do
    call("HSET", "toastr:player:" .. uuid, "lastOnline", curr_time)
end

call("HDEL", "toastr:proxies", proxy)
call("DEL", "toastr:proxy:" .. proxy .. ":onlines")

redis.log(redis.LOG_NOTICE, "[Toastr] removed proxy \"" .. proxy .. "\" because it has been shutdown")