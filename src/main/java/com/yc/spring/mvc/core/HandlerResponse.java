package com.yc.spring.mvc.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 	 * 处理响应数据
     * date: 2021年1月29日 上午11:05:37  
     * @author 李杭沅 
     * @version  
     * @since JDK 1.8
 */
public class HandlerResponse {
	protected static void handlerStaticResource(HttpServletResponse response, String url) throws IOException{
		File fl = new File(url);
		if(!fl.exists() || !fl.isFile()) {
			send404(response, url);
			return;
		}
		
		try(FileInputStream fis = new FileInputStream(fl)){
			byte[] bt = new byte[fis.available()];
			fis.read(bt);
			sendData(response, bt);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected static void send404(HttpServletResponse response, String url) throws IOException {
		PrintWriter writer = response.getWriter();
		writer.println("<h1>HTTP/1.1 404 File Not Found! - " + url + "</h1>");
		writer.flush();
	}
	
	protected static void sendData(HttpServletResponse response, byte[] bt) throws IOException {
		ServletOutputStream sos = response.getOutputStream();
		sos.write(bt);
		sos.flush();
	}
	
	protected static void sendJson(HttpServletResponse response, Object obj) throws IOException {
		PrintWriter out = response.getWriter();
		Gson gson = new GsonBuilder().serializeNulls().create();
		out.println(gson.toJson(obj));
		out.flush();
	}
}
