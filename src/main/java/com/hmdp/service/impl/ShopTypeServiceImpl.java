package com.hmdp.service.impl;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 1~5分钟随机
    private int randomTtl = ThreadLocalRandom.current().nextInt(1, 6);

    @Override
    public List<ShopType> typeList() {
        String key = RedisConstants.CACHE_SHOP_TYPE_KEY;
        // 从redis中查询缓存
        List<String> typeListString = stringRedisTemplate.opsForList().range(key, 0, -1);
        if (typeListString != null&& !typeListString.isEmpty()) {
            // 返回
            return typeListString.stream().map(item -> JSONUtil.toBean(item, ShopType.class))
                    .collect(Collectors.toList());
        }
        // 不存在，查询数据库
        List<ShopType> typeList = query().list();
        // 数据库中不存在，返回空集合
        if(typeList == null || typeList.isEmpty()){
            return Collections.emptyList();
        }
        // 存在，写入redis并返回
        // 要先把 Java 对象集合转成 JSON 字符串集合
        typeListString = typeList.stream()
                .map(JSONUtil::toJsonStr)
                .collect(Collectors.toList());
        // 写入redis
        stringRedisTemplate.opsForList().rightPushAll(key, typeListString);
        stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL + randomTtl, TimeUnit.MINUTES);
        return typeList;
    }
}
