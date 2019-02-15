package me.zohar.lottery.issue.service;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import org.dom4j.DocumentHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.http.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import me.zohar.lottery.constants.GameCode;
import me.zohar.lottery.issue.vo.IssueVO;

@Service
@Slf4j
public class CqsscService {

	@Autowired
	private IssueService issueService;

	/**
	 * 同步当前时间的开奖号码
	 */
	public void syncLotteryNum() {
		IssueVO latestWithInterface = getLatestLotteryResultWitOpenCai();
		if (latestWithInterface == null) {
			return;
		}
		issueService.syncLotteryNum(GameCode.重庆时时彩, latestWithInterface.getIssueNum(),
				latestWithInterface.getLotteryNum());
	}

	/**
	 * 通过新浪爱彩网获取重庆时时彩最新开奖期号
	 * 
	 * @return
	 */
	public IssueVO getLatestLotteryIssueWithAiCai() {
		try {
			String url = "https://kaijiang.aicai.com/cqssc/";
			Document document = Jsoup.connect(url).get();
			Element element = document.getElementById("jq_body_kc_result").child(0);
			String issueText = element.child(0).text();
			long issueNum = Long.parseLong(issueText.substring(0, issueText.length() - 1).replace("-", ""));
			String lotteryNum = element.child(2).text().replace("|", ",");
			IssueVO lotteryResult = IssueVO.builder().issueNum(issueNum).lotteryDate(null).lotteryNum(lotteryNum)
					.build();
			return lotteryResult;
		} catch (Exception e) {
			log.error("通过新浪爱彩网获取重庆时时彩最新开奖结果发生异常", e);
		}
		return null;
	}

	/**
	 * 通过500彩票网获取重庆时时彩最新开奖结果
	 * 
	 * @return
	 */
	public IssueVO getLatestLotteryResultWith500() {
		String currentDateFormat = DateUtil.format(new Date(), DatePattern.PURE_DATE_PATTERN);
		try {
			String url = "https://kaijiang.500.com/static/public/ssc/xml/qihaoxml/{0}.xml";
			url = MessageFormat.format(url, currentDateFormat);
			String result = HttpUtil.get(url);
			org.dom4j.Document document = DocumentHelper.parseText(result);
			List<org.dom4j.Element> elements = document.getRootElement().elements("row");
			org.dom4j.Element element = elements.get(0);
			long issueNum = Long.parseLong(element.attributeValue("expect"));
			String lotteryDateFormat = DateUtil.format(
					DateUtil.parse(String.valueOf(issueNum).substring(0, 8), DatePattern.PURE_DATE_PATTERN),
					DatePattern.NORM_DATE_PATTERN);
			String lotteryNum = element.attributeValue("opencode");
			IssueVO lotteryResult = IssueVO.builder().issueNum(issueNum).lotteryDate(lotteryDateFormat)
					.lotteryNum(lotteryNum).build();
			return lotteryResult;
		} catch (Exception e) {
			log.error("通过500彩票网获取重庆时时彩最新开奖结果发生异常", e);
		}
		return null;
	}

	/**
	 * 通过开彩票接口获取重庆时时彩最新开奖结果
	 */
	public IssueVO getLatestLotteryResultWitOpenCai() {
		try {
			String result = HttpUtil.get("http://f.apiplus.net/cqssc.json");
			log.info("接口返回结果:{}", result);
			JSONObject resultJsonObject = JSON.parseObject(result);
			JSONArray jsonArray = resultJsonObject.getJSONArray("data");
			JSONObject jsonObject = jsonArray.getJSONObject(0);
			long issueNum = Long.parseLong(jsonObject.getString("expect"));
			String lotteryDateFormat = DateUtil.format(
					DateUtil.parse(String.valueOf(issueNum).substring(0, 8), DatePattern.PURE_DATE_PATTERN),
					DatePattern.NORM_DATE_PATTERN);

			String lotteryNum = jsonObject.getString("opencode");
			IssueVO lotteryResult = IssueVO.builder().issueNum(issueNum).lotteryDate(lotteryDateFormat)
					.lotteryNum(lotteryNum).build();
			return lotteryResult;
		} catch (Exception e) {
			log.error("通过开彩票获取重庆时时彩最新开奖结果发生异常", e);
		}
		return null;
	}

}