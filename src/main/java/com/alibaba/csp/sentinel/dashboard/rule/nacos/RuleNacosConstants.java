package com.alibaba.csp.sentinel.dashboard.rule.nacos;

/**
 * 规则配置dataId常量
 */
public class RuleNacosConstants {

    /** 规则配置在nacos中Group名称 */
    public static final String GROUP_ID = "SENTINEL_GROUP";

    /** 流控规则配置在nacos中的data Id后缀 */
    public static final String FLOW_DATA_ID = ".sentinel.rule.flow";

    /** 熔断规则配置在nacos中的data Id后缀 */
    public static final String DEGRADE_DATA_ID = ".sentinel.rule.degrade";

    /** 热点规则配置在nacos中的data Id后缀 */
    public static final String PARAM_DATA_ID = ".sentinel.rule.param";

    /** 系统规则配置在nacos中的data Id后缀 */
    public static final String SYSTEM_DATA_ID = ".sentinel.rule.system";

    /** 授权规则配置在nacos中的data Id后缀 */
    public static final String AUTHORITY_DATA_ID = ".sentinel.rule.authority";

    /** 网关api管理配置在nacos中的data Id后缀 */
    public static final String GATEWAY_API_DATA_ID = ".sentinel.rule.gateway.api";

    /** 网关流控规则配置在nacos中的data Id后缀 */
    public static final String GATEWAY_FLOW_DATA_ID = ".sentinel.rule.gateway.flow";
}
