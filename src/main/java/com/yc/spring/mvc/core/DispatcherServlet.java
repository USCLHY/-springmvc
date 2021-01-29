package com.yc.spring.mvc.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yc.spring.mvc.core.annotation.ResponseBody;
import com.yc.spring.mvc.core.annotation.StringUtil;
/**
 	 * 请求分发器
     * date: 2021年1月24日 下午9:56:28  
     * @author 李杭沅 
     * @version  
     * @since JDK 1.8
 */
public class DispatcherServlet extends HttpServlet{
	private static final long serialVersionUID = 1417213935205122285L;
	private String contextConfigLocation = "application.properties";
	private FrameworkCore frameworkCore = null;
	/**
	 * 初始化方法
	 * 只会执行一次，在这个方法里面我们需要读取配置文件，解析注解信息
	 *@param
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		String temp = config.getInitParameter("contextConfigLocation");//根据web.xml的init-param获取
		
		if(!StringUtil.checkNull(temp)) {
			contextConfigLocation = temp;
		}
		
		//从指定的包开始扫描，根据我们既定的规则，解析所有注解
		frameworkCore = new FrameworkCore(contextConfigLocation);
	}
	
	/**
	 * 每次前端发请求都会调用的方法
	 *@param
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		//TODO 获取请求地址 /wowo/user/add
		String url = request.getRequestURI();
		System.out.println(url);
		//TODO 获取请求的项目名 /wowo
		String contextPath = request.getContextPath();
		System.out.println(contextPath);
		//TODO 获取请求的资源路径 /user/add
		url = url.replaceFirst(contextPath, "").replaceAll("/+", "/");
		System.out.println(url);
		
		//TODO 判断请求地址中是否含有参数
		if(url.contains("?")) { //说明请求地址中有参数
			url = url.substring(0, url.indexOf("?"));
		}
		//TODO 根据请求路径从handlerMapper中获取处理的方法
		HandlerMapperInfo mapperInfo = frameworkCore.getMapper(url);
		System.out.println(mapperInfo);
		System.out.println(request.getServletContext().getRealPath("") + url.substring(1));
		//TODO 如果获取不到，则说明没有设置这个请求的处理类，则当成静态资源处理
		if(mapperInfo == null) {
			HandlerResponse.handlerStaticResource(response, request.getServletContext().getRealPath("") + url.substring(1));
			return;
		}
		try {
		//TODO 如果有，则需要激活对应的方法来处理
		//TODO 获取处理这个请求的具体方法
		Method method = mapperInfo.getMethod();
		//注意这里默认会在编译的时候将参数名以arg0，arg1的方式替换，所以我们通过反射区获取的时候，没法得到用户配置的形参名
		//解决办法：项目右击 -> Properties -> Java Compiler 下的最后一项 Store Information About method parameter
			
		//TODO 获取这个方法的形参列表，然后从请求中获取对应的形参值，即将这个请求中的参数注入到这个方法的对应形参中
		Object[] args = HandlerRequest.Handle(request, method, response);
		
		//TODO 反向激活这个方法，获取返回值
		Object obj = method.invoke(mapperInfo.getObj(), args);
		//TODO 判断返回值以什么格式返回给前端
		//TODO 判断这个方法上有没有@ResponseBody注解，如果有则以json格式返回这个结果
		if(method.isAnnotationPresent(ResponseBody.class)) {
			HandlerResponse.sendJson(response, obj);
			return;
		}
		//TODO 如果没有，则判断是否以redirect:开头，如果是，则以重定向跳转页面
		String path = String.valueOf(obj);
		if(path.startsWith("redirect:")) {
			path = path.replaceFirst("redirect:", "");
			if(path.startsWith("/")) {
				path = contextPath + path;
			}
			response.sendRedirect(path);
			return;
		}
		//TODO 如果不是，则以内部转发的方法跳转页面
		request.getRequestDispatcher(path).forward(request, response);
		} catch(IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}

	
}
