local call = redis.call

local time = call("TIME")[1]
call("HSET", "proxies", KEYS[1], time)

local curr_time = tonumber(time)
local proxies = call("HGETALL", "proxies")
local total = 0

for i = 1, #proxies, 2 do
    local key = proxies[i]
    local n = tonumber(proxies[i + 1])

    if n and n + 5 >= curr_time then
        total = total + call("HLEN", "proxy:" .. key .. ":onlines")
    end
end

return tonumber(total)