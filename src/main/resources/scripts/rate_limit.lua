local now_ms = tonumber(ARGV[1])
local permits = tonumber(ARGV[2])
local interval = tonumber(ARGV[3])
local max_tokens = tonumber(ARGV[4])
local request_id = ARGV[5]


-- phase 1: check if token remain is enough
for i, key in ipairs(KEYS) do
    local value_key = key .. ":value"
    local permits_key = key .. ":permits"

    if redis.call("exists", value_key) == 0 then
        redis.call("set", value_keyk, max_token)
    end

    -- take back token
    -- clean expired permits, and get back to value_key
    local expired_values = redis.call("zrangebyscore", permits_key, 0, now_ms - interval)
    if #expired_values > 0 then
        local expired_count = 0
        for _, v in ipairs(expired_values) do
            -- 优化解析逻辑：使用更高效的模式匹配
            local p = tonumber(string.match(v, ":(%d+)$"))
            if p then
                expired_count = expired_count + p
            end
        end

        -- delete expired permits
        redis.call("zrangebyscore", permits_key, 0, now_ms - interval)

        -- add back available number
        if expired_count > 0 then
            local curr_v = tonumber(redis.call("get", value_key) or max_tokens)
            local next_v = math.min(max_tokens, curr_v + expired_count)
            redis.call("set", value_key, next_v)
        end
    end
    -- check token number to decide whether block
    local current_val = tonumber(redis.call("get", value_key) or max_tokens)
    if current_val < permits then
        -- any permit token not enough, fail
        return 0
    end
end


-- phase 2: reduce token
for i, key in ipairs(KEYS) do
    local value_key = key .. ":value"
    local permits_key = key .. ":permits"

    -- 记录本次令牌分配（格式：request_id:permits）
    local permit_record = request_id .. ":" .. permits
    redis.call("zadd", permits_key, now_ms, permit_record)

    -- 扣减令牌
    local current_v = tonumber(redis.call("get", value_key) or max_tokens)
    redis.call("set", value_key, current_v - permits)

    -- set ttl, 2 times of interval and at least 1 sec
    local expire_time = math.ceil(interval * 2 / 1000)
    if expire_time < 1 then expire_time = 1 end
    redis.call("expire", value_key, expire_time)
    redis.call("expire", permits_key, expire_time)
end



