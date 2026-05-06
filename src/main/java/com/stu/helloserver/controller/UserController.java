package com.stu.helloserver.controller;

import com.stu.helloserver.common.Result;
import com.stu.helloserver.dto.UserDTO;
import com.stu.helloserver.dto.UserDetailDTO;
import com.stu.helloserver.entity.User;
import com.stu.helloserver.entity.UserInfo;
import com.stu.helloserver.service.UserService;
import com.stu.helloserver.vo.UserDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 注册接口
    @PostMapping
    public Result<String> register(@RequestBody UserDTO userDTO) {
        return userService.register(userDTO);
    }

    // 登录接口
    @PostMapping("/login")
    public Result<String> login(@RequestBody UserDTO userDTO) {
        return userService.login(userDTO);
    }

    // 根据ID查询用户
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // 分页查询用户列表
    @GetMapping("/page")
    public Result<Object> getUserPage(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "5") Integer pageSize) {
        return userService.getUserPage(pageNum, pageSize);
    }

    // 获取用户详情（多表联查+Redis缓存）
    @GetMapping("/detail/{id}")
    public Result<UserDetailDTO> getUserDetailById(@PathVariable Long id) {
        return userService.getUserDetailById(id);
    }

    // 查询用户详情（多表联查 + Redis缓存）
    @GetMapping("/{id}/detail")
    public Result<UserDetailVO> getUserDetail(@PathVariable("id") Long userId) {
        return userService.getUserDetail(userId);
    }

    // 更新用户扩展信息
    @PutMapping("/{id}/detail")
    public Result<String> updateUserInfo(@PathVariable("id") Long userId, @RequestBody UserInfo userInfo) {
        userInfo.setUserId(userId);
        return userService.updateUserInfo(userInfo);
    }

    // 删除用户
    @DeleteMapping("/{id}")
    public Result<String> deleteUser(@PathVariable("id") Long userId) {
        return userService.deleteUser(userId);
    }
}