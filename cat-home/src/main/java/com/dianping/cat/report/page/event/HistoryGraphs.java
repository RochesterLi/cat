package com.dianping.cat.report.page.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.unidal.dal.jdbc.DalNotFoundException;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.util.StringUtils;

import com.dianping.cat.Cat;
import com.dianping.cat.consumer.event.EventAnalyzer;
import com.dianping.cat.core.dal.DailyGraph;
import com.dianping.cat.core.dal.DailyGraphDao;
import com.dianping.cat.core.dal.DailyGraphEntity;
import com.dianping.cat.core.dal.Graph;
import com.dianping.cat.core.dal.GraphDao;
import com.dianping.cat.core.dal.GraphEntity;
import com.dianping.cat.helper.TimeUtil;
import com.dianping.cat.report.page.BaseHistoryGraphs;
import com.dianping.cat.report.page.LineChart;
import com.dianping.cat.report.page.event.Handler.DetailOrder;
import com.dianping.cat.report.page.event.Handler.SummaryOrder;

public class HistoryGraphs extends BaseHistoryGraphs {

	@Inject
	private GraphDao m_graphDao;

	@Inject
	private DailyGraphDao m_dailyGraphDao;

	private void appendArray(double[] src, int index, String str, int size) {
		String[] values = str.split(",");
		if (values.length < size) {
			for (int i = 0; i < size; i++) {
				src[index + i] = Double.valueOf(str);
			}
		} else {
			for (int i = 0; i < size; i++) {
				src[index + i] = Double.valueOf(values[i]);
			}
		}
	}

	private LineChart buildFail(List<Map<String, double[]>> datas, Date start, int size, long step, String name,
	      String queryType) {
		LineChart item = new LineChart();

		item.setStart(start);
		item.setSize(size);
		item.setStep(step);
		item.setTitle(name + " Error (count)");

		for (Map<String, double[]> data : datas) {
			item.addValue(data.get("failure_count"));
		}
		item.setSubTitles(buildSubTitle(start, size, step, queryType));
		return item;
	}

	public Map<String, double[]> buildGraphDatasForDaily(Date start, Date end, String type, String name,
	      List<DailyGraph> graphs) {
		Map<String, double[]> result = new HashMap<String, double[]>();
		int size = (int) ((end.getTime() - start.getTime()) / TimeUtil.ONE_DAY);
		double[] totalCount = new double[size];
		double[] failureCount = new double[size];

		if (!StringUtils.isEmpty(type) && StringUtils.isEmpty(name)) {
			for (DailyGraph graph : graphs) {
				int indexOfperiod = (int) ((graph.getPeriod().getTime() - start.getTime()) / TimeUtil.ONE_DAY);
				String summaryContent = graph.getSummaryContent();
				String[] allLines = summaryContent.split("\n");
				for (int j = 0; j < allLines.length; j++) {
					String[] records = allLines[j].split("\t");
					if (records[SummaryOrder.TYPE.ordinal()].equals(type)) {
						totalCount[indexOfperiod] = Double.valueOf(records[SummaryOrder.TOTAL_COUNT.ordinal()]);
						failureCount[indexOfperiod] = Double.valueOf(records[SummaryOrder.FAILURE_COUNT.ordinal()]);
					}
				}
			}
		} else if (!StringUtils.isEmpty(type) && !StringUtils.isEmpty(name)) {
			for (DailyGraph graph : graphs) {
				int indexOfperiod = (int) ((graph.getPeriod().getTime() - start.getTime()) / TimeUtil.ONE_DAY);
				String detailContent = graph.getDetailContent();
				String[] allLines = detailContent.split("\n");
				for (int j = 0; j < allLines.length; j++) {
					String[] records = allLines[j].split("\t");
					if (records[DetailOrder.TYPE.ordinal()].equals(type) && records[DetailOrder.NAME.ordinal()].equals(name)) {
						totalCount[indexOfperiod] = Double.valueOf(records[DetailOrder.TOTAL_COUNT.ordinal()]);
						failureCount[indexOfperiod] = Double.valueOf(records[DetailOrder.FAILURE_COUNT.ordinal()]);
					}
				}
			}
		}

		result.put("total_count", totalCount);
		result.put("failure_count", failureCount);
		return result;
	}

	public Map<String, double[]> buildGraphDatasForHour(Date start, Date end, String type, String name,
	      List<Graph> graphs) {
		Map<String, double[]> result = new HashMap<String, double[]>();
		int size = (int) ((end.getTime() - start.getTime()) / TimeUtil.ONE_HOUR * 12);
		double[] total_count = new double[size];
		double[] failure_count = new double[size];

		if (!StringUtils.isEmpty(type) && StringUtils.isEmpty(name)) {
			for (Graph graph : graphs) {
				int indexOfperiod = (int) ((graph.getPeriod().getTime() - start.getTime()) / TimeUtil.ONE_HOUR * 12);
				String summaryContent = graph.getSummaryContent();
				String[] allLines = summaryContent.split("\n");
				for (int j = 0; j < allLines.length; j++) {
					String[] records = allLines[j].split("\t");
					if (records[SummaryOrder.TYPE.ordinal()].equals(type)) {
						appendArray(total_count, indexOfperiod, records[SummaryOrder.TOTAL_COUNT.ordinal()], 12);
						appendArray(failure_count, indexOfperiod, records[SummaryOrder.FAILURE_COUNT.ordinal()], 12);
					}
				}
			}
		} else if (!StringUtils.isEmpty(type) && !StringUtils.isEmpty(name)) {
			for (Graph graph : graphs) {
				int indexOfperiod = (int) ((graph.getPeriod().getTime() - start.getTime()) / TimeUtil.ONE_HOUR * 12);
				String detailContent = graph.getDetailContent();
				String[] allLines = detailContent.split("\n");
				for (int j = 0; j < allLines.length; j++) {
					String[] records = allLines[j].split("\t");
					if (records[DetailOrder.TYPE.ordinal()].equals(type) && records[DetailOrder.NAME.ordinal()].equals(name)) {

						appendArray(total_count, indexOfperiod, records[DetailOrder.TOTAL_COUNT.ordinal()], 12);
						appendArray(failure_count, indexOfperiod, records[DetailOrder.FAILURE_COUNT.ordinal()], 12);
					}
				}
			}
		}

		result.put("total_count", total_count);
		result.put("failure_count", failure_count);
		return result;
	}

	public void buildGroupTrendGraph(Model model, Payload payload, List<String> ips) {
		Date start = payload.getHistoryStartDate();
		Date end = payload.getHistoryEndDate();
		String domain = model.getDomain();
		String type = payload.getType();
		String name = payload.getName();
		String display = name != null ? name : type;
		int size = (int) ((end.getTime() - start.getTime()) * 12 / TimeUtil.ONE_HOUR);
		long step = TimeUtil.ONE_MINUTE * 5;
		String queryType = payload.getReportType();

		if (queryType.equalsIgnoreCase("month")) {
			size = (int) ((end.getTime() - start.getTime()) / TimeUtil.ONE_DAY);
			step = TimeUtil.ONE_DAY;
		}

		List<Map<String, double[]>> allDatas = null;
		for (String ip : ips) {
			List<Map<String, double[]>> datas = buildLineChartData(start, end, domain, type, name, ip, queryType);

			if (allDatas == null) {
				allDatas = datas;
			} else {
				mergerList(allDatas, datas);
			}
		}
		LineChart item = buildTotal(allDatas, start, size, step, display, queryType);
		model.setHitTrend(item.getJsonString());

		item = buildFail(allDatas, start, size, step, display, queryType);
		model.setFailureTrend(item.getJsonString());
	}

	private List<Map<String, double[]>> buildLineChartData(Date start, Date end, String domain, String type,
	      String name, String ip, String queryType) {
		List<Map<String, double[]>> allDatas = new ArrayList<Map<String, double[]>>();
		
		if (queryType.equalsIgnoreCase("day")) {
			Map<String, double[]> currentGraph = getGraphDatasForHour(start, end, domain, type, name, ip);
			Map<String, double[]> lastDayGraph = getGraphDatasForHour(new Date(start.getTime() - TimeUtil.ONE_DAY),
			      new Date(end.getTime() - TimeUtil.ONE_DAY), domain, type, name, ip);
			Map<String, double[]> lastWeekGraph = getGraphDatasForHour(new Date(start.getTime() - TimeUtil.ONE_WEEK),
			      new Date(end.getTime() - TimeUtil.ONE_WEEK), domain, type, name, ip);

			allDatas.add(currentGraph);
			allDatas.add(lastDayGraph);
			allDatas.add(lastWeekGraph);
		} else if (queryType.equalsIgnoreCase("week")) {
			Map<String, double[]> currentGraph = getGraphDatasForHour(start, end, domain, type, name, ip);
			Map<String, double[]> lastWeek = getGraphDatasForHour(new Date(start.getTime() - TimeUtil.ONE_WEEK), new Date(
			      end.getTime() - TimeUtil.ONE_WEEK), domain, type, name, ip);

			allDatas.add(currentGraph);
			allDatas.add(lastWeek);
		} else if (queryType.equalsIgnoreCase("month")) {
			Map<String, double[]> graphData = getGraphDatasFromDaily(start, end, domain, type, name, ip);

			allDatas.add(graphData);
		} else {
			throw new RuntimeException("Error graph query type");
		}
		return allDatas;
	}

	private LineChart buildTotal(List<Map<String, double[]>> datas, Date start, int size, long step, String name,
	      String queryType) {
		LineChart item = new LineChart();

		item.setStart(start);
		item.setSize(size);
		item.setStep(step);
		item.setTitle(name + " Hits (count)");

		for (Map<String, double[]> data : datas) {
			double[] totalCount = data.get("total_count");
			item.addValue(totalCount);
		}
		item.setSubTitles(buildSubTitle(start, size, step, queryType));
		return item;
	}

	public void buildTrendGraph(Model model, Payload payload) {
		Date start = payload.getHistoryStartDate();
		Date end = payload.getHistoryEndDate();
		String type = payload.getType();
		String name = payload.getName();
		String domain = model.getDomain();
		String ip = model.getIpAddress();
		String display = name != null ? name : type;
		int size = (int) ((end.getTime() - start.getTime()) * 12 / TimeUtil.ONE_HOUR);
		String queryType = payload.getReportType();
		long step = TimeUtil.ONE_MINUTE * 5;

		if (queryType.equalsIgnoreCase("month")) {
			size = (int) ((end.getTime() - start.getTime()) / TimeUtil.ONE_DAY);
			step = TimeUtil.ONE_DAY;
		}

		List<Map<String, double[]>> allDatas = buildLineChartData(start, end, type, name, domain, ip, queryType);

		LineChart item = buildTotal(allDatas, start, size, step, display, queryType);
		model.setHitTrend(item.getJsonString());

		item = buildFail(allDatas, start, size, step, display, queryType);
		model.setFailureTrend(item.getJsonString());
	}

	public Map<String, double[]> getGraphDatasForHour(Date start, Date end, String domain, String type, String name,
	      String ip) {
		String queryIP = "All".equals(ip) == true ? "all" : ip;
		List<Graph> events = new ArrayList<Graph>();
		for (long startLong = start.getTime(); startLong < end.getTime(); startLong = startLong + TimeUtil.ONE_HOUR) {
			try {
				Graph graph = m_graphDao.findSingalByDomainNameIpDuration(new Date(startLong), queryIP, domain,
				      EventAnalyzer.ID, GraphEntity.READSET_FULL);
				events.add(graph);
			} catch (DalNotFoundException e) {
			} catch (Exception e) {
				Cat.logError(e);
			}
		}
		return buildGraphDatasForHour(start, end, type, name, events);
	}

	private Map<String, double[]> getGraphDatasFromDaily(Date start, Date end, String domain, String type, String name,
	      String ip) {
		String queryIp = "All".equalsIgnoreCase(ip) == true ? "All" : ip;
		List<DailyGraph> graphs = new ArrayList<DailyGraph>();

		for (long startLong = start.getTime(); startLong < end.getTime(); startLong = startLong + TimeUtil.ONE_DAY) {
			try {
				DailyGraph graph = m_dailyGraphDao.findByDomainNameIpDate(new Date(startLong), queryIp, domain,
				      EventAnalyzer.ID, DailyGraphEntity.READSET_FULL);
				graphs.add(graph);
			} catch (DalNotFoundException e) {
			} catch (Exception e) {
				Cat.logError(e);
			}
		}
		return buildGraphDatasForDaily(start, end, type, name, graphs);
	}
}
