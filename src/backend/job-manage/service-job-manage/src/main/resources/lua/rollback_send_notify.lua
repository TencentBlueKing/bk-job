local system_key = KEYS[1]
local resource_scope_key = KEYS[2]
local user_key = KEYS[3]

local resource_scope_id = ARGV[1]
local user_id = ARGV[2]

local v = tonumber(redis.call('GET', system_key) or "0")
if v > 0 then
    redis.call('DECR', system_key)
end

if resource_scope_id and resource_scope_id ~= "" then
    local v = tonumber(redis.call('HGET', resource_scope_key, resource_scope_id) or "0")
    if v > 0 then
        redis.call('HINCRBY', resource_scope_key, resource_scope_id, -1)
    end
end

if user_id and user_id ~= "" then
    local v = tonumber(redis.call('HGET', user_key, user_id) or "0")
    if v > 0 then
        redis.call('HINCRBY', user_key, user_id, -1)
    end
end
