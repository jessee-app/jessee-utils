#插件工作流程
1.初始化servlet时，遍历spring容器，将标注了自定义注解的方法加入api容器 2.有外部请求时，根据参数中的method，从api容器中找到对应的方法，通过反射执行业务逻辑



#第一步引入jar包
```
<dependency>
    <groupId>com.sunlands.plugin</groupId>
    <artifactId>api-dispatcher</artifactId>
    <version>1.0.4</version>
</dependency>
```
#第二步：实现ApiSign接口
实现checkParameters方法，自定义参数校验和验签
````
/**
 * 自定义api签名实现类，用于验签{@link com.sunlands.api.ApiHandler#handle(HttpServletRequest, HttpServletResponse)}
 *
 * @author chengweijie
 */
@Component
public class MyApiSign implements ApiSign {

    private static final Logger logger = LoggerFactory.getLogger(MyApiSign.class);

    @Autowired
    private MerchantConfigHolder merchantConfigHolder;

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_CHANNEL_CODE = "merCode";
    private static final String KEY_METHOD = "method";
    private static final String KEY_SIGN_TYPE = "signType";
    private static final String KEY_SIGN = "sign";
    private static final String KEY_VERSION = "version";
    private static final String KEY_BIZ_CONTENT = "bizContent";

    @Override
    public boolean checkParameters(HttpServletRequest request) {
        String timestamp = request.getParameter("timestamp");
        String merCode = request.getParameter("merCode");
        String signType = request.getParameter("signType");
        String sign = request.getParameter("sign");
        String version = request.getParameter("version");
        String method = request.getParameter("method");
        String bizContent = request.getParameter("bizContent");

        // 根据渠道获取对应的秘钥
        String secretKey = merchantConfigHolder.getSecretKey(merCode);
        Map<String, String> param = new HashMap<>(16);
        param.put(KEY_TIMESTAMP, timestamp);
        param.put(KEY_CHANNEL_CODE, merCode);
        param.put(KEY_METHOD, method);
        param.put(KEY_SIGN_TYPE, signType);
        param.put(KEY_SIGN, sign);
        param.put(KEY_VERSION, version);
        param.put(KEY_BIZ_CONTENT, bizContent);
        String newSignature = SignUtils.signByMd5(param, secretKey, SignUtils.CHARSET_UTF8);
        logger.info("signature is {}", newSignature);
        return sign.equals(newSignature);
    }
}
````

#第三步：注册入口bean
```
/**
 * 拦截器配置，拦截到请求后，根据method转发到对应的处理类{@link com.sunlands.api.ApiHandler#handle(HttpServletRequest, HttpServletResponse)}
 *
 * @author chengweijie
 */
@Configuration
public class ApiHandlerConfig {

	/**
     * 实现ApiSign接口
     */
    @Autowired
    private MyApiSign myApiSign;

    /**
     * 配置servlet
     *
     * @return
     */
    @Bean
    public ServletRegistrationBean servlet() {
        ApiGatewayServlet apiGatewayServlet = new ApiGatewayServlet(myApiSign);
        // 自定义验签规则
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(apiGatewayServlet);
        // 拦截路径
        registrationBean.addUrlMappings("/rest/execute");
        // 应用启动时就加载并初始化这个servlet
        registrationBean.setLoadOnStartup(0);
        return registrationBean;
    }
}
```
___
#备注
POST请求，x-www-form-urlencoded格式
请求参数需要method和bizContent
method对应具体实现类
bizContent对应具体实现类入参


