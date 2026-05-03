package com.ldd.initialization.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 用户登录请求DTO
 */
@Data
public class LoginDTO {
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^\\d{11}$", message = "手机号必须为11位数字")
    private String phone;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^.{6,20}$", message = "密码长度必须在6-20位")
    private String password;

    @NotBlank(message = "验证码不能为空")
    private String captcha;

    @NotBlank(message = "验证码ID不能为空")
    private String captchaId; // 用于标识验证码的唯一ID

    @NotBlank(message = "用户角色不能为空")
    @Pattern(regexp = "^(1|2|3)$", message = "角色类型必须为1(求职者)、2(招聘者)、3(管理员)")
    private Integer roleType;
}