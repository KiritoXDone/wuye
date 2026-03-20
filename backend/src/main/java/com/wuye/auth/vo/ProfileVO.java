package com.wuye.auth.vo;

import java.util.List;

public class ProfileVO {

    private Long accountId;
    private String accountType;
    private String productRole;
    private String realName;
    private List<String> roles;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getProductRole() {
        return productRole;
    }

    public void setProductRole(String productRole) {
        this.productRole = productRole;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

}
