package com.yc.spring.mvc.core;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import com.yc.spring.mvc.core.annotation.Autowired;
import com.yc.spring.mvc.core.annotation.Component;
import com.yc.spring.mvc.core.annotation.Controller;
import com.yc.spring.mvc.core.annotation.RequestMapping;
import com.yc.spring.mvc.core.annotation.StringUtil;

/**
 	 * 核心代码
 	 * 1、读取配置文件-> 获取要扫描的基址路径
 	 * 2、扫描包，获取类路径
 	 * 3、初始化需要IOC容器管理的类，并交给IOC容器管理 @Component
 	 * 4、执行依赖注入，即完成@Autowired注解的解析
 	 * 5、构建HandlerMapping，完成URL与对应方法之间的关联映射 @Controller @RequestMapping
     * @author 李杭沅 
     * @version  
     * @since JDK 1.8
 */
public class FrameworkCore {
	private String contextConfigLocation; //配置文件路径
	private String basePackage; //基址路径
	private Set<String> classNames = new HashSet<String>(); //扫描获取到的类路径信息
	private Map<String, Object> instanceObject = new HashMap<String, Object>(); //用来存放需要IoC容器管理实例化好的类对象
	// url请求地址对应的处理对象
	private Map<String, HandlerMapperInfo> handlerMapper = new HashMap<String, HandlerMapperInfo>();
	
	public FrameworkCore(String contextConfigLocation) {
		this.contextConfigLocation = contextConfigLocation;
		init(); //初始化
	}

	private void init() {
		// TODO 1、读取配置文件 -> 获取要扫描的基址路径
		doLoadConfig();
		
		// TODO 2、扫描包，获取类路径
		doScannerPackage();
		
		// TODO 3、初始化需要IOC容器管理的类，并交给IOC容器管理 @Component
		doInstanceObject();
		
		// TODO 4、执行依赖注入，即完成@Autowired注解的解析
		doAutowired();
		
		// TODO 5、构建HandlerMapping，完成URL与对应方法之间的关联映射 @Controller @RequestMapping
		initHandlerMapping();
	}
	/**
	 * 实现请求与方法之间的关联映射
	 * @author 李杭沅
	 * 2021年1月25日
	 */
	private void initHandlerMapping() {
		if(instanceObject.isEmpty()) {
			return;
		}
		
		Method[] methods = null;
		Class<?> cls = null;
		RequestMapping requestMapper = null;
		String baseUrl = "";
		String url = null;
		
		for(Entry<String, Object> entry : instanceObject.entrySet()) {
			cls = entry.getValue().getClass();
			
			if(!cls.isAnnotationPresent(Controller.class)) {
				continue;
			}
			requestMapper = cls.getAnnotation(RequestMapping.class);
			if(requestMapper != null) {
				baseUrl = requestMapper.value(); //获取配置在控制器类上的映射路径
				
				if(baseUrl.startsWith("/")) {
					baseUrl = "/" + baseUrl;
				}
			}
			
			methods = cls.getDeclaredMethods();
			if(methods == null || methods.length <= 0) {
				continue;
			}
			
			for(Method method : methods) {
				if(!method.isAnnotationPresent(RequestMapping.class)) {
					continue;
				}
				
				requestMapper = method.getAnnotation(RequestMapping.class);
				url = requestMapper.value();
				
				if(!url.startsWith("/")) {
					url = "/" + url;
				}
				url = baseUrl + url;
				handlerMapper.put(url.replaceAll("/+", "/"), new HandlerMapperInfo(method, entry.getValue()));
			}
		}
	}
	/**
	 * 实现依赖注入
	 * @author 李杭沅
	 * 2021年1月25日
	 */
	private void doAutowired() {
		if(instanceObject.isEmpty()) {
			return;
		}
		Field[] fields = null;
		Class<?> cls = null;
		Autowired awd = null;
		String beanName = null;
		
		for(Entry<String, Object> entry : instanceObject.entrySet()) {
			cls = entry.getValue().getClass();
			
			fields = cls.getDeclaredFields();
			if(fields == null || fields.length <= 0) {
				continue;
			}
			
			//循环所有属性，判断有没有@Autowired注解
			for(Field fd : fields) {
				if(!fd.isAnnotationPresent(Autowired.class)) {
					continue;
				}
				
				awd = fd.getAnnotation(Autowired.class);
				beanName = awd.value().trim();//获取配置的名字
				
				fd.setAccessible(true);
				
				if(StringUtil.checkNull(beanName)) { //用户没有指定对应的名字，那么就根据类名来注值
					beanName = fd.getType().getSimpleName();
					
					try {
						fd.set(entry.getValue(), instanceObject.get(beanName));
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
				}else { //如果指定了名字
					if(!instanceObject.containsKey(beanName)) {
						throw new RuntimeException(cls.getName() + "." + fd.getName() + " 注值失败，没有对应的实体类 " + beanName);
					}
					
					try {
						//第一个参数是这个属性是哪个类的，第二个参数是这个属性的值
						fd.set(entry.getValue(), instanceObject.get(beanName));
					} catch (IllegalArgumentException | IllegalAccessException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	/**
	 * 实例化需要IOC容器管理的对象
	 * @author 李杭沅
	 * 2021年1月24日
	 */
	private void doInstanceObject() {
		if(classNames.isEmpty()) {
			return;
		}
		Class<?> cls = null;
		Object instance = null;
		String beanName = null;
		Class<?>[] interfaces = null;
		String temp = null;
		
		for(String className : classNames) {
			try {
				cls = Class.forName(className);
				beanName = this.toFirstLowerCase(cls.getSimpleName());
			
				//判断有没有@Controller注解
				if(cls.isAnnotationPresent(Controller.class)) {
					temp = cls.getAnnotation(Controller.class).value();
					if(!StringUtil.checkNull(temp)) {
						beanName = temp;
					}
					instanceObject.put(beanName, cls.newInstance()); //实例化这个控制层对象
				} else if(cls.isAnnotationPresent(Component.class)) { //有没有Component注解
					temp = cls.getAnnotation(Component.class).value();
					if(!StringUtil.checkNull(temp)) {
						beanName = temp;
					}
					
					instance = cls.newInstance();
					instanceObject.put(beanName, instance);
					
					//这个类有实现其他接口吗
					interfaces = cls.getInterfaces();
					if(interfaces == null || interfaces.length <= 0) {
						continue;
					}
					
					for(Class<?> its : interfaces) {
						instanceObject.put(its.getSimpleName(), instance);
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private String toFirstLowerCase(String name) {
		char[] chs = name.toCharArray();
		chs[0] += 32;
		return String.valueOf(chs);
	}
	/**
	 * 扫描包，读取类路径
	 * @author 李杭沅
	 * 2021年1月29日
	 */
	private void doScannerPackage() {
		if(StringUtil.checkNull(basePackage)) {
			throw new RuntimeException("您扫描的基址路径不存在！");
		}
		URL url = this.getClass().getClassLoader().getResource(basePackage.replaceAll("\\.", "/"));//把所有.替换为/
		//获取指定路径下所有文件和子目录
		File dist = new File(url.getFile());
		
		getClassInfo(basePackage, dist);
	}
	/**
	 * 获取指定目录下的子文件和目录
	 * @author 李杭沅
	 * 2021年1月24日
	 * @param basePackage2
	 * @param dist
	 */
	private void getClassInfo(String basePackage, File dist) {
		if(dist.exists() && dist.isDirectory()) {
			for(File fl : dist.listFiles()) {
				if(fl.isDirectory()) {
					getClassInfo(basePackage + "." + fl.getName(), fl);
				}else {
					classNames.add(basePackage + "." + fl.getName().replace(".class", ""));
				}
			}
		}
	}
	/**
	 * 读取解析配置文件
	 * @author 李杭沅
	 * 2021年1月29日
	 */
	private void doLoadConfig() {
		try(InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation)){
			Properties pro = new Properties();
			pro.load(is);
			basePackage = pro.getProperty("basePackage").trim();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getContextConfigLocation() {
		return contextConfigLocation;
	}

	public String getBasePackage() {
		return basePackage;
	}

	public Set<String> getClassNames() {
		return classNames;
	}

	public Map<String, Object> getInstanceObject() {
		return instanceObject;
	}

	public Map<String, HandlerMapperInfo> getHandlerMapper() {
		return handlerMapper;
	}
	
	public HandlerMapperInfo getMapper(String url) {
		return handlerMapper.get(url);
	}
}
