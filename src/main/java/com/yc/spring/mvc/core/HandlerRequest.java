package com.yc.spring.mvc.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.yc.spring.mvc.core.annotation.RequestParam;
import com.yc.spring.mvc.core.annotation.StringUtil;

/**
 	 * 处理请求中的参数
     * @author 李杭沅 
     * @version  
     * @since JDK 1.8
 */
public class HandlerRequest {
	protected static Object[] Handle(HttpServletRequest request, Method method, HttpServletResponse response) throws InstantiationException, IllegalAccessException {
		//数组的长度取决于这个方法的形参个数
		int count = method.getParameterCount();
		
		//激活这个方法，这个方法要几个形参，我们就给几个
		Object[] args = new Object[count];
		
		//获取这个方法的参数
		Parameter[] params = method.getParameters();
		
		String paramName = null; //形参名
		Class<?> cls = null; //形参类型
		RequestParam requestParam = null;
		String value = null;
		int index = 0;
		Map<String, String[]> paramValues = null;
		Map<String, Object> paramMap = null;
		Field[] fields = null;
		String attrName = null;
		Class<?> attrType = null;
		Object obj = null;
		
		//循环获取这些参数信息
		for(Parameter param : params) {
			paramName = param.getName(); //获取形参名称
			cls = param.getType();
			
			//判断这个形参上有没有@RequestParam注解，如果有则说明等一下我们要根据配置在@RequestParam注解中的value值来从请求中获取对应的属性值，而不是通过paramName
			requestParam = param.getAnnotation(RequestParam.class);
			if(requestParam != null) {
				paramName = requestParam.value();
			}
			
			value = request.getParameter(paramName);
			
			//将这个字符强制转换成方法需要的类型
			if(cls == Integer.TYPE) {
				args[index] = Integer.parseInt(value);
			} else if(cls == Integer.class) {
				args[index] = Integer.valueOf(value);
			} else if(cls == Float.TYPE) {
				args[index] = Float.parseFloat(value);
			} else if(cls == Float.class) {
				args[index] = Float.valueOf(value);
			} else if(cls == Double.TYPE) {
				args[index] = Double.parseDouble(value);
			} else if(cls == Double.class) {
				args[index] = Double.valueOf(value);
			} else if(cls == String.class) {
				args[index] = value;
			} else if(cls == Map.class) {
				paramValues = request.getParameterMap();
				paramMap = new HashMap<String, Object>();
				for(Entry<String, String[]> entry : paramValues.entrySet()) {
					paramMap.put(entry.getKey(), entry.getValue()[0]);
				}
			} else if(cls == ServletRequest.class || cls == HttpServletRequest.class) {
				args[index] = request;
			} else if(cls == ServletResponse.class || cls == HttpServletResponse.class) {
				args[index] = response;
			} else if(cls == HttpSession.class) {
				args[index] = request.getSession();
			} else if(cls == ServletContext.class) {
				args[index] = request.getServletContext();
			} else { //当成对象处理
				//获取这个对象的属性
				fields = cls.getDeclaredFields();
				obj = cls.newInstance(); //实例化一个这个类的对象
				
				for(Field fd : fields) {
					fd.setAccessible(true);//暴力反射
					attrName = fd.getName();
					attrType = fd.getType();
					value = request.getParameter(attrName); //根据属性名从请求中取出属性值
					if(StringUtil.checkNull(value)) { //没有给值
						continue;
					}
					
					if(attrType == Integer.TYPE) {
						fd.set(obj, Integer.parseInt(value));
					} else if(attrType == Integer.class) {
						fd.set(obj, Integer.valueOf(value));
					} else if(attrType == Double.TYPE) {
						fd.set(obj, Double.parseDouble(value));
					} else if(attrType == Double.class) {
						fd.set(obj, Double.valueOf(value));
					} else if(attrType == Float.TYPE) {
						fd.set(obj, Float.parseFloat(value));
					} else if(attrType == Float.class) {
						fd.set(obj, Float.valueOf(value));
					} else {
						fd.set(obj, value);
					}
				}
				args[index] = obj;
			}
			++index;
		}
		return args;
	}
}
