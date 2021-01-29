package com.yc.spring.mvc.core;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yc.spring.mvc.core.annotation.StringUtil;



	@WebFilter(value="/*", filterName="CharacterEncodingFilter", initParams= {@WebInitParam(name="encoding", value="utf-8")})
	public class CharacterEncodingFilter implements Filter{
		private String encoding = "utf-8";
		
		public void init(FilterConfig config) {
			String temp = config.getInitParameter("encoding");
			if(StringUtil.checkNull(temp)) {
				return;
			}
			encoding = temp;
		}

		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			HttpServletRequest req = (HttpServletRequest) request;
			HttpServletResponse resp = (HttpServletResponse) response;
			
			req.setCharacterEncoding(encoding);
			resp.setCharacterEncoding(encoding);
			
			//将这个请求和响应往下传递
			chain.doFilter(req, resp);
		}
	}

