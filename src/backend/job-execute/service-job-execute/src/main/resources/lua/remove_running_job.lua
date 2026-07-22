local system_running_job_zset_key = KEYS[1]
local resource_scope_job_count_hash_key = KEYS[2]
local app_job_count_hash_key = KEYS[3]
local job_weight_hash_key = KEYS[4]
local system_weighted_count_key = KEYS[5]
local job_id = ARGV[1]
local resource_scope = ARGV[2]
local app_code = ARGV[3]

local del_result = redis.call('zrem', system_running_job_zset_key, job_id)
if del_result == 1 then
  -- 权重自描述：从 weight-hash 读取本作业入队时登记的权重；旧任务无记录时默认按 1 回退
  local weight = tonumber(redis.call('hget', job_weight_hash_key, job_id) or "1")
  if weight == nil or weight < 1 then
    weight = 1
  end
  redis.call('hdel', job_weight_hash_key, job_id)

  -- 系统加权计数按权重回退，max(0) 兜底防止旧数据/漂移下溢为负
  local system_weighted_count = tonumber(redis.call('get', system_weighted_count_key) or "0")
  local new_system_weighted_count = system_weighted_count - weight
  if new_system_weighted_count < 0 then
    new_system_weighted_count = 0
  end
  redis.call('set', system_weighted_count_key, new_system_weighted_count)

  local scope_count = tonumber(redis.call('hget', resource_scope_job_count_hash_key, resource_scope) or "0")
  local new_scope_count = scope_count - weight
  if new_scope_count < 0 then
    new_scope_count = 0
  end
  redis.call('hset', resource_scope_job_count_hash_key, resource_scope, new_scope_count)

  if app_code ~= "None" then
    local app_count = tonumber(redis.call('hget', app_job_count_hash_key, app_code) or "0")
    local new_app_count = app_count - weight
    if new_app_count < 0 then
      new_app_count = 0
    end
    redis.call('hset', app_job_count_hash_key, app_code, new_app_count)
  end
end
