package common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 汎用メソッド集
 *  */
public class CommonUtil {

	/** ロガー */
	private static Logger log = LoggerFactory.getLogger("dailyRotation");


	/**
	 * MapオブジェクトからJSON文字列に変換する
	 * @param map 変換対象
	 * @return string 変換後
	 */
	public static String toJSON(Map<String, String> map) {

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			String jsonString = objectMapper.writeValueAsString(map);
			log.info(jsonString);
			return jsonString;
		} catch (JsonProcessingException e) {
			log.error(e.toString());
			log.info("Json変換に失敗" + e);
			return null;
		}
	}

	/**
	 * テキストファイルを出力
	 * @param fileName 出力するファイル名
	 * @param txt ファイルの本文
	 *  */
	public static void makeReport(String fileName, String txt) {

		String dir = System.getProperty("user.dir");
		File file = new File(dir, fileName);
		if(file.exists() == false) {
			try{
				if(file.createNewFile() == true) {
					FileWriter filewriter = new FileWriter(file);
					filewriter.write(txt);
					filewriter.close();
				} else {
					log.info("ファイル作成に失敗");
				}
			}catch(IOException e){
				log.error(e.toString());
				log.info("ファイル出力に失敗" + e);
			}
		} else {
			log.info("ファイルが既に存在");
		}
	}
}
