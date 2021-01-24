local call = redis.call

local curr_time = tonumber(call("TIME")[1])

local proxies = call("HGETALL", "proxies")
local list = {}

for i = 1, #proxies, 2 do
    local key = proxies[i]
    local n = tonumber(proxies[i + 1])

    if n and n + 5 >= curr_time then
        local players = call("HVALS", "proxy:" .. key .. ":onlines")

        for _, v in pairs(players) do
            table.insert(list, v)
        end
    end
end

return list