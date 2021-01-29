package com.yc.spring.mvc.core;
/**
 	 * 请求映射对象
     * date: 2021年1月24日 下午9:51:14  
     * @author 李杭沅 
     * @version  
     * @since JDK 1.8
 */

import java.lang.reflect.Method;
import java.util.Arrays;

public class HandlerMapperInfo {
	private Method method; //处理这个请求的方法
	private Object obj; //这个方法所属的对象method.invoke(obj, args)
	private Object[] args; //这个方法需要的形参列表
	@Override
	public String toString() {
		return "HandlerMapperInfo [method=" + method + ", obj=" + obj + ", args=" + Arrays.toString(args) + "]";
	}
	public Method getMethod() {
		return method;
	}
	public void setMethod(Method method) {
		this.method = method;
	}
	public Object getObj() {
		return obj;
	}
	public void setObj(Object obj) {
		this.obj = obj;
	}
	public Object[] getArgs() {
		return args;
	}
	public void setArgs(Object[] args) {
		this.args = args;
	}
	public HandlerMapperInfo(Method method, Object obj) {
		super();
		this.method = method;
		this.obj = obj;
	}
	public HandlerMapperInfo() {
		super();
	}
	
	
}
