local system_running_job_zset_key = KEYS[1]
local resource_scope_job_count_hash_key = KEYS[2]
local app_job_count_hash_key = KEYS[3]
local job_weight_hash_key = KEYS[4]
local system_weighted_count_key = KEYS[5]
local job_id = ARGV[1]
local resource_scope = ARGV[2]
local app_code = ARGV[3]
local job_create_time = tonumber(ARGV[4])
local weight = tonumber(ARGV[5])
if weight == nil or weight < 1 then
  weight = 1
end

local add_result = redis.call('zadd', system_running_job_zset_key, 'NX', job_create_time, job_id)
if add_result == 1 then
  -- 记忆本作业权重，供 remove/崩溃回收在不知权重的情况下精确回退
  redis.call('hset', job_weight_hash_key, job_id, weight)
  redis.call('incrby', system_weighted_count_key, weight)
  redis.call('hincrby', resource_scope_job_count_hash_key, resource_scope, weight)

  if app_code ~= "None" then
    redis.call('hincrby', app_job_count_hash_key, app_code, weight)
  end
end
