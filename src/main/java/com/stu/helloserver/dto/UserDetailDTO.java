package com.stu.helloserver.dto;

import com.stu.helloserver.entity.User;
import com.stu.helloserver.entity.UserInfo;
import lombok.Data;

@Data
public class UserDetailDTO {
    private User user;
    private UserInfo userInfo;

    public UserDetailDTO() {}

    public UserDetailDTO(User user, UserInfo userInfo) {
        this.user = user;
        this.userInfo = userInfo;
    }
}
