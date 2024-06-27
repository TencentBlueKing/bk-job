local system_running_job_zset_key = KEYS[1]
local resource_scope_job_count_hash_key = KEYS[2]
local app_job_count_hash_key = KEYS[3]
local job_id = ARGV[1]
local resource_scope = ARGV[2]
local app_code = ARGV[3]
local job_create_time = tonumber(ARGV[4])

local add_result = redis.call('zadd', system_running_job_zset_key, 'NX', job_create_time, job_id)
if add_result == 1 then
  redis.call('hincrby', resource_scope_job_count_hash_key, resource_scope, 1)

  if app_code ~= "None" then
    redis.call('hincrby', app_job_count_hash_key, app_code, 1)
  end
end
