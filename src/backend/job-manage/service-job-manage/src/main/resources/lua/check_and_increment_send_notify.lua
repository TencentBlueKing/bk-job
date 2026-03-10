local system_key = KEYS[1]
local resource_scope_key = KEYS[2]
local user_key = KEYS[3]

local ttl_seconds = tonumber(ARGV[1])
local resource_scope_id = ARGV[2]
local user_id = ARGV[3]
local system_limit = tonumber(ARGV[4])
local resource_scope_limit = tonumber(ARGV[5])
local user_limit = tonumber(ARGV[6])


-- check
if system_limit > 0 then
    local system_count = tonumber(redis.call('GET', system_key) or "0")
    if system_count >= system_limit then
        return "system_quota_limit"
    end
end

if resource_scope_id and resource_scope_id ~= "" and resource_scope_limit > 0 then
    local scope_count = tonumber(redis.call('HGET', resource_scope_key, resource_scope_id) or "0")
    if scope_count >= resource_scope_limit then
        return "resource_scope_quota_limit"
    end
end

if user_id and user_id ~= "" and user_limit > 0 then
    local user_count = tonumber(redis.call('HGET', user_key, user_id) or "0")
    if user_count >= user_limit then
        return "user_quota_limit"
    end
end


-- add
local system_exists = redis.call('EXISTS', system_key)
redis.call('INCR', system_key)
if system_exists == 0 then
    redis.call('EXPIRE', system_key, ttl_seconds)
end

if resource_scope_id and resource_scope_id ~= "" then
    local scope_exists = redis.call('EXISTS', resource_scope_key)
    redis.call('HINCRBY', resource_scope_key, resource_scope_id, 1)
    if scope_exists == 0 then
        redis.call('EXPIRE', resource_scope_key, ttl_seconds)
    end
end

if user_id and user_id ~= "" then
    local user_exists = redis.call('EXISTS', user_key)
    redis.call('HINCRBY', user_key, user_id, 1)
    if user_exists == 0 then
        redis.call('EXPIRE', user_key, ttl_seconds)
    end
end

return "no_limit"
