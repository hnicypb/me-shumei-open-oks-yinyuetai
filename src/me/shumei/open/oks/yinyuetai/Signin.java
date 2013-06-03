package me.shumei.open.oks.yinyuetai;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import android.content.Context;

/**
 * 使签到类继承CommonData，以方便使用一些公共配置信息
 * @author wolforce
 *
 */
public class Signin extends CommonData {
	String resultFlag = "false";
	String resultStr = "未知错误！";
	
	/**
	 * <p><b>程序的签到入口</b></p>
	 * <p>在签到时，此函数会被《一键签到》调用，调用结束后本函数须返回长度为2的一维String数组。程序根据此数组来判断签到是否成功</p>
	 * @param ctx 主程序执行签到的Service的Context，可以用此Context来发送广播
	 * @param isAutoSign 当前程序是否处于定时自动签到状态<br />true代表处于定时自动签到，false代表手动打开软件签到<br />一般在定时自动签到状态时，遇到验证码需要自动跳过
	 * @param cfg “配置”栏内输入的数据
	 * @param user 用户名
	 * @param pwd 解密后的明文密码
	 * @return 长度为2的一维String数组<br />String[0]的取值范围限定为两个："true"和"false"，前者表示签到成功，后者表示签到失败<br />String[1]表示返回的成功或出错信息
	 */
	public String[] start(Context ctx, boolean isAutoSign, String cfg, String user, String pwd) {
		//把主程序的Context传送给验证码操作类，此语句在显示验证码前必须至少调用一次
		CaptchaUtil.context = ctx;
		//标识当前的程序是否处于自动签到状态，只有执行此操作才能在定时自动签到时跳过验证码
		CaptchaUtil.isAutoSign = isAutoSign;
		
		try{
			//存放Cookies的HashMap
			HashMap<String, String> cookies = new HashMap<String, String>();
			//Jsoup的Response
			Response res;
			
			//在线破解JS登录验证串的地址
			String jscrackUrl = "http://oks.shumei.me/jscrack.php?";
			//登录的URL
			String loginUrl = "http://www.yinyuetai.com/login-ajax";
			//签到的URL
			String siginUrl = "http://www.yinyuetai.com/person/sign-in";
			
			//把数据提交到在线破解JS的服务器，这个是需要提交账号密码到作者服务器的
			//信不过的可以自己架设Node.js服务器或想其他办法破解登录验证串
			//具体信息可参考作者博文：http://shumei.me/exp/node-js-crack-the-javascript-login-encryption-of-yinyuetai.html
			String data = "yinyuetai|@@|login_time_crack|@@|{\"logindata\":\"" + user + pwd + "\"}";
			jscrackUrl += "data=" + it.sauronsoftware.base64.Base64.encode(data);
			res = Jsoup.connect(jscrackUrl).userAgent(UA_ANDROID).timeout(TIME_OUT).ignoreContentType(true).method(Method.GET).execute();
			System.out.println(jscrackUrl);
			System.out.println(res.body());
			JSONObject jsonObj = new JSONObject(res.body());
			String _t = jsonObj.getString("_t");
			String _p1 = jsonObj.getString("_p1");
			String _p2 = jsonObj.getString("_p2");
			
			//登录音悦台
			res = Jsoup.connect(loginUrl)
					.data("email", user)
					.data("password", pwd)
					.data("_t", _t)
					.data("_p1", _p1)
					.data("_p2", _p2)
					.referrer(loginUrl).userAgent(UA_CHROME).timeout(TIME_OUT).ignoreContentType(true).method(Method.POST).execute();
			//保存Cookies
			cookies.putAll(res.cookies());
						
			try {
				//以登录返回的JSON字符串创建JSONObject
				jsonObj = new JSONObject(res.body());
				boolean logined = jsonObj.getBoolean("logined");
				if(logined)
				{
					res = Jsoup.connect(siginUrl).userAgent(UA_CHROME).cookies(cookies).timeout(TIME_OUT).referrer(loginUrl).method(Method.GET).ignoreContentType(true).execute();
					//{"message":"您今天已经打过卡了","error":true,"logined":true}
					//{"message":"打卡成功并给您增加20个积分","error":false,"logined":true}
					jsonObj = new JSONObject(res.body());
					String message = jsonObj.getString("message");
					
					resultFlag = "true";
					resultStr = message;
				}
				else
				{
					resultFlag = "false";
					resultStr = "用户名或密码错误";
				}
			} catch (JSONException e) {
				resultFlag = "false";
				resultStr = "提交登录时服务器返加错误信息，登录失败";
				e.printStackTrace();
			}
			
		} catch (IOException e) {
			this.resultFlag = "false";
			this.resultStr = "连接超时";
			e.printStackTrace();
		} catch (Exception e) {
			this.resultFlag = "false";
			this.resultStr = "未知错误！";
			e.printStackTrace();
		}
		
		return new String[]{resultFlag, resultStr};
	}
	
	
}
