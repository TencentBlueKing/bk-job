local system_running_job_zset_key = KEYS[1]
local resource_scope_job_count_hash_key = KEYS[2]
local app_job_count_hash_key = KEYS[3]
local resource_scope = ARGV[1]
local app_code = ARGV[2]

local system_limit = tonumber(ARGV[3])
if system_limit > 0 then
  local system_count = tonumber(redis.call('zcard', system_running_job_zset_key) or "0")
  if system_count >= system_limit then
    return "system_quota_limit"
  end
end

local resource_scope_limit = tonumber(ARGV[4])
if resource_scope_limit > 0 then
  local resource_scope_count = tonumber(redis.call('hget', resource_scope_job_count_hash_key, resource_scope) or "0")
  if resource_scope_count >= resource_scope_limit then
    return "resource_scope_quota_limit"
  end
end

if app_code ~= "None" then
  local app_limit = tonumber(ARGV[5])
  if app_limit > 0 then
    local app_count = tonumber(redis.call('hget', app_job_count_hash_key, app_code) or "0")
    if app_count >= app_limit then
      return "app_quota_limit"
    end
  end
end

return "no_limit"
