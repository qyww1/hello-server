package com.stu.helloserver.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stu.helloserver.common.Result;
import com.stu.helloserver.common.ResultCode;
import com.stu.helloserver.dto.UserDTO;
import com.stu.helloserver.dto.UserDetailDTO;
import com.stu.helloserver.entity.User;
import com.stu.helloserver.entity.UserInfo;
import com.stu.helloserver.mapper.UserMapper;
import com.stu.helloserver.mapper.UserInfoMapper;
import com.stu.helloserver.security.JwtUtil;
import com.stu.helloserver.service.UserService;
import com.stu.helloserver.vo.UserDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final String CACHE_KEY_PREFIX = "user:detail:";

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private UserInfoMapper userInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public Result<String> register(UserDTO userDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userDTO.getUsername());
        User dbUser = userMapper.selectOne(queryWrapper);

        if (dbUser != null) {
            return Result.error(ResultCode.USER_HAS_EXISTED);
        }

        User user = new User(userDTO.getUsername(), userDTO.getPassword());
        userMapper.insert(user);
        return Result.success("注册成功");
    }

    @Override
    public Result<String> login(UserDTO userDTO) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, userDTO.getUsername());
        User dbUser = userMapper.selectOne(queryWrapper);

        if (dbUser == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        if (!dbUser.getPassword().equals(userDTO.getPassword())) {
            return Result.error(ResultCode.PASSWORD_ERROR);
        }
        String jwt = jwtUtil.generateToken(dbUser.getUsername());
        return Result.success(jwt);
    }

    @Override
    public Result<User> getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }
        // 关联查询UserInfo
        LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
        userInfoQueryWrapper.eq(UserInfo::getUserId, user.getId());
        UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);
        user.setUserInfo(userInfo);
        return Result.success(user);
    }

    @Override
    public Result<Object> getUserPage(Integer pageNum, Integer pageSize) {
        try {
            Page<User> pageParam = new Page<>(pageNum, pageSize);
            Page<User> resultPage = userMapper.selectPage(pageParam, null);
            // 为每个用户关联UserInfo
            resultPage.getRecords().forEach(user -> {
                LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
                userInfoQueryWrapper.eq(UserInfo::getUserId, user.getId());
                UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);
                user.setUserInfo(userInfo);
            });
            return Result.success(resultPage);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(ResultCode.ERROR);
        }
    }

    @Override
    public Result<UserDetailDTO> getUserDetailById(Long id) {
        // 定义缓存键
        String cacheKey = "user:detail:" + id;

        // 1. 先查询缓存
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json != null && !json.isBlank()) {
            try {
                // 缓存命中，直接返回
                UserDetailDTO userDetailDTO = JSONUtil.toBean(json, UserDetailDTO.class);
                return Result.success(userDetailDTO);
            } catch (Exception e) {
                // 缓存数据异常，删除脏缓存，继续查询数据库
                redisTemplate.delete(cacheKey);
            }
        }

        // 2. 缓存未命中，查询数据库
        User user = userMapper.selectById(id);
        if (user == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 3. 查询用户详细信息
        LambdaQueryWrapper<UserInfo> userInfoQueryWrapper = new LambdaQueryWrapper<>();
        userInfoQueryWrapper.eq(UserInfo::getUserId, user.getId());
        UserInfo userInfo = userInfoMapper.selectOne(userInfoQueryWrapper);

        // 4. 组装返回结果
        UserDetailDTO userDetailDTO = new UserDetailDTO(user, userInfo);

        // 5. 写入缓存，设置过期时间为30分钟
        redisTemplate.opsForValue().set(cacheKey, JSONUtil.toJsonStr(userDetailDTO), 30, TimeUnit.MINUTES);

        return Result.success(userDetailDTO);
    }

    @Override
    public Result<UserDetailVO> getUserDetail(Long userId) {
        String key = CACHE_KEY_PREFIX + userId;

        // 1. 先查缓存（带降级处理）
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json != null && !json.isBlank()) {
                try {
                    UserDetailVO cacheVO = JSONUtil.toBean(json, UserDetailVO.class);
                    return Result.success(cacheVO);
                } catch (Exception e) {
                    // 缓存数据异常，删除脏缓存，继续查询数据库
                    redisTemplate.delete(key);
                }
            }
        } catch (Exception e) {
            // Redis连接失败，跳过缓存，直接查询数据库
        }

        // 2. 查询数据库
        UserDetailVO detail = userInfoMapper.getUserDetail(userId);
        if (detail == null) {
            return Result.error(ResultCode.USER_NOT_EXIST);
        }

        // 3. 写缓存（带降级处理）
        try {
            redisTemplate.opsForValue().set(
                    key,
                    JSONUtil.toJsonStr(detail),
                    10,
                    TimeUnit.MINUTES
            );
        } catch (Exception e) {
            // Redis连接失败，跳过写缓存
        }

        return Result.success(detail);
    }

    @Override
    @Transactional
    public Result<String> updateUserInfo(UserInfo userInfo) {
        if (userInfo == null || userInfo.getUserId() == null) {
            return Result.error(ResultCode.PARAM_ERROR);
        }

        // 先操作DB
        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getUserId, userInfo.getUserId());
        UserInfo existingInfo = userInfoMapper.selectOne(queryWrapper);

        if (existingInfo != null) {
            // 更新
            userInfo.setId(existingInfo.getId());
            userInfoMapper.updateById(userInfo);
        } else {
            // 插入
            userInfoMapper.insert(userInfo);
        }

        // 成功后删除缓存（保证一致性）
        String key = CACHE_KEY_PREFIX + userInfo.getUserId();
        redisTemplate.delete(key);

        return Result.success("更新成功");
    }

    @Override
    @Transactional
    public Result<String> deleteUser(Long userId) {
        if (userId == null) {
            return Result.error(ResultCode.PARAM_ERROR);
        }

        // 先操作DB
        userMapper.deleteById(userId);

        LambdaQueryWrapper<UserInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserInfo::getUserId, userId);
        userInfoMapper.delete(queryWrapper);

        // 成功后删除缓存（保证一致性）
        String key = CACHE_KEY_PREFIX + userId;
        redisTemplate.delete(key);

        return Result.success("删除成功");
    }
}