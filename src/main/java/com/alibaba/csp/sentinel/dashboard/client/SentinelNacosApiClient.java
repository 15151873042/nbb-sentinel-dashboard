/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.csp.sentinel.dashboard.client;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.ApiDefinitionEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.gateway.GatewayFlowRuleEntity;
import com.alibaba.csp.sentinel.dashboard.datasource.entity.rule.*;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.RuleNacosClient;
import com.alibaba.csp.sentinel.dashboard.rule.nacos.RuleNacosConstants;
import com.alibaba.csp.sentinel.dashboard.util.AsyncUtils;
import com.alibaba.csp.sentinel.slots.block.Rule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.util.AssertUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * TODO 新增配置后列表刷新未能展示最新新增的数据，还需要详细调试推送到nacos的流程
 */
@Component
public class SentinelNacosApiClient {

    private static Logger logger = LoggerFactory.getLogger(SentinelNacosApiClient.class);

    private Executor executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    @Autowired
    private RuleNacosClient nacosClient;


    /**
     * 获取微服务流控规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public List<FlowRuleEntity> fetchFlowRuleOfMachine(String app, String ip, int port) {
        List<FlowRule> rules = fetchRules(app, RuleNacosConstants.FLOW_DATA_ID, FlowRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> FlowRuleEntity.fromFlowRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    /**
     * 推送微服务流控规则
     * @param app
     * @param ip
     * @param port
     * @param rules
     * @return
     */
    public CompletableFuture<Void> setFlowRuleOfMachineAsync(String app, String ip, int port, List<FlowRuleEntity> rules) {
        return setRulesAsync(app, RuleNacosConstants.FLOW_DATA_ID, ip, port, rules);
    }


    /**
     * 获取微服务熔断规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public List<DegradeRuleEntity> fetchDegradeRuleOfMachine(String app, String ip, int port) {
        List<DegradeRule> rules = fetchRules(app, RuleNacosConstants.DEGRADE_DATA_ID, DegradeRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> DegradeRuleEntity.fromDegradeRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }


    public boolean setDegradeRuleOfMachine(String app, String ip, int port, List<DegradeRuleEntity> rules) {
        return setRules(app, RuleNacosConstants.DEGRADE_DATA_ID, rules);
    }


    /**
     * 获取微服务热点规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public List<ParamFlowRuleEntity> fetchParamFlowRulesOfMachine(String app, String ip, int port) {
        List<ParamFlowRule> rules = fetchRules(app, RuleNacosConstants.PARAM_DATA_ID, ParamFlowRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> ParamFlowRuleEntity.fromParamFlowRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            // 此处不能返回null，否则前台显示不正确
            return Collections.emptyList();
        }
    }





    /**
     * 推送微服务热点规则
     * @param app
     * @param ip
     * @param port
     * @param rules
     * @return
     */
    public CompletableFuture<Void> setParamFlowRuleOfMachine(String app, String ip, int port, List<ParamFlowRuleEntity> rules) {
        if (rules == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }
        try {
            String data = JSON.toJSONString(
                    rules.stream().map(ParamFlowRuleEntity::getRule).collect(Collectors.toList())
            );

            CompletableFuture<Void> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    nacosClient.publishRules(app, RuleNacosConstants.PARAM_DATA_ID, data);
                    future.complete(null);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        } catch (Exception ex) {
            logger.warn("Error when setting parameter flow rule", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }


    /**
     * 获取微服务系统规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public List<SystemRuleEntity> fetchSystemRuleOfMachine(String app, String ip, int port) {
        List<SystemRule> rules = fetchRules(app, RuleNacosConstants.SYSTEM_DATA_ID, SystemRule.class);
        if (rules != null) {
            return rules.stream().map(rule -> SystemRuleEntity.fromSystemRule(app, ip, port, rule))
                    .collect(Collectors.toList());
        } else {
            return null;
        }
    }

    /**
     * 推送微服务系统规则
     * @param app
     * @param ip
     * @param port
     * @param rules
     * @return
     */
    public boolean setSystemRuleOfMachine(String app, String ip, int port, List<SystemRuleEntity> rules) {
        return setRules(app, RuleNacosConstants.SYSTEM_DATA_ID, rules);
    }


    /**
     * 获取微服务授权规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public List<AuthorityRuleEntity> fetchAuthorityRulesOfMachine(String app, String ip, int port) {
        AssertUtil.notEmpty(app, "Bad app name");
        AssertUtil.notEmpty(ip, "Bad machine IP");
        AssertUtil.isTrue(port > 0, "Bad machine port");
        List<AuthorityRule> rules = fetchRules(app, RuleNacosConstants.AUTHORITY_DATA_ID, AuthorityRule.class);
        return Optional.ofNullable(rules).map(r -> r.stream()
                .map(e -> AuthorityRuleEntity.fromAuthorityRule(app, ip, port, e))
                .collect(Collectors.toList())
        ).orElse(null);
    }

    /**
     * 推送微服务授权规则
     * @param app
     * @param ip
     * @param port
     * @param rules
     * @return
     */
    public boolean setAuthorityRuleOfMachine(String app, String ip, int port, List<AuthorityRuleEntity> rules) {
        return setRules(app, RuleNacosConstants.AUTHORITY_DATA_ID, rules);
    }


    /**
     * 获取网关服务api分组规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public CompletableFuture<List<ApiDefinitionEntity>> fetchApis(String app, String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }

        try {
            CompletableFuture<List<ApiDefinitionEntity>> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    String rules = nacosClient.getRules(app, RuleNacosConstants.GATEWAY_API_DATA_ID);
                    List<ApiDefinitionEntity> entities = JSON.parseArray(rules, ApiDefinitionEntity.class);
                    entities = entities == null ? Collections.emptyList() : entities;
                    for (ApiDefinitionEntity entity : entities) {
                        entity.setApp(app);
                        entity.setIp(ip);
                        entity.setPort(port);
                    }
                    future.complete(entities);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        } catch (Exception ex) {
            logger.warn("Error when fetching gateway apis", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }


    /**
     * 推送网关服务api分组规则
     * @param app
     * @param ip
     * @param port
     * @param apis
     * @return
     */
    public boolean modifyApis(String app, String ip, int port, List<ApiDefinitionEntity> apis) {
        if (apis == null) {
            return true;
        }

        try {
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                    apis.stream().map(r -> r.toApiDefinition()).collect(Collectors.toList()));
            boolean result = nacosClient.publishRules(app, RuleNacosConstants.GATEWAY_API_DATA_ID, data);
            logger.info("Modify gateway apis: {}", result);
            return result;
        } catch (Exception e) {
            logger.warn("Error when modifying gateway apis", e);
            return false;
        }
    }

    /**
     * 获取网关服务流控规则
     * @param app
     * @param ip
     * @param port
     * @return
     */
    public CompletableFuture<List<GatewayFlowRuleEntity>> fetchGatewayFlowRules(String app, String ip, int port) {
        if (StringUtil.isBlank(ip) || port <= 0) {
            return AsyncUtils.newFailedFuture(new IllegalArgumentException("Invalid parameter"));
        }

        try {
            CompletableFuture<List<GatewayFlowRuleEntity>> future = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    String rules = nacosClient.getRules(app, RuleNacosConstants.GATEWAY_FLOW_DATA_ID);
                    List<GatewayFlowRule> gatewayFlowRules = JSON.parseArray(rules, GatewayFlowRule.class);
                    gatewayFlowRules = gatewayFlowRules == null ? Collections.emptyList() : gatewayFlowRules;
                    List<GatewayFlowRuleEntity> entities = gatewayFlowRules.stream().map(rule -> GatewayFlowRuleEntity.fromGatewayFlowRule(app, ip, port, rule)).collect(Collectors.toList());
                    future.complete(entities);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }

            });
            return future;
        } catch (Exception ex) {
            logger.warn("Error when fetching gateway flow rules", ex);
            return AsyncUtils.newFailedFuture(ex);
        }
    }

    /**
     * 推送网关服务流控规则
     * @param app
     * @param ip
     * @param port
     * @param rules
     * @return
     */
    public boolean modifyGatewayFlowRules(String app, String ip, int port, List<GatewayFlowRuleEntity> rules) {
        if (rules == null) {
            return true;
        }

        try {
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                    rules.stream().map(r -> r.toGatewayFlowRule()).collect(Collectors.toList()));
            boolean result = nacosClient.publishRules(app, RuleNacosConstants.GATEWAY_FLOW_DATA_ID, data);
            logger.info("Modify gateway flow rules: {}", result);
            return true;
        } catch (Exception e) {
            logger.warn("Error when modifying gateway apis", e);
            return false;
        }
    }

    private <T extends Rule> List<T> fetchRules(String app, String ruleDataIdSuffix, Class<T> ruleType) {
        return fetchItemsFromNacos(app, ruleDataIdSuffix, ruleType);
    }


    private boolean setRules(String app, String ruleDataIdSuffix, List<? extends RuleEntity> entities) {
        if (entities == null) {
            return true;
        }
        try {
            String data = JSON.toJSONString(
                    entities.stream().map(r -> r.toRule()).collect(Collectors.toList()));
            boolean result = nacosClient.publishRules(app, ruleDataIdSuffix, data);
            logger.info("推送{}{}配置到nacos成功", app, ruleDataIdSuffix);
            return result;
        } catch (Exception e) {
            logger.error("推送{}{}配置到nacos失败", app, ruleDataIdSuffix, e);
            return false;
        }
    }

    private CompletableFuture<Void> setRulesAsync(String app, String ruleDataIdSuffix, String ip, int port, List<? extends RuleEntity> entities) {
        try {
            AssertUtil.notNull(entities, "rules cannot be null");
            AssertUtil.notEmpty(app, "Bad app name");
            AssertUtil.notEmpty(ip, "Bad machine IP");
            AssertUtil.isTrue(port > 0, "Bad machine port");
            String data = JSON.toJSONString(
                    entities.stream().map(r -> r.toRule()).collect(Collectors.toList()));

            CompletableFuture<Void> future = new CompletableFuture<>();
            executor.execute(()-> {
                try {
                    nacosClient.publishRules(app, ruleDataIdSuffix, data);
                    logger.info("推送{}{}配置到nacos成功", app, ruleDataIdSuffix);
                    future.complete(null);
                } catch (Exception e) {
                    logger.error("推送{}{}配置到nacos失败", app, ruleDataIdSuffix, e);
                    future.completeExceptionally(e);
                }
            });
            return future;
        } catch (Exception e) {
            logger.error("推送{}{}配置到nacos失败", app, ruleDataIdSuffix, e);
            return AsyncUtils.newFailedFuture(e);
        }
    }



    /**
     * 从nacos获取配置对象
     * @param app 微服务名称
     * @param ruleDataIdSuffix 规则配置在nacos中的data Id后缀
     * @param ruleType 规则实体类
     * @return
     * @param <T>
     */
    private <T> List<T> fetchItemsFromNacos(String app, String ruleDataIdSuffix, Class<T> ruleType) {
        try {
            String rules = nacosClient.getRules(app, ruleDataIdSuffix);
            return JSON.parseArray(rules, ruleType);
        }  catch (Exception e) {
            logger.error("从nacos获取配置: {}{}失败", app, ruleDataIdSuffix, e);
            return null;
        }
    }


    private <T> CompletableFuture<List<T>> fetchItemsFromNacosAsync(String app, String ruleDataIdSuffix, Class<T> ruleType) {
        CompletableFuture<List<T>> future = new CompletableFuture<List<T>>();
        executor.execute(() -> {

            try {
                String rules = nacosClient.getRules(app, ruleDataIdSuffix);
                future.complete(JSON.parseArray(rules, ruleType));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
