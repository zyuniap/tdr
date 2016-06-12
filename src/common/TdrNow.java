package common;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TDR待ち時間を取得する
 *  */
public class TdrNow {

	/** ユーザーエージェント */
	public static final String userAgents = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 "
			+ "(KHTML, like Gecko) Chrome/52.0.2734.0 Safari/537.36";

	/** ロガー */
	private static Logger log = LoggerFactory.getLogger("dailyRotation");


	public static void main(String[] args) {

		Map<String, String> cookies = inTdr();

		if(cookies != null) {
			atrStatus(cookies);
		}
	}


	/**
	 * GPS情報を偽装することで、TDRエリア内からのみ獲得可能なcookieを取得する
	 * @return Map<String, String> TDRのcookie
	 *  */
	private static Map<String, String> inTdr() {

		CookieManager man = new CookieManager();
		CookieHandler.setDefault(man);
		try {
			String gpsCheckUrl = "http://info.tokyodisneyresort.jp/rt/s/gps/tds_index.html?nextUrl="
					+ "http://info.tokyodisneyresort.jp/rt/s/realtime/tds_attraction.html";
			/* GPS情報を偽装 */
			String gpsParam = "&lat=35.6323121345449&lng=139.88004142065444";

			URL url = new URL(gpsCheckUrl + gpsParam);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.getResponseCode();
		} catch (IOException e) {
			log.error(e.toString());
			log.info("GPSアクセスに失敗" + e);
			return null;
		}

		/* TDR内cookieを保持 */
		CookieStore store = man.getCookieStore();
		List<HttpCookie> cookies = store.getCookies();
		if(cookies.size() > 0) {
			HttpCookie cookie = cookies.get(0);
			if(StringUtils.isEmpty(cookie.getName()) == false) {
				Map<String, String> ret = new HashMap<String, String>();
				ret.put(cookie.getName(), cookie.getValue());
				return ret;
			}
		}
		log.info("cookie取得に失敗");
		return null;
	}


	/**
	 * TDSとTDLのURLから共通処理を呼び出す
	 * @param cookies TDRのcookie
	 *  */
	private static void atrStatus(Map<String, String> cookies) {

		/* ファイル名の重複防止 */
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
		String strDate = sdf.format(cal.getTime());

		String seaAtr  = "tds_attraction.html";
		waitTimeReport(seaAtr,  cookies, strDate + "TDS.txt");

		String landAtr = "tdl_attraction.html";
		waitTimeReport(landAtr, cookies, strDate + "TDL.txt");
	}


	/**
	 * 待ち時間のレポートを出力する
	 * @param atrUrl 対象URL
	 * @param cookies TDRのcookie
	 * @param file ファイル名
	 *  */
	private static void waitTimeReport(String atrUrl, Map<String, String> cookies, String file) {
		Map<String, String> atrWait = waitTime(atrUrl, cookies);
		if(atrWait != null) {
			String json = CommonUtil.toJSON(atrWait);
			if(StringUtils.isEmpty(json) == false) {
				CommonUtil.makeReport(file, json);
			}
		}
	}


	/**
	 * 待ち時間HPを解析して、名前と待ち時間の対応表を作成する
	 * @param targetUrl 対象URL
	 * @param cookies TDRのcookie
	 * @return Map<String, String> 対応表
	 *  */
	private static Map<String, String> waitTime(String targetUrl, Map<String, String> cookies) {

		Document doc;
		try {
			String realtimeBaseUrl = "http://info.tokyodisneyresort.jp/rt/s/realtime/";

			/* 待ち時間HPにアクセス */
			doc = Jsoup.connect(realtimeBaseUrl + targetUrl).userAgent(userAgents).cookies(cookies).timeout(5000).get();
		} catch (IOException e) {
			log.error(e.toString());
			log.info(targetUrl + "にアクセス失敗" + e);
			return null;
		}

		/* HP解析で待ち時間を取得 */
		Map<String, String> nameTime = new HashMap<String, String>();
		Elements lis = doc.select(".midArw");
		for(Element li: lis) {
			Elements h3 = li.select("h3");
			String name = h3.text();
			if(StringUtils.isEmpty(name) == false) {
				Elements strong = li.select("strong");
				String waitTime = strong.text();
				if(StringUtils.isEmpty(waitTime) == true) waitTime = "none";
				nameTime.put(name, waitTime);
				log.debug(name + "\n" + waitTime);
			}
		}
		return nameTime;
	}

}
