# Redis

## 简介
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
    将 daemonize no 修改为 daemonize yes （后台运行）
    
### 指定配置文件启动
    ./redis-server /usr/local/redis-4.0.6/redis.conf
    
### 查看进程
    ps -aux | grep redis
    
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
spring.redis.host=47.112.106.174
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

### 解决本地无法远程连接 Linux 上的 Redis 问题
    vim /usr/local/redis-4.0.6/redis.conf
    # 将 protected-mode yes 改为 protected-mode no （在没有密码的情况下，关闭保护模式）
    # 注释掉 bind 127.0.0.1 （取消绑定本地地址）
    
