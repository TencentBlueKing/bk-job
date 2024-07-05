local system_running_job_zset_key = KEYS[1]
local resource_scope_job_count_hash_key = KEYS[2]
local app_job_count_hash_key = KEYS[3]
local job_id = ARGV[1]
local resource_scope = ARGV[2]
local app_code = ARGV[3]

local del_result = redis.call('zrem', system_running_job_zset_key, job_id)
if del_result == 1 then
  redis.call('hincrby', resource_scope_job_count_hash_key, resource_scope, -1)

  if app_code ~= "None" then
    redis.call('hincrby', app_job_count_hash_key, app_code, -1)
  end
end
