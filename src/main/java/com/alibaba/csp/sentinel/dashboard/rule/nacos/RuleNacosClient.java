package com.alibaba.csp.sentinel.dashboard.rule.nacos;

import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 推送配置到nacos，以及从nacos获取配置的客户端
 */
@Component
public class RuleNacosClient {

    @Autowired
    private ConfigService configService;


    /**
     * 获取配置
     * @param appName 微服务名称
     * @param ruleDataIdSuffix nacos中规则配置信息对应data id后缀
     * @return 配置规则信息
     * @throws Exception
     */
    public String getRules(String appName, String ruleDataIdSuffix) throws Exception {
        // 以服务名称 拼接 规则配置信息对应data id后缀作为最终data id
        return configService.getConfig(appName + ruleDataIdSuffix,
                RuleNacosConstants.GROUP_ID, 3000);

    }

    /**
     * 推送配置
     * @param app 微服务名称
     * @param ruleDataIdSuffix nacos中规则配置信息对应data id后缀
     * @param rules 配置规则信息
     * @throws Exception
     */
    public boolean publishRules(String app, String ruleDataIdSuffix, String rules) throws Exception {
        return configService.publishConfig(app + ruleDataIdSuffix, RuleNacosConstants.GROUP_ID, rules);
    }
}
