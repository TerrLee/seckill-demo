local stock = redis.call('get',KEYS[1])
if(not stock) then return -1 end
if(tonumber(stock)<=0)then return 0 end

--2.查重（必须写在扣减之前）
local userSetKey = "seckill:order:"..KEYS[1]
local isBuy = redis.call("sismember",userSetKey,ARGV[1])
if(isBuy == 1) then return -2 end

--3，扣减库存（只有没买过的人，才有资格走到这一步）
redis.call("decr",KEYS[1])

--4.记录用户已经购买

redis.call('sadd',userSetKey,ARGV[1])
return 1