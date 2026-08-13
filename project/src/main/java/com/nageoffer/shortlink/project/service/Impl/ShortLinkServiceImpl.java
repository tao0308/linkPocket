package com.nageoffer.shortlink.project.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValiDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import com.nageoffer.shortlink.project.toolkit.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;

/**
 * 短链接接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;


    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = StrBuilder.create(requestParam.getDomain())
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(requestParam.getDomain())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .createdType(requestParam.getCreatedType())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .describe(requestParam.getDescribe())
                .shortUri(shortLinkSuffix)
                .enableStatus(1)
                .fullShortUrl(fullShortUrl)
                .build();
        ShortLinkGotoDO linkGotoDO = ShortLinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try{
            baseMapper.insert(shortLinkDO);

            shortLinkGotoMapper.insert(linkGotoDO);
        } catch (DuplicateKeyException ex){
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl);
            ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
            if(hasShortLinkDO != null){
                log.warn("短链接: {} 重复入库",fullShortUrl);
                throw new ServiceException("短链接生成重复");
            }
        }
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()), TimeUnit.MILLISECONDS
        );//缓存预热
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(shortLinkDO.getOriginUrl())
                .gid(shortLinkDO.getGid())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0);
        ShortLinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO ==null){
            throw new ClientException("短链接记录不存在");
        }
        ShortLinkDO shortLinkDO = ShortLinkDO.builder()
                .domain(hasShortLinkDO.getDomain())
                .shortUri(hasShortLinkDO.getShortUri())
                .clickNum(hasShortLinkDO.getClickNum())
                .favicon(hasShortLinkDO.getFavicon())
                .createdType(hasShortLinkDO.getCreatedType())
                .gid(requestParam.getGid())
                .originUrl(requestParam.getOriginUrl())
                .describe(requestParam.getDescribe())
                .validDateType(requestParam.getValidDateType())
                .validDate(requestParam.getValidDate())
                .build();
        if (Objects.equals(hasShortLinkDO.getGid(), requestParam.getGid())){
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, requestParam.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .set(Objects.equals(requestParam.getValidDateType(), ValiDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);

            baseMapper.update(shortLinkDO, updateWrapper);
        } else {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                    .eq(ShortLinkDO::getGid, hasShortLinkDO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 1)
                    .eq(ShortLinkDO::getDelFlag, 0);
            baseMapper.delete(updateWrapper);
            baseMapper.insert(shortLinkDO);
        }
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .eq(ShortLinkDO::getGid, requestParam.getGid())
                .eq(ShortLinkDO::getEnableStatus, 1)
                .eq(ShortLinkDO::getDelFlag, 0)
                .orderByDesc(ShortLinkDO::getCreateTime);
        IPage<ShortLinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each ->{
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain("http://" + result.getDomain());
            return result;
        });
    }

    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> requestParam) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid, count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 1)
                .eq("del_flag", 0)
                .groupBy("gid");
        List<Map<String, Object>> shortLinkDOList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(shortLinkDOList,ShortLinkGroupCountQueryRespDTO.class);
    }

    /**
     * 短链接跳转：根据短链接 URI 重定向到原始 URL
     * <p>
     * 整体流程（缓存 + 双重检查锁 防缓存击穿）：
     * <ol>
     *   <li>拼接完整短链接 → 查 Redis 缓存</li>
     *   <li>缓存命中 → 直接 302 跳转（快速路径）</li>
     *   <li>缓存未命中 → 加分布式锁 → 再次查缓存（双重检查）</li>
     *   <li>仍不命中 → 查 t_link_goto 获取 gid → 查 t_link 获取 origin_url</li>
     *   <li>查到结果 → 写入 Redis 缓存 → 302 跳转</li>
     *   <li>finally 释放锁</li>
     * </ol>
     *
     * @param shortUri 短链接路径，如 "abc123"
     * @param request  原生 Servlet 请求对象，用于获取域名
     * @param response 原生 Servlet 响应对象，用于发送 302 重定向
     */
    @SneakyThrows  // Lombok 注解：自动将 checked exception 转为 unchecked，简化代码
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {

        // ==================== 1. 拼接完整的短链接 ====================
        // 从请求中获取域名（如 s.naoffer.com），与短 URI 拼接成完整短链接
        // 例如：serverName="s.naoffer.com", shortUri="abc123" → "s.naoffer.com/abc123"
        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;

        // ==================== 2. 第一次查 Redis 缓存 ====================
        // 用 String.format 将 %s 替换为完整短链接，得到 Redis key：short-link_goto_s.naoffer.com/abc123
        // 尝试从 Redis 获取对应的原始链接（创建短链接时写入的缓存）
        String originalLink = stringRedisTemplate.opsForValue()
                .get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));

        // ==================== 3. 缓存命中 → 快速返回 ====================
        // 如果 Redis 中有数据，说明这个短链接之前被访问过，直接 302 跳转，无需查 DB
        if (StrUtil.isNotBlank(originalLink)) {
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;  // 直接结束，后续逻辑不再执行
        }

        boolean contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl); // 判断短链接是否在布隆过滤器中
        if (!contains) { // 如果布隆过滤器中不包含该短链接
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return; // 直接返回，不进行后续处理
        }

        String gotoIsNullShortLink = stringRedisTemplate.opsForValue()
                .get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl)); // 尝试从 Redis 获取对应的原始链接
        if (StrUtil.isNotBlank(gotoIsNullShortLink)){ // 如果 Redis 中有数据，说明这个短链接之前被访问过，直接 302 跳转，无需查 DB
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }

        // ==================== 4. 缓存未命中 → 加分布式锁 ====================
        // 获取一把基于 Redis 的分布式锁，key 如：short-link_lock_goto_s.naoffer.com/abc123
        // 为什么需要锁？防止缓存击穿——
        //   假设热门链接缓存刚好过期，同一瞬间来了 1000 个请求，
        //   如果没有锁，1000 个请求全部穿透缓存去打 DB，可能把数据库压垮。
        //   有了锁，同一时刻只有一个线程能进临界区查 DB 并重建缓存，
        //   其他线程排队等待，等缓存建好后直接从 Redis 拿。
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();  // 阻塞等待，直到获取到锁
        try {

            // ==================== 5. 双重检查（Double Check） ====================
            // 再次查 Redis 缓存。
            // 为什么要再查一次？——可能在你排队等锁的时候，前面拿到锁的线程
            // 已经查完 DB 并把结果写入了 Redis 缓存。再查一次可以避免重复查 DB。
            originalLink = stringRedisTemplate.opsForValue()
                    .get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));

            // 如果前面排队时缓存已经被重建好了，直接 302 跳转，跳过 DB 查询
            if (StrUtil.isNotBlank(originalLink)) {
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }

            // ==================== 6. 缓存确实没有 → 查数据库 ====================

            // 6.1 第一步：查 t_link_goto 路由表（按 full_short_url 分片）
            // 为什么需要这一步？因为 t_link 主表是按 gid 做 hash 分片的（16 张表），
            // 不知道 gid 就无法定位到具体分片，必须全表扫描。
            // t_link_goto 是轻量级路由表，按 full_short_url 分片，只存 3 列：
            //   id（主键）、gid（分组标识）、full_short_url（完整短链接）
            // 通过 full_short_url 查出 gid，就能在 t_link 中精准定位到对应分片
            LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers
                    .lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);  // WHERE full_short_url = ?
            ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);

            // 路由表中也没有 → 说明这个短链接不存在（可能是恶意扫描或瞎编的）
            if (shortLinkGotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30, TimeUnit.MINUTES); // 将不存在的短链接缓存 30 分钟
                // 严谨来说此处需要对不存在的短链接做风控/限流
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }

            // 6.2 第二步：拿到 gid 后，查 t_link 主表
            // 查询条件：
            //   - gid 匹配（通过分片键定位到正确的分片表）
            //   - full_short_url 匹配
            //   - enable_status = 1（只查启用状态的链接）
            //   - del_flag = 0（排除软删除的记录）
            LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers
                    .lambdaQuery(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkGotoDO.getGid())         // 分片键
                    .eq(ShortLinkDO::getFullShortUrl, fullShortUrl)            // 完整短链接
                    .eq(ShortLinkDO::getEnableStatus, 1)                        // 启用状态
                    .eq(ShortLinkDO::getDelFlag, 0);                           // 未删除
            ShortLinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);

            // 查到了有效的短链接记录
            if (shortLinkDO != null) {

                if(shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
                    stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl),"-",30, TimeUnit.MINUTES); // 将不存在的短链接缓存 30 分钟
                    ((HttpServletResponse) response).sendRedirect("/page/notfound");
                    return;
                }


                // 6.3 将原始链接写入 Redis 缓存（缓存预热）
                // 这样下次再有人访问同一个短链接，直接从 Redis 拿，不用再查 DB

                stringRedisTemplate.opsForValue().set(
                        String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                        shortLinkDO.getOriginUrl(),
                        LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS
                );//缓存预热

                // 6.4 发送 HTTP 302 临时重定向
                // 浏览器收到 302 响应后会自动跳转到原始 URL
                ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
            }

        } finally {
            // ==================== 7. 释放锁 ====================
            // 无论业务逻辑成功还是抛异常，都必须释放锁，否则其他线程会永远等下去（死锁）
            lock.unlock();
        }
    }

    private String generateSuffix(ShortLinkCreateReqDTO requestParam){
        int customGenerateCount = 0;
        String shortUri;
        while(true){
            if (customGenerateCount>10){
                throw new ServiceException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            originUrl+=System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            if(!shortUriCreateCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
            customGenerateCount++;

        }
        return shortUri;
    }
}
