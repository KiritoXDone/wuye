package com.wuye.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AppAuthProperties {

    private final Wechat wechat = new Wechat();

    public Wechat getWechat() {
        return wechat;
    }

    public static class Wechat {
        private String mode = "real";
        private String appId = "";
        private String appSecret = "";
        private String jscode2sessionUrl = "https://api.weixin.qq.com/sns/jscode2session";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getJscode2sessionUrl() {
            return jscode2sessionUrl;
        }

        public void setJscode2sessionUrl(String jscode2sessionUrl) {
            this.jscode2sessionUrl = jscode2sessionUrl;
        }
    }
}
