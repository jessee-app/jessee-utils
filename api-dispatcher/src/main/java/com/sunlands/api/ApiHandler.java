package com.sunlands.api;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 拦截处理类
 *
 * @author chengweijie
 */
public class ApiHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiHandler.class);

    private ApiStore apiStore;

    private ApiSign apiSign;

    public ApiHandler(ApiSign apiSign) {
        this.apiSign = apiSign;
    }

    public void initApiStore(ApplicationContext applicationContext) {
        apiStore = new ApiStore(applicationContext);
        apiStore.loadApiFromApplicationContext();
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) {
        String method = request.getParameter("method");
        String bizContent = request.getParameter("bizContent");

        logger.info("接口{}请求参数: bizContent={}", method, bizContent);
        try {
            Assert.hasText(method, "method不可为空");
            // 参数校验&验签
            Assert.isTrue(apiSign.checkParameters(request), "参数校验失败");
            // 检验接口是否存在
            ApiStore.ApiRunnable apiRunnable = apiStore.findApiRunnable(method);
            Assert.notNull(apiRunnable, "接口不存在或接口版本号错误");
            Object[] obj = null;
            Class<?>[] clazzs = apiRunnable.method.getParameterTypes();
            if (clazzs != null && clazzs.length > 0) {
                Object object = JSONObject.parseObject(bizContent, clazzs[0]);
                obj = new Object[]{object};
            }
            Object result = apiRunnable.run(obj);
            logger.info("接口{}调用成功, response={}", method, JSONObject.toJSONString(result));
            returnResult(result, response);
        } catch (Exception e) {
            logger.error("调用异常", e);
            logger.error("调用异常{}参数: bizContent={},e.getMessage={}", method, bizContent, e.getMessage());
            returnResult("接口调用失败:" + e.getMessage(), response);
        }
    }

    private void returnResult(Object result, HttpServletResponse response) {
        String json = JSONObject.toJSONString(result);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html/json;charset=utf-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        try {
            response.getWriter().write(json);
        } catch (IOException e) {
            logger.error("io异常",e);
        }
    }
}
