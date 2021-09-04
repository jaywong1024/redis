# Redis

### 简介
- Redis支持数据的持久化，可以将内存中的数据保存在磁盘中，重启的时候可以再次加载进行使用
- Redis不仅仅支持简单的key-value类型的数据，同时还提供list，set，zset，hash等数据结构的存储
- Redis支持数据的备份，即master-slave模式的数据备份

## 在 Linux 上安装 Redis

### 下载 Redis
    wget http://download.redis.io/releases/redis-4.0.6.tar.gz
    
### 解压到 /usr/local 中
    tar zxvf redis-4.0.6.tar.gz
    mv redis-4.0.6 /usr/local/
    
### 因为 Redis 是使用 C语言 编写的，所以编译是需要 gcc
    # yum install gcc -y
    
### 编译安装
    make MALLOC=libc
    cd src && make install

### 修改配置文件 redis.conf
    vim /usr/local/redis-4.0.6/redis.conf
    # 将 daemonize no 修改为 daemonize yes （后台运行）
    
### 指定配置文件启动
    ./redis-server /usr/local/redis-4.0.6/redis.conf
    
### 查看进程
    ps -aux | grep redis
    
### 解决本地无法远程连接 Linux 上的 Redis 问题
    vim /usr/local/redis-4.0.6/redis.conf
    # 将 protected-mode yes 改为 protected-mode no （在没有密码的情况下，关闭保护模式）
    # 注释掉 bind 127.0.0.1 （取消绑定本地地址）
    
## 利用 Docker 安装 Redis

### 拉取镜像
    #默认拉取最新版本
    $ docker pull redis
    $ docker pull redis:latest
    #拉取指定版本
    $ docker pull redis:6.2.5
    
### 查看镜像
    $ docker images
    
### 运行容器
    $ docker run -itd --name redis-test -p 6379:6379 redis
    
### 查看进程
    $ docker ps
    
### 进入控制台
    $ docker exec -it redis-test /bin/bash
    
## 本地客户端推荐

https://github.com/qishibo/AnotherRedisDesktopManager/releases

## Spring Boot 整合 Redis

### 引入依赖
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 配置文件
```properties
# Redis数据库索引（默认为0）
spring.redis.database=0
# Redis服务器地址
spring.redis.host=127.0.0.1
# Redis服务器连接端口
spring.redis.port=6379
# Redis服务器连接密码（默认为空）
spring.redis.password=
# 连接池最大连接数（使用负值表示没有限制）
spring.redis.jedis.pool.max-active=200
# 连接池最大阻塞等待时间（使用负值表示没有限制）
# spring.redis.jedis.pool.max-wait=-1
# 连接池中的最大空闲连接
# spring.redis.jedis.pool.max-idle=10
# 连接池中的最小空闲连接
# spring.redis.jedis.pool.min-idle=0
# 连接超时时间（毫秒）
# spring.redis.timeout=3000
```

### 配置类
#### 因为 RedisAutoConfiguration 类中使用了 @ConditionalOnMissingBean(name = "redisTemplate") 注解，所以我们可以配置自己的 RedisTemplate 类，实现一些定制功能，例如：
   - 返回的 RedisTemplate 的泛型是 <String, Object> 的，可以减少不必要的类型转换操作
   - 设置 Key 和 Value 的序列化方式
```java
@Configuration
public class RedisConfig {

    /**
     * @SuppressWarnings 该批注的作用是给编译器一条指令，告诉它对被批注的代码元素内部的某些警告保持静默
     *
     *    关键字         用途
     *    deprecation   使用了不赞成使用的类或方法时的警告
     *    unchecked     执行了未检查的转换时的警告，例如当使用集合时没有用泛型 (Generics) 来指定集合保存的类型。
     *    fallthrough   当 Switch 程序块直接通往下一种情况而没有 Break 时的警告。
     *    path          在类路径、源文件路径等中有不存在的路径时的警告。
     *    serial        当在可序列化的类上缺少 serialVersionUID 定义时的警告。
     *    finally       任何 finally 子句不能正常完成时的警告。
     *    all           关于以上所有情况的警告。
     */

//    注入 Spring 容器中，忽略所有警告
    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
//        key 采用 String 的序列化方式
        template.setKeySerializer(stringRedisSerializer);
//        hash 的 key 也采用 String 的序列化方式
        template.setHashKeySerializer(stringRedisSerializer);
//        value 的序列化方式采用 JSON
        template.setValueSerializer(jackson2JsonRedisSerializer);
//        hash value 的序列化方式也采用 JSON
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        template.afterPropertiesSet();
        return template;
    }

}
```
### 工具类
```java
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间（秒）
     * @return true / false
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据 key 获取过期时间
     * @param key 键
     * @return
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 判断 key 是否存在
     * @param key 键
     * @return true / false
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除缓存
     * @SuppressWarnings("unchecked") 忽略类型转换警告
     * @param key 键（一个或者多个）
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
//                传入一个 Collection<String> 集合
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }

//    ============================== String ==============================

    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true / false
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间（秒），如果 time < 0 则设置无限时间
     * @return true / false
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递增
     * @param key 键
     * @param delta 递增大小
     * @return
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于 0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     * @param key 键
     * @param delta 递减大小
     * @return
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于 0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }

//    ============================== Map ==============================

    /**
     * HashGet
     * @param key 键（no null）
     * @param item 项（no null）
     * @return 值
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }

    /**
     * 获取 key 对应的 map
     * @param key 键（no null）
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * HashSet
     * @param key 键
     * @param map 值
     * @return true / false
     */
    public boolean hmset(String key, Map<Object, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * HashSet 并设置时间
     * @param key 键
     * @param map 值
     * @param time 时间
     * @return true / false
     */
    public boolean hmset(String key, Map<Object, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张 Hash表 中放入数据，如不存在则创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true / false
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 向一张 Hash表 中放入数据，并设置时间，如不存在则创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间（如果原来的 Hash表 设置了时间，这里会覆盖）
     * @return true / false
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除 Hash表 中的值
     * @param key 键
     * @param item 项（可以多个，no null）
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }

    /**
     * 判断 Hash表 中是否有该键的值
     * @param key 键（no null）
     * @param item 值（no null）
     * @return true / false
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }

    /**
     * Hash递增，如果不存在则创建一个，并把新增的值返回
     * @param key 键
     * @param item 项
     * @param by 递增大小 > 0
     * @return
     */
    public Double hincr(String key, String item, Double by) {
        return redisTemplate.opsForHash().increment(key, item, by);
    }

    /**
     * Hash递减
     * @param key 键
     * @param item 项
     * @param by 递减大小
     * @return
     */
    public Double hdecr(String key, String item, Double by) {
        return redisTemplate.opsForHash().increment(key, item, -by);
    }

//    ============================== Set ==============================

    /**
     * 根据 key 获取 set 中的所有值
     * @param key 键
     * @return 值
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从键为 key 的 set 中，根据 value 查询是否存在
     * @param key 键
     * @param value 值
     * @return true / false
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将数据放入 set缓存
     * @param key 键值
     * @param values 值（可以多个）
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 将数据放入 set缓存，并设置时间
     * @param key 键
     * @param time 时间
     * @param values 值（可以多个）
     * @return 成功放入个数
     */
    public long sSet(String key, long time, Object... values) {
        try {
            long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取 set缓存的长度
     * @param key 键
     * @return 长度
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 移除 set缓存中，值为 value 的
     * @param key 键
     * @param values 值
     * @return 成功移除个数
     */
    public long setRemove(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().remove(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

//    ============================== List ==============================

    /**
     * 获取 list缓存的内容
     * @param key 键
     * @param start 开始
     * @param end 结束（0 到 -1 代表所有值）
     * @return
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取 list缓存的长度
     * @param key 键
     * @return 长度
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 根据索引 index 获取键为 key 的 list 中的元素
     * @param key 键
     * @param index 索引
     *              当 index >= 0 时 {0:表头, 1:第二个元素}
     *              当 index < 0 时 {-1:表尾, -2:倒数第二个元素}
     * @return 值
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 将值 value 插入键为 key 的 list 中，如果 list 不存在则创建空 list
     * @param key 键
     * @param value 值
     * @return true / false
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将值 value 插入键为 key 的 list 中，并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间
     * @return true / false
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将 values 插入键为 key 的 list 中
     * @param key 键
     * @param values 值
     * @return true / false
     */
    public boolean lSetList(String key, List<Object> values) {
        try {
            redisTemplate.opsForList().rightPushAll(key, values);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将 values 插入键为 key 的 list 中，并设置时间
     * @param key 键
     * @param values 值
     * @param time 时间
     * @return true / false
     */
    public boolean lSetList(String key, List<Object> values, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 根据索引 index 修改键为 key 的值
     * @param key 键
     * @param index 索引
     * @param value 值
     * @return true / false
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 在键为 key 的 list 中删除值为 value 的元素
     * @param key 键
     * @param count 如果 count == 0 则删除 list 中所有值为 value 的元素
     *              如果 count > 0 则删除 list 中最左边那个值为 value 的元素
     *              如果 count < 0 则删除 list 中最右边那个值为 value 的元素
     * @param value
     * @return
     */
    public long lRemove(String key, long count, Object value) {
        try {
            return redisTemplate.opsForList().remove(key, count, value);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

}
```
