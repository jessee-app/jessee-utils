package com.sunlands.api;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 容器
 * @author chengweijie
 */
public class ApiStore {
    private ApplicationContext applicationContext;
    private Map<String, ApiRunnable> apiMap = new ConcurrentHashMap<>();

    public ApiStore(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void loadApiFromApplicationContext() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        for (String beanName : beanNames) {
            Class<?> clazz = applicationContext.getType(beanName);
            for (Method method : clazz.getDeclaredMethods()) {
                ApiMapping apiMapping = AnnotationUtils.findAnnotation(method,ApiMapping.class);
                if (apiMapping != null) {
                    addApiItem(apiMapping, beanName, method);
                }
            }
        }
    }

    private void addApiItem(ApiMapping apiMapping, String name, Method method) {
        ApiRunnable apiRunnable = new ApiRunnable();
        apiRunnable.apiMapping = apiMapping;
        apiRunnable.apiName = apiMapping.value();
        apiRunnable.beanName = name;
        apiRunnable.method = method;
        apiMap.put(apiMapping.value(), apiRunnable);
    }

    public ApiRunnable findApiRunnable(String apiName) {
        return apiMap.get(apiName);
    }

    public class ApiRunnable {
        String apiName;
        String beanName;
        Object bean;
        Method method;
        ApiMapping apiMapping;

        public Object run(Object... args) throws InvocationTargetException, IllegalAccessException {
            if (bean == null) {
                bean = applicationContext.getBean(beanName);
            }
            return method.invoke(bean, args);
        }
    }
}
