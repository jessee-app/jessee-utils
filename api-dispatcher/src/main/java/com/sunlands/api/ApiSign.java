package com.sunlands.api;

import javax.servlet.http.HttpServletRequest;

/**
 * 验签接口
 * @author chengweijie
 */
public interface ApiSign {

    /**
     * 参数校验
     */
    public boolean checkParameters(HttpServletRequest request);
}
