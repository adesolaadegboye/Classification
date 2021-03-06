package misc;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import dc.GP.AbstractNode;
import dc.GP.Add;
import dc.GP.Const;

import dc.GP.TreeHelperClass;
import dc.GP.TreeOperation;
import dc.GP.Const.treeStructurePostcreate;

import dc.ga.GA;
import dc.ga.PreProcess;

import static dc.ga.DCCurve.Type.Downturn;
import static dc.ga.DCCurve.Type.DownwardOvershoot;
import static dc.ga.DCCurve.Type.Upturn;
import static dc.ga.DCCurve.Type.UpwardOvershoot;

import dc.ga.DCCurve.Event;
import dc.ga.DCCurve.Type;
import dc.io.FReader;
import dc.io.Logger;
import eu.verdelhan.ta4j.BaseStrategy;
import eu.verdelhan.ta4j.BaseTick;
import eu.verdelhan.ta4j.BaseTimeSeries;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TimeSeriesManager;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.indicators.EMAIndicator;
import eu.verdelhan.ta4j.indicators.SMAIndicator;
import eu.verdelhan.ta4j.indicators.helpers.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.statistics.StandardDeviationIndicator;
import eu.verdelhan.ta4j.trading.rules.OverIndicatorRule;
import eu.verdelhan.ta4j.trading.rules.UnderIndicatorRule;
import files.FWriter;
import misc.technicalAnalysis.AroonIndicator;
import misc.technicalAnalysis.BolingerBAndS;
import misc.technicalAnalysis.CommodityChannelIndexIndicator;
import misc.technicalAnalysis.ExponentialMovingAverageFX;
import misc.technicalAnalysis.MovingMomentum;
import misc.technicalAnalysis.ParabolicSar;
import misc.technicalAnalysis.PriceRateOfChangeIndicator;
import misc.technicalAnalysis.RelativeStrengthIndex;
import misc.technicalAnalysis.SimpleMovingAverage;
import misc.technicalAnalysis.StochOscillatorKIndicator;
import misc.technicalAnalysis.TechnicalAnaysisBaseTrading;
import misc.technicalAnalysis.WilliamR;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;

public class SymbolicRegression {

	int numberOfThresholds;
	protected double[] THRESHOLDS;
	protected double[] SELECTED_THRESHOLDS;
	protected double[] SELECTED_THRESHOLDS_MF;
	protected double[] SELECTED_THRESHOLDS_OLSEN;
	

	protected double[] SELECTED_THRESHOLDS_CLASSIFICATION_ONLY;

	
	
	double thresholdIncrement;
	String autowekaRunsList = "";
	ConcurrentHashMap<Integer, String> gpDaysMap = null;
	int TrainingDay = -1;
	PreProcess[] preprocess = null;
	PreProcess[] preprocessMF = null;
	PreProcess[] preprocessOlsen = null;
	
	Map<String, Event[]> testEventsArray = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> testEventsArrayMF = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> testEventsArrayOlsen = new LinkedHashMap<String, Event[]>();
	

	Map<String, Event[]> trainingEventsArray = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> trainingEventsArrayMF = new LinkedHashMap<String, Event[]>();
	Map<String, Event[]> trainingEventsArrayOlsen = new LinkedHashMap<String, Event[]>();
	
	static int currentProcessorCounter;
	static Instances testInstance; // This is needed because PreProcess is
									// static

	
	int currentGeneration;

	Double[] training;
	Double[] test;
	DCCurveRegression[] curveCifre;
	DCCurveRegression[] curveMF;
	DCCurveRegression[] curveOlsen;
	

	DCCurveRegression[] curveClassifcationMF;
	DCCurveRegression[] curveClassifcation;
	DCCurveRegression[] curveClassificationOlsen;
	
	
	

	DCCurveRegression[] curvePerfectForesight;
	DCCurveRegression[] curvePerfectForesightMF;
	DCCurveRegression[] curvePerfectForesightOlsen;
	

	DCCurveRegression[] curveRandomGP;
	DCCurveRegression[] curveRandomMF;
	DCCurveRegression[] curveRandomOlsen;
	
	DCCurveRegression[] curveNoClassificationNoRegressionGP;
	DCCurveRegression[] curveNoClassificationNoRegressionMF;
	DCCurveRegression[] curveNoClassificationNoRegressionOlsen;
	

	
	//static Map<Double, String> thresholdGPStringMap = new HashMap<Double, String>();
	Map<Double, Double> perfectForecastReturnMap = new HashMap<Double, Double>();
	Map<Double, Double> perfectForecastMFReturnMap = new HashMap<Double, Double>();
	Map<Double, Double> perfectForecastOlsenReturnMap = new HashMap<Double, Double>();
	
	

	// Map<String, Double> CGPReturnMap = new HashMap<String, Double>();
	// to decide whether to split dataset to upward and downward trend datasets
	
	
	
	@SuppressWarnings({ "static-access", "unused" })
	public SymbolicRegression(String filename, int trainingIndexStart, int trainingIndexEnd, int testIndexStart,
			int testIndexEnd, int populationSize, int noOfGENERATIONS, int tournamentSize, double crossOverProb,
			int maxDepth, double threshold) throws IOException, ParseException {

		numberOfThresholds = Const.NUMBER_OF_THRESHOLDS;
		Const.POP_SIZE = populationSize;
		Const.MAX_GENERATIONS = noOfGENERATIONS;
		Const.TOURNAMENT_SIZE = tournamentSize;
		Const.CROSSOVER_PROB = crossOverProb;
		Const.MAX_DEPTH = maxDepth;
		// Const.EVOLUTION_RATIO = reproductionRatio;
		Const.file_Name = filename;

		TrainingDay = trainingIndexStart;
		THRESHOLDS = new double[Const.NUMBER_OF_THRESHOLDS];// Adesola change
															// after tunning to

		gpDaysMap = FReader.loadDataMap(filename);

		for (int i = 0; i < THRESHOLDS.length; i++) {
			// THRESHOLDS[i] = (initial * (i + 1)) / 100.0;
			// THRESHOLDS[i] = (initial + (0.0020 * i)) / 100.0; //Correct one
			// /Right one, replace after test
			THRESHOLDS[i] = (0.005 + (0.0025 * i)) / 100.0;
			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			// System.out.println(thresholdStr);
		}

		// System.out.println("Loading directional changes data...");

		// loads the data
		ArrayList<Double[]> days = FReader.loadData(filename, false);

		// allow the creation of training & testing data sets that are longer
		// than 1 day
		ArrayList<Double[]> ar = new ArrayList<Double[]>();
		for (int i = trainingIndexStart; i <= trainingIndexEnd; i++)
			ar.add(days.get(i));
		int size = 0;
		for (Double[] d : ar)
			size += d.length;
		training = new Double[size];
		int counter = 0;
		for (Double[] d : ar) {
			for (double n : d) {
				training[counter] = n;
				counter++;
			}
		}
		ar = new ArrayList<Double[]>();
		for (int i = testIndexStart; i <= testIndexEnd; i++) {
			// System.out.println("Print " + i);
			ar.add(days.get(i));
		}
		size = 0;
		for (Double[] d : ar)
			size += d.length;
		test = new Double[size];
		counter = 0;
		for (Double[] d : ar) {
			for (double n : d) {
				test[counter] = n;
				counter++;
			}
		}
		// budget = 100000;

		int timeSeriesDataPt = training.length;
		List<Tick> bidTicks = new ArrayList<>();
		List<Tick> askTicks = new ArrayList<>();

		for (int timeSeriesCount = timeSeriesDataPt; timeSeriesCount < FReader.dataRecordInFileArray.size()
				&& bidTicks.size() < test.length; timeSeriesCount++) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
			LocalDateTime datetime = LocalDateTime.parse(FReader.dataRecordInFileArray.get(timeSeriesCount).Day + " "
					+ FReader.dataRecordInFileArray.get(timeSeriesCount).time, formatter);
			ZonedDateTime klDateTIme = datetime.atZone(ZoneId.systemDefault());
			bidTicks.add(new BaseTick(klDateTIme, 0.0, 0.0, 0.0,
					Double.parseDouble(FReader.dataRecordInFileArray.get(timeSeriesCount).bidPrice), 0.0));
			askTicks.add(new BaseTick(klDateTIme, 0.0, 0.0, 0.0,
					Double.parseDouble(FReader.dataRecordInFileArray.get(timeSeriesCount).askPrice), 0.0));

			// System.out.println(bidTicks.get(0).toString());
		}

		String fileNameWithoutExtension = filename.split("\\.")[0]; // extension
																	// removed
		TechnicalAnaysisBaseTrading emaTrading = new ExponentialMovingAverageFX(bidTicks, askTicks,
				fileNameWithoutExtension, 500000.00);

		TechnicalAnaysisBaseTrading rsiTrading = new RelativeStrengthIndex(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		TechnicalAnaysisBaseTrading mcadTrading = new MovingMomentum(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading bollingerBAndS = new BolingerBAndS(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading commodityChannelIndexIndicator = new CommodityChannelIndexIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading simpleMovingAverage = new SimpleMovingAverage(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading parabolicSar = new ParabolicSar(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading aroonIndicator = new AroonIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		TechnicalAnaysisBaseTrading stochOscillatorKIndicator = new StochOscillatorKIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		
		
		TechnicalAnaysisBaseTrading williamR = new WilliamR(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		
		
		TechnicalAnaysisBaseTrading priceRateOfChangeIndicator = new PriceRateOfChangeIndicator(bidTicks, askTicks, fileNameWithoutExtension,
				500000.00);
		
		rsiTrading.getOrders();
		mcadTrading.getOrders();
		emaTrading.getOrders();
		bollingerBAndS.getOrders();
		commodityChannelIndexIndicator.getOrders();
		simpleMovingAverage.getOrders();
		parabolicSar.getOrders();
		aroonIndicator.getOrders();
		stochOscillatorKIndicator.getOrders();
		williamR.getOrders();
		priceRateOfChangeIndicator.getOrders();
		
		
		
		double rsiTradeResult = rsiTrading.trade();
		double mcadTradeResult = mcadTrading.trade();
		double emaTradeResult = ((ExponentialMovingAverageFX) emaTrading).trade();
		double bollingerBAndSResult =  bollingerBAndS.trade();
		double commodityChannelIndexIndicatorResult = commodityChannelIndexIndicator.trade();
		double simpleMovingAverageResult =  simpleMovingAverage.trade();
		double parabolicSarResult = parabolicSar.trade();
		double aroonIndicatorResult = aroonIndicator.trade();
		double stochOscillatorKIndicatorResult =  stochOscillatorKIndicator.trade();
		double williamRResult = williamR.trade();
		double priceRateOfChangeIndicatorResult = priceRateOfChangeIndicator.trade();
		
		
		String TechnicalIndicatorMDD = "Dataset \t bollinger \t "
				+ "commodityChannelIndexIndicatorResult \t SMA  "
				+ "\t parabolicSarResult \t aroon \t StochastichOscillatorK \t"
				+ " williamR \t ROC "
				+ "\t RSI \t EMA \t MACD  ";
		
		FWriter TechnicalIndicatorMDDWriter = new FWriter(Const.log.publicFolder + "TechnicalIndicatorMDDBaseCcy.txt");
		Const.log.save("TechnicalIndicatorMDDBaseCcy.txt", TechnicalIndicatorMDD);
		
		for (int printCount = 0  ; printCount < 5 ; printCount++ ){
		
		TechnicalIndicatorMDD = filename + " \t" 
				+ bollingerBAndS.getMaxMddBase() + "\t"
				+ commodityChannelIndexIndicator.getMaxMddBase() + "\t"
				+ simpleMovingAverage.getMaxMddBase() + "\t"
				+ parabolicSar.getMaxMddBase() + "\t"
				+ aroonIndicator.getMaxMddBase() + "\t"
				+ stochOscillatorKIndicator.getMaxMddBase() +"\t"
				+ williamR.getMaxMddBase() + "\t"
				+ priceRateOfChangeIndicator.getMaxMddBase() + "\t"
				+ rsiTrading.getMaxMddBase() + "\t" + emaTrading.getMaxMddBase() + "\t" + mcadTrading.getMaxMddBase();
		Const.log.save("TechnicalIndicatorMDDBaseCcy.txt", TechnicalIndicatorMDD);
		}
		
		String TechnicalIndicatorReturn = "Dataset \t bollinger \t "
				+ "commodityChannelIndexIndicatorResult \t SMA  "
				+ "\t parabolicSarResult \t aroon \t StochastichOscillatorK \t"
				+ " williamR \t ROC "
				+ "\t RSI \t EMA \t MACD  ";
		FWriter TechnicalIndicatorReturnWriter = new FWriter(Const.log.publicFolder + "TechnicalIndicatorSimpleTradingResult.txt");
		Const.log.save("TechnicalIndicatorSimpleTradingResult.txt", TechnicalIndicatorReturn);
		

		
		
		
		DecimalFormat df = new DecimalFormat("#.####");
		df.setRoundingMode(RoundingMode.CEILING);
	
		for (int printCount = 0  ; printCount < 5 ; printCount++ ){
			
		
		TechnicalIndicatorReturn = filename + " \t" 
				+ df.format((( bollingerBAndSResult - 500000)/500000) * 100) + "\t"
				+ df.format(((commodityChannelIndexIndicatorResult - 500000)/500000) * 100) + "\t"
				+ df.format(((simpleMovingAverageResult  - 500000)/500000) * 100 )+ "\t"
				+ df.format(((parabolicSarResult - 500000)/ 500000) * 100) + "\t"
				+ df.format(((aroonIndicatorResult - 500000)/ 500000) * 100) + "\t"
				+ df.format(((stochOscillatorKIndicatorResult  - 500000)/500000) * 100) +"\t"
				+ df.format(((williamRResult - 500000)/500000) * 100) + "\t"
				+ df.format(((priceRateOfChangeIndicatorResult - 500000)/500000) * 100) + "\t"
				+ df.format(((rsiTradeResult - 500000)/500000) * 100) + "\t"
				+ df.format(((emaTradeResult - 500000)/500000) * 100) + "\t"
				+ df.format(((mcadTradeResult - 500000)/500000) * 100);
		Const.log.save("TechnicalIndicatorSimpleTradingResult.txt", TechnicalIndicatorReturn);
		}
		
	
		String TechnicalIndicatorSR = "Dataset \t bollinger \t "
				+ "commodityChannelIndexIndicatorResult \t SMA  "
				+ "\t parabolicSarResult \t aroon \t StochastichOscillatorK \t"
				+ " williamR \t ROC "
				+ "\t RSI \t EMA \t MACD  ";
		
		
		FWriter TechnicalIndicatorSharpeRatioWriter = new FWriter(Const.log.publicFolder + "TechnicalIndicatorSharpeRatioBaseCcy.txt");
		Const.log.save("TechnicalIndicatorSharpeRatio.txt", TechnicalIndicatorSR);
		
		
		for (int printCount = 0  ; printCount < 5 ; printCount++ ){
			
			TechnicalIndicatorSR = filename + " \t" 
					+ bollingerBAndS.getSharpRatio() + "\t"
					+ commodityChannelIndexIndicator.getSharpRatio() + "\t"
					+ simpleMovingAverage.getSharpRatio() + "\t"
					+ parabolicSar.getSharpRatio() + "\t"
					+ aroonIndicator.getSharpRatio() + "\t"
					+ stochOscillatorKIndicator.getSharpRatio() +"\t"
					+ williamR.getSharpRatio() + "\t"
					+ priceRateOfChangeIndicator.getSharpRatio() + "\t"
					+ rsiTrading.getSharpRatio() + "\t" + emaTrading.getSharpRatio() + "\t" + mcadTrading.getSharpRatio();
			Const.log.save("TechnicalIndicatorSharpeRatio.txt", TechnicalIndicatorSR);
			}

		
		

		curvePerfectForesight = new DCCurvePerfectForesight[THRESHOLDS.length];
		curvePerfectForesightMF = new DCCurvePerfectForesightMF[THRESHOLDS.length];
		curvePerfectForesightOlsen = new DCCurvePerfectForesightOlsen[THRESHOLDS.length];
		

		curveCifre = new DCCurveCifre[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveClassifcation = new DCCurveClassification[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveRandomGP = new DCCurveRandomGP[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		curveNoClassificationNoRegressionGP = new DCCurveNoClassificationNoRegression[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		
		
		
		
		curveMF = new DCCurveMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveClassifcationMF = new DCCurveClassificationMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveRandomMF = new DCCurveRandomMF[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveNoClassificationNoRegressionMF = new DCCurveNoClassificationNoRegression[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		curveOlsen = new DCCurveOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveClassificationOlsen = new DCCurveClassificationOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveRandomOlsen = new DCCurveRandomOlsen[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		curveNoClassificationNoRegressionOlsen = new DCCurveNoClassificationNoRegression[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		

		

		preprocess = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessMF = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		preprocessOlsen = new PreProcess[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		

		String[] testArrayPerfectForesight = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayCifre = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassification = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandom = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionGP = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		
		String[] testArrayPerfectForesightMF = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandomMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionMF = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		String[] testArrayPerfectForesightOlsen = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandomOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionOlsen = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		String[] testArrayPerfectForesightTOS = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayTOSAverage = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationTOSAverage = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandomTOSAverage = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionTOSAverage = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		String[] testArrayPerfectForesightTOSDDCUDC = new String[Const.NUMBER_OF_THRESHOLDS];
		String[] testArrayTOSDCCUDC = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationTOSDDCUDC = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayRandomTOSDDCUDC = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];
		String[] testArrayClassificationNoRegressionTOSDDCUDC = new String[Const.NUMBER_OF_SELECTED_THRESHOLDS];

		String autoWEKAClassifierListEvalString = "Dataset \t Threshold \t classification_type \t Accuracy \t TP_Rate \t FP_Rate \t Precision \t Recall \t F-Measure \t MCC \t ROC_Area \t PRC_Area  \t Class";

		FWriter autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluation.txt");

		autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluationMF.txt");

		autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluationOlsen.txt");

		autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluationTOS.txt");

		autoWEKAClassifierListEvaluation = new FWriter(
				Const.log.publicFolder + "autoWEKAClassifierListEvaluationTOSDDCUDC.txt");

		Const.log.save("autoWEKAClassifierListEvaluation.txt", autoWEKAClassifierListEvalString);
		Const.log.save("autoWEKAClassifierListEvaluationMF.txt", autoWEKAClassifierListEvalString);
		Const.log.save("autoWEKAClassifierListEvaluationOlsen.txt", autoWEKAClassifierListEvalString);
		Const.log.save("autoWEKAClassifierListEvaluationTOS.txt", autoWEKAClassifierListEvalString);
		Const.log.save("autoWEKAClassifierListEvaluationTOSDDCUDC.txt", autoWEKAClassifierListEvalString);

		/*	String SimpleTradingTraining = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t Random+GP \t GP+DCC \t PerfectForesight+MF \t MF  \t Classifier+MF \t Random+MF \t MF+DCC  \t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen \t Random+Olsen \t Olsen+DCC  \t PerfectForesight+H  \t H \t Classifier+H \t Random+H \t H+DCC \t H+DCC_trail  \t PerfectForesight+HSplitDCUp_Down \t HSplitDCUp_Down \t Classifier+HSplitDCUp_Down \t Random+HSplitDCUp_Down \t HSplitDCUp_Down+DCC \t HSplitDCUp_Down+DCC_trail ";

		FWriter SimpleTradingTrainingWriter = new FWriter(
				Const.log.publicFolder + "SimpleTradingTraining.txt");
		Const.log.save("SimpleTradingTraining.txt", SimpleTradingTraining);
*/
		String selectedThresholds = "Dataset \t ThresholdGP \t zeroOvershootGP  \t  OvershootLengthPercentageGP"
				+ "\t ThresholdMF   \t zeroOvershootMF  \t  OvershootLengthPercentagetMF  "
				+ "\t  ThresholdOlsen  \t zeroOvershootOlsen  \t  OvershootLengthPercentagetOlsen "
				+ "\t  HThresholdH  \t zeroOvershootH  \t  OvershootLengthPercentagetH "
				+ "\t  ThresholdHDDCUDC \t zeroOvershootHDCCUDC  \t  OvershootLengthPercentagetHDCCUDC  ";

		FWriter SimpleTradingTrainingWriter = new FWriter(Const.log.publicFolder + "selectedThresholdsTest.txt");
		Const.log.save("selectedThresholdsTest.txt", selectedThresholds);

		SimpleTradingTrainingWriter = new FWriter(
				Const.log.publicFolder + "selectedThresholdsTraining.txt");
		Const.log.save("selectedThresholdsTraining.txt", selectedThresholds);

		Event[] copiedArray;
		
		for (int i = 0; i < THRESHOLDS.length; i++) { // The arrays all have
			// same size

			curvePerfectForesightMF[i] = new DCCurvePerfectForesightMF();

			String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
			String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
			String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";


			DCEventGenerator dCEventGenerator = new DCEventGenerator();
			dCEventGenerator.generateEvents(training, THRESHOLDS[i]);
			copiedArray =Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
			if (copiedArray.length < 10)
				continue;

			curvePerfectForesightMF[i].filename = filename;
			curvePerfectForesightMF[i].build(training, THRESHOLDS[i], gpFileName, copiedArray, null);
			curvePerfectForesightMF[i].estimateTraining(null); // null
																// because
			// not doing
			// classification

			double perfectForcastTrainingReturn = curvePerfectForesightMF[i].trainingTrading(null);
			perfectForecastMFReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);

		}

		
		for (int runCount = 0; runCount < 1; runCount++) {
			// select threshold via perfect foresight
			for (int i = 0; i < THRESHOLDS.length; i++) { // The arrays all have
															// same size

				curvePerfectForesight[i] = new DCCurvePerfectForesight();

				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(training, THRESHOLDS[i]);
				copiedArray = Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				trainingEventsArray.put(thresholdStr, copiedArray);

				if (copiedArray.length < 10)
					continue;

				curvePerfectForesight[i].filename = filename;
				curvePerfectForesight[i].build(training, THRESHOLDS[i], gpFileName, copiedArray, null);
				curvePerfectForesight[i].estimateTraining(null); // null because
																	// not doing
																	// classification

				double perfectForcastTrainingReturn = curvePerfectForesight[i].trainingTrading(null);
				perfectForecastReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			}

			// populated threshold of all algorithms
			trainingEventsArray.forEach((k, v) -> {
				trainingEventsArrayMF.put(k, Arrays.copyOf(v, v.length));
				trainingEventsArrayOlsen.put(k, Arrays.copyOf(v, v.length));
			});

			

			for (int i = 0; i < THRESHOLDS.length; i++) { // The arrays all have
				// same size
				curvePerfectForesightOlsen[i] = new DCCurvePerfectForesightOlsen();

				String thresholdStr = String.format("%.8f", THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";


				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(training, THRESHOLDS[i]);
				copiedArray =Arrays.copyOf(dCEventGenerator.getEvents(), dCEventGenerator.getEvents().length);
				if (copiedArray.length < 10)
					continue;
				curvePerfectForesightOlsen[i].filename = filename;
				curvePerfectForesightOlsen[i].build(training, THRESHOLDS[i], gpFileName, copiedArray, null);
				curvePerfectForesightOlsen[i].estimateTraining(null); // null
																		// because
				// not doing
				// classification
				double perfectForcastTrainingReturn = curvePerfectForesightOlsen[i].trainingTrading(null);
				perfectForecastOlsenReturnMap.put(THRESHOLDS[i], perfectForcastTrainingReturn);
			}



			// Select best threshold based on perfect forecast result
			SELECTED_THRESHOLDS = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			SELECTED_THRESHOLDS_MF = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			SELECTED_THRESHOLDS_OLSEN = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			
			SELECTED_THRESHOLDS_CLASSIFICATION_ONLY = new double[Const.NUMBER_OF_SELECTED_THRESHOLDS];
			List<Entry<Double, Double>> greatest = findGreatest(perfectForecastReturnMap,
					Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
			
			
			// 5
			// thresholds

			int tradingThresholdCount = 0;
			System.out.println("Selecting GP threshold");
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				SELECTED_THRESHOLDS[tradingThresholdCount] = entry.getKey();
				System.out.println(SELECTED_THRESHOLDS[tradingThresholdCount]);
				tradingThresholdCount++;
			}

			greatest.clear();
			greatest = findGreatest(perfectForecastMFReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
																										// 5
																										// thresholds
			tradingThresholdCount = 0;
			System.out.println("Selecting MF threshold");
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				SELECTED_THRESHOLDS_MF[tradingThresholdCount] = entry.getKey();
				System.out.println(SELECTED_THRESHOLDS_MF[tradingThresholdCount]);
				tradingThresholdCount++;
			}

			greatest.clear();
			greatest = findGreatest(perfectForecastOlsenReturnMap, Const.NUMBER_OF_SELECTED_THRESHOLDS); // best
			// 5
			// thresholds
			tradingThresholdCount = 0;
			System.out.println("Selecting Olsen threshold");
			for (Entry<Double, Double> entry : greatest) {
				// System.out.println(entry);
				SELECTED_THRESHOLDS_OLSEN[tradingThresholdCount] = entry.getKey();
				System.out.println(SELECTED_THRESHOLDS_OLSEN[tradingThresholdCount]);
				tradingThresholdCount++;
			}

			

			
			// DO classification for all thresholds
			for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);

				int arrayLength = trainingEventsArray.get(thresholdStr).length;
				Arrays.copyOf(trainingEventsArray.get(thresholdStr), arrayLength);
				copiedArray = Arrays.copyOf(trainingEventsArray.get(thresholdStr), arrayLength);
				// trainingEventsArray.put(thresholdStr, copiedArray);
				preprocess[i] = null;
				preprocess[i] = new PreProcess(SELECTED_THRESHOLDS[i], filename, "GP");

				preprocess[i].buildTraining(copiedArray);
				preprocess[i].selectBestClassifier();

				// loadTrainingData load classification data "order is
				// important"
				preprocess[i].loadTrainingData(copiedArray);

				preprocess[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];

				preprocess[i].runAutoWeka();

			}

			for (int i = 0; i < SELECTED_THRESHOLDS_MF.length; i++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[i]);

				copiedArray = Arrays.copyOf(trainingEventsArrayMF.get(thresholdStr),
						trainingEventsArrayMF.get(thresholdStr).length);
				// trainingEventsArrayMF.put(thresholdStr, copiedArray);
				preprocessMF[i] = null;
				preprocessMF[i] = new PreProcess(SELECTED_THRESHOLDS_MF[i], filename, "MF");

				preprocessMF[i].buildTraining(copiedArray);
				preprocessMF[i].selectBestClassifier();

				// loadTrainingData load classification data "order is
				// important"
				preprocessMF[i].loadTrainingData(copiedArray);

				preprocessMF[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];

				preprocessMF[i].runAutoWeka();

			}

			for (int i = 0; i < SELECTED_THRESHOLDS_OLSEN.length; i++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[i]);
				copiedArray = Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
						trainingEventsArrayOlsen.get(thresholdStr).length);
				// trainingEventsArrayOlsen.put(thresholdStr, copiedArray);
				preprocessOlsen[i] = null;
				preprocessOlsen[i] = new PreProcess(SELECTED_THRESHOLDS_OLSEN[i], filename, "Olsen");

				preprocessOlsen[i].buildTraining(copiedArray);
				preprocessOlsen[i].selectBestClassifier();

				// loadTrainingData load classification data "order is
				// important"
				preprocessOlsen[i].loadTrainingData(copiedArray);
				preprocessOlsen[i].lastTrainingEvent = copiedArray[copiedArray.length - 1];
				preprocessOlsen[i].runAutoWeka();

			}

			
			

			if ((SELECTED_THRESHOLDS.length != SELECTED_THRESHOLDS_MF.length)
					|| (SELECTED_THRESHOLDS_MF.length != SELECTED_THRESHOLDS_OLSEN.length)) {
				System.out.println(" SELECTED_THRESHOLDS  ARRAYS are not matching");
				System.exit(-1);
			}

			for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) { // The arrays
																	// all have
																	// same size
				currentProcessorCounter = i;

				curveCifre[i] = new DCCurveCifre();
				curveClassifcation[i] = new DCCurveClassification();
				curveRandomGP[i] = new DCCurveRandomGP();
				curveNoClassificationNoRegressionGP[i] = new DCCurveNoClassificationNoRegression();

				
				curveMF[i] = new DCCurveMF();
				curveClassifcationMF[i] = new DCCurveClassificationMF();
				curveRandomMF[i] = new DCCurveRandomMF();
				curveNoClassificationNoRegressionMF[i] = new DCCurveNoClassificationNoRegression();

				curveOlsen[i] = new DCCurveOlsen();
				curveClassificationOlsen[i] = new DCCurveClassificationOlsen();
				curveRandomOlsen[i] = new DCCurveRandomOlsen();
				curveNoClassificationNoRegressionOlsen[i] = new DCCurveNoClassificationNoRegression();


				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
				String gpFileNamePrefix = gpDaysMap.get(trainingIndexStart);
				String gpFileName = gpFileNamePrefix + "_" + thresholdStr + ".txt";

				// System.out.println(thresholdStr);

				// System.out.println("GA:" + gpFileNamePrefix);
				String regressionModelGP[] = new String[2];
				

				for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesight.length; thresholdCounter++) {
					String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[i]);
					if (thisThresholdStr
							.equalsIgnoreCase(curvePerfectForesight[thresholdCounter].getThresholdString())) {
						regressionModelGP[0] = curvePerfectForesight[thresholdCounter].getDownwardTrendTreeString();
						regressionModelGP[1] = curvePerfectForesight[thresholdCounter].getUpwardTrendTreeString();

						break;
					}
				}

				curveCifre[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveCifre[i].assignPerfectForesightRegressionModel(regressionModelGP);

				curveCifre[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays.copyOf(
						trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length), null);
				curveCifre[i].estimateTraining(null);

				curveClassifcation[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelGP[0] != null && !regressionModelGP[0].isEmpty() && regressionModelGP[1] != null
						&& !regressionModelGP[1].isEmpty())
					curveClassifcation[i].assignPerfectForesightRegressionModel(regressionModelGP);

				curveClassifcation[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						preprocess[i]);
				curveClassifcation[i].estimateTraining(preprocess[i]);

				
				
				
				curveRandomGP[i].filename = filename;
				// Assign perfect foresight regression Model here
				// if (regressionModelGP[0] != null &&
				// !regressionModelGP[0].isEmpty() &&
				// regressionModelGP[1] != null &&
				// !regressionModelGP[1].isEmpty())
				// curveRandomGP[i].assignPerfectForesightRegressionModel(regressionModelGP);

				curveRandomGP[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays.copyOf(
						trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length), null);
				curveRandomGP[i].estimateTraining(null);

				

				curveNoClassificationNoRegressionGP[i].filename = filename;
				curveNoClassificationNoRegressionGP[i].build(training, SELECTED_THRESHOLDS[i], gpFileName, Arrays
						.copyOf(trainingEventsArray.get(thresholdStr), trainingEventsArray.get(thresholdStr).length),
						null);
				curveNoClassificationNoRegressionGP[i].estimateTraining(null);

				thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[i]);
				Double regressionModelMF[] = new Double[2];
				regressionModelMF[0] = new Double(0.0);
				regressionModelMF[1] = new Double(0.0);

				for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesightMF.length; thresholdCounter++) {
					String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[i]);
					if (thisThresholdStr
							.equalsIgnoreCase(curvePerfectForesightMF[thresholdCounter].getThresholdString())) {
						regressionModelMF[0] = new Double(
								curvePerfectForesightMF[thresholdCounter].getDownwardTrendRatio());
						regressionModelMF[1] = new Double(
								curvePerfectForesightMF[thresholdCounter].getUpwardTrendRatio());
						break;
					}
				}

				curveMF[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				curveMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays.copyOf(
						trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length), null);
				curveMF[i].estimateTraining(null);

				curveClassifcationMF[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue() > 0.0)
					curveClassifcationMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				curveClassifcationMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayMF.get(thresholdStr),
								trainingEventsArrayMF.get(thresholdStr).length),
						preprocessMF[i]);
				curveClassifcationMF[i].estimateTraining(preprocessMF[i]);

				curveRandomMF[i].filename = filename;
				// Assign perfect foresight regression Model here
				 if (regressionModelMF[0].doubleValue() > 0.0 && regressionModelMF[1].doubleValue()> 0.0)
					 curveRandomMF[i].assignPerfectForesightRegressionModel(regressionModelMF);

				curveRandomMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName, Arrays.copyOf(
						trainingEventsArrayMF.get(thresholdStr), trainingEventsArrayMF.get(thresholdStr).length), null);
				curveRandomMF[i].estimateTraining(null);

				
				curveNoClassificationNoRegressionMF[i].filename = filename;
				curveNoClassificationNoRegressionMF[i].build(training, SELECTED_THRESHOLDS_MF[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayMF.get(thresholdStr),
								trainingEventsArrayMF.get(thresholdStr).length),
						null);
				curveNoClassificationNoRegressionMF[i].estimateTraining(null);

				thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[i]);
				Double regressionModelOlsen[] = new Double[1];
				regressionModelOlsen[0] = new Double(0.0);

				for (int thresholdCounter = 0; thresholdCounter < curvePerfectForesightOlsen.length; thresholdCounter++) {
					String thisThresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[i]);
					if (thisThresholdStr
							.equalsIgnoreCase(curvePerfectForesightOlsen[thresholdCounter].getThresholdString())) {
						regressionModelOlsen[0] = new Double(
								curvePerfectForesightOlsen[thresholdCounter].getSingleRatio());
						break;

					}
				}

				curveOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						null);
				curveOlsen[i].estimateTraining(null);

				curveClassificationOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				if (regressionModelOlsen[0].doubleValue() > 0.0)
					curveClassificationOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveClassificationOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						preprocessOlsen[i]);
				curveClassificationOlsen[i].estimateTraining(preprocessOlsen[i]);

				curveRandomOlsen[i].filename = filename;
				// Assign perfect foresight regression Model here
				// if (regressionModelOlsen[0].doubleValue() > 0.0)
				// curveRandomOlsen[i].assignPerfectForesightRegressionModel(regressionModelOlsen);

				curveRandomOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						null);
				curveRandomOlsen[i].estimateTraining(null);

				
				curveNoClassificationNoRegressionOlsen[i].filename = filename;
				curveNoClassificationNoRegressionOlsen[i].build(training, SELECTED_THRESHOLDS_OLSEN[i], gpFileName,
						Arrays.copyOf(trainingEventsArrayOlsen.get(thresholdStr),
								trainingEventsArrayOlsen.get(thresholdStr).length),
						null);
				curveNoClassificationNoRegressionOlsen[i].estimateTraining(null);

				
				// perfectForecastReturnMap from previous loop
				double perfectForcastTrainingReturn = perfectForecastReturnMap.get(SELECTED_THRESHOLDS[i]);
				double perfectForcastMFTrainingReturn = perfectForecastMFReturnMap.get(SELECTED_THRESHOLDS_MF[i]);
				double perfectForcastOlsenTrainingReturn = perfectForecastOlsenReturnMap
						.get(SELECTED_THRESHOLDS_OLSEN[i]);
				
				double curveCifreTrainingReturn = curveCifre[i].trainingTrading(null);
				double curveMFTrainingReturn = curveMF[i].trainingTrading(null);
				double curveOlsenTrainingReturn = curveOlsen[i].trainingTrading(null);
				
				double curveClassifcationTrainingReturn = curveClassifcation[i].trainingTrading(preprocess[i]);
				double curveClassifcationMFTrainingReturn = curveClassifcationMF[i].trainingTrading(preprocessMF[i]);
				double curveClassificationOlsenTrainingReturn = curveClassificationOlsen[i]
						.trainingTrading(preprocessOlsen[i]);
				
				
				
				double curveRandomTrainingReturn = curveRandomGP[i].trainingTrading(null);
				double curveRandomMFTrainingReturn = curveRandomMF[i].trainingTrading(null);
				double curveRandomOlsenTrainingReturn = curveRandomOlsen[i].trainingTrading(null);
				
				

				double curveClassificationNoRegressionTrainingReturn = curveNoClassificationNoRegressionGP[i]
						.trainingTrading(null);
				double curveClassificationNoRegressionMFTrainingReturn = curveNoClassificationNoRegressionMF[i]
						.trainingTrading(null);
				
				double curveClassificationNoRegressionOlsenTrainingReturn = curveNoClassificationNoRegressionOlsen[i]
						.trainingTrading(null);
		/*		
				SimpleTradingTraining = filename + " \t" + thresholdStr + " \t " + perfectForcastTrainingReturn + "\t"
						+ curveCifreTrainingReturn + "\t" + curveClassifcationTrainingReturn + "\t"
						+ curveRandomTrainingReturn + "\t" + curveClassificationNoRegressionTrainingReturn + "\t"
						+ perfectForcastMFTrainingReturn + "\t" + curveMFTrainingReturn + "\t"
						+ curveClassifcationMFTrainingReturn + "\t" + curveRandomMFTrainingReturn + "\t"
						+ curveClassificationNoRegressionMFTrainingReturn + "\t" + perfectForcastOlsenTrainingReturn
						+ "\t" + curveOlsenTrainingReturn + "\t" + curveClassificationOlsenTrainingReturn + "\t"
						+ curveRandomOlsenTrainingReturn + "\t" + curveClassificationNoRegressionOlsenTrainingReturn;
				Const.log.save("SimpleTradingTraining.txt", SimpleTradingTraining);
*/
				Const.log.save("autoWEKAClassifierListEvaluation.txt", preprocess[i].getAutoWEKAClassifierListEvalString());
				Const.log.save("autoWEKAClassifierListEvaluationMF.txt",
						preprocessMF[i].getAutoWEKAClassifierListEvalString());
				Const.log.save("autoWEKAClassifierListEvaluationOlsen.txt",
						preprocessOlsen[i].getAutoWEKAClassifierListEvalString());
				
				// curveClassifcation[i].preprocess.clearAutoWEKAClassifierListEvalString();
				// CGPReturnMap.put(thresholdStr,
				// curveClassifcationTrainingReturn);

				// cleanup
				String tempFolderName = preprocess[i].tempFilePath.get(0).substring(0,
						preprocess[i].tempFilePath.get(0).lastIndexOf(File.separator));

				File dir = new File(tempFolderName);
				if (!dir.isDirectory())
					continue;

				String fileNames = filename.substring(filename.lastIndexOf(File.separator) + 1, filename.length() - 4);

				File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith("autoweka" + fileNames);
					}
				});

				for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
					try {
						preprocess[i].deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
					} catch (IOException e) {

						System.out.println("Unable to delete one of the directory");
					}
				}

			} // for (int i = 0; i < SELECTED_THRESHOLDS.length; i++) {

			// TODO Do above loop for each regregression algo

			// reset currentProcessorCounter
			currentProcessorCounter = 0;
			for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS.length; testBuildCount++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[testBuildCount]);

				String gpFileNamePrefix = gpDaysMap.get(TrainingDay); // This is
																		// just
																		// to
																		// get
																		// folder
																		// locations
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS[testBuildCount]);

				Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
						dCEventGenerator.getEvents().length);

				testEventsArray.put(thresholdStr, copiedTestArray);

				preprocess[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocess[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);
				preprocess[testBuildCount].processTestData(copiedTestArray);

				preprocess[testBuildCount].loadTestData(copiedTestArray);

				preprocess[testBuildCount].classifyTestData();

				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS[testBuildCount]);

				curvePerfectForesight[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, null);

				curveCifre[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS[testBuildCount],
						copiedTestArray, null);

				curveClassifcation[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, preprocess[testBuildCount]);

				
				curveRandomGP[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS[testBuildCount],
						copiedTestArray, null);

				
				curveNoClassificationNoRegressionGP[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS[testBuildCount], copiedTestArray, null);

				currentProcessorCounter = testBuildCount;
			} // testing GP

			currentProcessorCounter = 0;
			for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS_MF.length; testBuildCount++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[testBuildCount]);

				String gpFileNamePrefix = gpDaysMap.get(TrainingDay); // This is
																		// just
																		// to
																		// get
																		// folder
																		// locations
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS_MF[testBuildCount]);

				Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
						dCEventGenerator.getEvents().length);

				testEventsArrayMF.put(thresholdStr, copiedTestArray);

				preprocessMF[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocessMF[testBuildCount].buildTest(copiedTestArray);
				System.out.println("About to print test data for threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);
				preprocessMF[testBuildCount].processTestData(copiedTestArray);

				preprocessMF[testBuildCount].loadTestData(copiedTestArray);

				preprocessMF[testBuildCount].classifyTestData();

				// testInstance =
				// preprocess[testBuildCount].getCopyOfTestInstances();

				System.out
						.println("About to print test data for MF threshold " + SELECTED_THRESHOLDS_MF[testBuildCount]);

				curveMF[testBuildCount].testbuild(training.length, this.test, SELECTED_THRESHOLDS_MF[testBuildCount],
						copiedTestArray, null);

				curveClassifcationMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, preprocessMF[testBuildCount]);

				curvePerfectForesightMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, null);

				curveRandomMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, null);

				
				curveNoClassificationNoRegressionMF[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_MF[testBuildCount], copiedTestArray, null);

				currentProcessorCounter = testBuildCount;
			} // testing MF

			currentProcessorCounter = 0;
			for (int testBuildCount = 0; testBuildCount < SELECTED_THRESHOLDS_OLSEN.length; testBuildCount++) {
				String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[testBuildCount]);

				String gpFileNamePrefix = gpDaysMap.get(TrainingDay);
				DCEventGenerator dCEventGenerator = new DCEventGenerator();
				dCEventGenerator.generateEvents(this.test, SELECTED_THRESHOLDS_OLSEN[testBuildCount]);

				Event[] copiedTestArray = Arrays.copyOf(dCEventGenerator.getEvents(),
						dCEventGenerator.getEvents().length);

				testEventsArrayOlsen.put(thresholdStr, copiedTestArray);

				preprocessOlsen[testBuildCount].lastTestingEvent = copiedTestArray[copiedTestArray.length - 1];

				preprocessOlsen[testBuildCount].buildTest(copiedTestArray);
				System.out.println(
						"About to print test data for Olsen threshold " + SELECTED_THRESHOLDS_OLSEN[testBuildCount]);
				preprocessOlsen[testBuildCount].processTestData(copiedTestArray);

				preprocessOlsen[testBuildCount].loadTestData(copiedTestArray);

				preprocessOlsen[testBuildCount].classifyTestData();

				curveOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, null);

				curveClassificationOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, preprocessOlsen[testBuildCount]);

				curvePerfectForesightOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, null);

				curveRandomOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, null);

				
				curveNoClassificationNoRegressionOlsen[testBuildCount].testbuild(training.length, this.test,
						SELECTED_THRESHOLDS_OLSEN[testBuildCount], copiedTestArray, null);

				currentProcessorCounter = testBuildCount;
			} // testing Olsen

			currentProcessorCounter = 0;
			
			currentProcessorCounter = 0;
			

			String regressionresult = "Dataset  \t GP  \t Classifier+GP  \t MF \t Classifier+MF  \t Olsen \t  Classifier+Olsen ";
			System.out.println(regressionresult);
			FWriter writer = new FWriter(Const.log.publicFolder + "RegressionAnalysisCurves.txt");
			Const.log.save("RegressionAnalysisCurves.txt", regressionresult);

			String classificationresult = "Dataset \t Threshold \t TotalMissedOvershoot \t TotalMissedOvershootLength \t TotalAssumedOvershoot \t PossibleOvershoot \t FoundOvershootLength \t TotalFoundOvershoot  \t totalDcEvent \t testAccuracy \t testPrecision \t testRecall";

			FWriter Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysis.txt");
			Const.log.save("ClassificationAnalysis.txt", classificationresult);

			Classicationwriter = new FWriter(Const.log.publicFolder + "ClassificationAnalysisMF.txt");
			Const.log.save("ClassificationAnalysisMF.txt", classificationresult);

			Classicationwriter = new FWriter(Const.log.publicFolder + "ClassificationAnalysisOlsen.txt");
			Const.log.save("ClassificationAnalysisOlsen.txt", classificationresult);

			
			
			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisTraining.txt");
			Const.log.save("ClassificationAnalysisTraining.txt", classificationresult);

			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisTraining.txt");
			Const.log.save("ClassificationAnalysisTrainingMF.txt", classificationresult);

			Classicationwriter = new FWriter(
					Const.log.publicFolder + "ClassificationAnalysisTraining.txt");
			Const.log.save("ClassificationAnalysisTrainingOlsen.txt", classificationresult);

			
			String SimpleTradingResult = "Dataset \t  GP \t Classifier+GP  \t "
					+ "Random+GP \t GP+DCC"
					+ " \t MF  \t Classifier+MF \t Random+MF \t MF+DCC  \t "
					+ " Olsen \t Classifier+Olsen \t Random+Olsen \t Olsen+DCC  ";
			FWriter SimpleTradingResultWriter = new FWriter(
					Const.log.publicFolder + "SimpleTradingResult.txt");
			Const.log.save("SimpleTradingResult.txt", SimpleTradingResult);

			FWriter sharpRatioWriter = new FWriter(Const.log.publicFolder + "sharpRatio.txt");
			Const.log.save("SharpRatio.txt", SimpleTradingResult);

			String mDD = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  "
					+ "\t Random+GP \t GP+DCC \t"
					+ " PerfectForesight+MF \t MF  \t Classifier+MF \t"
					+ " Random+MF \t MF+DCC  "
					+ "\t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen "
					+ "\t Random+Olsen \t Olsen+DCC  "
					+ "\t PerfectForesight+H  \t H \t Classifier+H \t"
					+ " Random+H \t H+DCC \t H+DCC_trail  \t "
					+ "PerfectForesight+HSplitDCUp_Down \t HSplitDCUp_Down \t Classifier+HSplitDCUp_Down \t "
					+ "Random+HSplitDCUp_Down \t HSplitDCUp_Down+DCC \t HSplitDCUp_Down+DCC_trail "
					+ "\t rsi \t ema \t macd  ";
			FWriter MDDWriter = new FWriter(Const.log.publicFolder + "mddBaseCcy.txt");
			Const.log.save("mddBaseCcy.txt", mDD);

			FWriter MDDWriterQuote = new FWriter(Const.log.publicFolder + "mddQuoteCcy.txt");
			Const.log.save("mddQuoteCcy.txt", mDD);

			String profit = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t Random+GP \t GP+DCC \t PerfectForesight+MF \t MF  \t Classifier+MF \t Random+MF \t MF+DCC  \t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen \t Random+Olsen \t Olsen+DCC  ";
			FWriter profitWriter = new FWriter(Const.log.publicFolder + "BaseCCyProfit.txt");
			Const.log.save("BaseCCyProfit.txt", profit);

			FWriter profitWriterQuote = new FWriter(Const.log.publicFolder + "QuoteCCyProfit.txt");
			Const.log.save("QuoteCCyProfit.txt", profit);

			String transactions = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t Random+GP \t GP+DCC \t PerfectForesight+MF \t MF  \t Classifier+MF \t Random+MF \t MF+DCC  \t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen \t Random+Olsen \t Olsen+DCC  ";
			FWriter NumberOfBaseCCyTransactionWriter = new FWriter(
					Const.log.publicFolder + "NumberOfBaseCCyTransaction.txt");
			Const.log.save("NumberOfBaseCCyTransaction.txt", transactions);

			FWriter NumberOfQuoteCCyTransactionWriter = new FWriter(
					Const.log.publicFolder + "NumberOfQuoteCCyTransaction.txt");
			Const.log.save("NumberOfQuoteCCyTransaction.txt", transactions);

			String trendCollection = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t Random+GP \t GP+DCC \t PerfectForesight+MF \t MF  \t Classifier+MF \t Random+MF \t MF+DCC  \t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen \t Random+Olsen \t Olsen+DCC  ";
			FWriter actualTrendWriter = new FWriter(Const.log.publicFolder + "actualTrend.txt");
			Const.log.save("actualTrend.txt", trendCollection);

			FWriter predictedTrendWriter = new FWriter(
					Const.log.publicFolder + "predictedTrendString.txt");
			Const.log.save("predictedTrendString.txt", trendCollection);

			String DCAndTaTradingResult = "Dataset \t PerfectForesight+GP \t GP \t Classifier+GP  \t Random+GP \t GP+DCC \t PerfectForesight+MF \t MF  \t Classifier+MF \t Random+MF \t MF+DCC  \t PerfectForesight+Olsen \t Olsen \t Classifier+Olsen \t Random+Olsen \t Olsen+DCC  ";
			FWriter DCAndTaTradingResultWriter = new FWriter(
					Const.log.publicFolder + "DCAndTaTradingResult.txt");
			Const.log.save("DCAndTaTradingResult.txt", DCAndTaTradingResult);

			// reset currentProcessorCounter
			currentProcessorCounter = 0;
			double perfectForesightTradeResult = 0.0;
			double gPTradeResult = 0.0;
			double classificationAndGpTradeResult = 0.0;
			double randomAndGpTradeResult = 0.0;
			
			double ddcOnlyGPTradeResult = 0.0;
			
		
			double perfectForesightMFTradeResult = 0.0;
			double mFTradeResult = 0.0;
			double classificationAndMFTradeResult = 0.0;
			double randomAndMFTradeResult = 0.0;
			double ddcOnlyMFTradeResult = 0.0;

			double perfectForesightOlsenTradeResult = 0.0;
			double olsenTradeResult = 0.0;
			double classificationAndOlsenTradeResult = 0.0;
			double randomAndOlsenTradeResult = 0.0;
			double ddcOnlyOlsenTradeResult = 0.0;

			
			

			double rsiTechnicalAnalysisTradeResult = 0.0;
			double emaTechnicalAnalysisTradeResult = 0.0;
			double macdTechnicalAnalysisTradeResult = 0.0;

			for (int reportCount = 0; reportCount < SELECTED_THRESHOLDS.length; reportCount++) {
				// String thresholdStr = String.format("%.8f",
				// SELECTED_THRESHOLDS[reportCount]);
				String gpFileNamePrefix = gpDaysMap.get(TrainingDay);
				// System.out.println("RUN:: " + gpFileNamePrefix);
				String gpFileName = ""; // no longer relevant

				// Report RMSE
				testArrayPerfectForesight[reportCount] = curvePerfectForesight[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				testArrayCifre[reportCount] = curveCifre[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				testArrayClassification[reportCount] = curveClassifcation[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				testArrayRandom[reportCount] = curveRandomGP[reportCount].report(this.test,
						SELECTED_THRESHOLDS[reportCount], gpFileName);
				
				testArrayClassificationNoRegressionGP[reportCount] = curveNoClassificationNoRegressionGP[reportCount]
						.report(this.test, SELECTED_THRESHOLDS[reportCount], gpFileName);

				testArrayPerfectForesightMF[reportCount] = curvePerfectForesightMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				testArrayMF[reportCount] = curveMF[reportCount].report(this.test, SELECTED_THRESHOLDS_MF[reportCount],
						gpFileName);
				testArrayClassificationMF[reportCount] = curveClassifcationMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
				testArrayRandomMF[reportCount] = curveRandomMF[reportCount].report(this.test,
						SELECTED_THRESHOLDS_MF[reportCount], gpFileName);
			
				testArrayClassificationNoRegressionMF[reportCount] = curveNoClassificationNoRegressionMF[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_MF[reportCount], gpFileName);

				testArrayPerfectForesightOlsen[reportCount] = curvePerfectForesightOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				testArrayOlsen[reportCount] = curveOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				testArrayClassificationOlsen[reportCount] = curveClassificationOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				testArrayRandomOlsen[reportCount] = curveRandomOlsen[reportCount].report(this.test,
						SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);
				
				testArrayClassificationNoRegressionOlsen[reportCount] = curveNoClassificationNoRegressionOlsen[reportCount]
						.report(this.test, SELECTED_THRESHOLDS_OLSEN[reportCount], gpFileName);

				
				/*
				 * Start because we use different threshold for each algo we
				 * need to get seperated distribution for each regression
				 * algorithm compared
				 */
				curveCifre[reportCount].TrainingDCEventOSEventDistribution();
				curveCifre[reportCount].TestDCEventOSEventDistribution();

				curveMF[reportCount].TrainingDCEventOSEventDistribution();
				curveMF[reportCount].TestDCEventOSEventDistribution();

				curveOlsen[reportCount].TrainingDCEventOSEventDistribution();
				curveOlsen[reportCount].TestDCEventOSEventDistribution();

				
				/*
				 * End
				 */

				selectedThresholds = filename + "\t" + String.format("%.8f", SELECTED_THRESHOLDS[reportCount]) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].zeroPercentageTest) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].dC_OS_Length_RatioTest) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_MF[reportCount]) + "\t"
						+ String.format("%.8f", curveMF[reportCount].zeroPercentageTest) + "\t"
						+ String.format("%.8f", curveMF[reportCount].dC_OS_Length_RatioTest) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[reportCount]) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].zeroPercentageTest) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].dC_OS_Length_RatioTest);

				Const.log.save("selectedThresholdsTest.txt", selectedThresholds);

				selectedThresholds = filename + "\t" + String.format("%.8f", SELECTED_THRESHOLDS[reportCount]) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].zeroPercentageTraining) + "\t"
						+ String.format("%.8f", curveCifre[reportCount].dC_OS_Length_RatioTraining) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_MF[reportCount]) + "\t"
						+ String.format("%.8f", curveMF[reportCount].zeroPercentageTraining) + "\t"
						+ String.format("%.8f", curveMF[reportCount].dC_OS_Length_RatioTraining) + "\t"
						+ String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[reportCount]) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].zeroPercentageTraining) + "\t"
						+ String.format("%.8f", curveOlsen[reportCount].dC_OS_Length_RatioTraining);

				Const.log.save("selectedThresholdsTraining.txt", selectedThresholds);

				Locale locale = new Locale("en", "UK");
				String pattern = "##.##########";

				DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getNumberInstance(locale);
				decimalFormat.applyPattern(pattern);

				String formatPerfectForesight = decimalFormat
						.format(Double.parseDouble(testArrayPerfectForesight[reportCount]));
				String formatCifre = decimalFormat.format(Double.parseDouble(testArrayCifre[reportCount]));
				String formatClassifier = decimalFormat
						.format(Double.parseDouble(testArrayClassification[reportCount]));
				String formatRandom = decimalFormat.format(Double.parseDouble(testArrayRandom[reportCount]));
				/*
				 * Skipping testArrayDCCOnlyAndTrailGP[reportCount]
				 * testArrayClassificationNoRegressionGP[reportCount];
				 */

				String formatPerfectForesightMF = decimalFormat
						.format(Double.parseDouble(testArrayPerfectForesightMF[reportCount]));
				String formatMF = decimalFormat.format(Double.parseDouble(testArrayMF[reportCount]));
				String formatClassifierMF = decimalFormat
						.format(Double.parseDouble(testArrayClassificationMF[reportCount]));
				String formatRandomMF = decimalFormat.format(Double.parseDouble(testArrayRandomMF[reportCount]));
				/*
				 * Skipping testArrayDCCOnlyAndTrailMF[reportCount]
				 * testArrayClassificationNoRegressionMF[reportCount];
				 */

				String formatPerfectForesightOlsen = decimalFormat
						.format(Double.parseDouble(testArrayPerfectForesightOlsen[reportCount]));
				String formatOlsen = decimalFormat.format(Double.parseDouble(testArrayOlsen[reportCount]));
				String formatClassifierOlsen = decimalFormat
						.format(Double.parseDouble(testArrayClassificationOlsen[reportCount]));
				String formatRandomOlsen = decimalFormat.format(Double.parseDouble(testArrayRandomOlsen[reportCount]));
				

				

				regressionresult = filename + "\t" +  formatCifre + "\t"
						+ formatClassifier + "\t"  + formatMF  + "\t"
						+ "\t" + formatClassifierMF + "\t"  
						+ formatOlsen + "\t" + formatClassifierOlsen  ;

				Const.log.save("RegressionAnalysisCurves.txt", regressionresult);

				double pFTrade = curvePerfectForesight[reportCount].trade(null);
				double gPTrade = curveCifre[reportCount].trade(null);
				double classificationAndGpTrade = curveClassifcation[reportCount].trade(preprocess[reportCount]);
				double randomTrade = curveRandomGP[reportCount].trade(null);
				double noClassifierNoRegressionGPTrade = curveNoClassificationNoRegressionGP[reportCount].trade(null);
				
				
				double perfectForesightMFTrade = curvePerfectForesightMF[reportCount].trade(null);
				double mFTrade = curveMF[reportCount].trade(null);
				double classificationAndMFTrade = curveClassifcationMF[reportCount].trade(preprocessMF[reportCount]);
				double randomTradeMF = curveRandomMF[reportCount].trade(null);
				
				double noClassifierNoRegressionMFTrade = curveNoClassificationNoRegressionMF[reportCount].trade(null);

				double perfectForesightOlsenTrade = curvePerfectForesightOlsen[reportCount].trade(null);
				double olsenTrade = curveOlsen[reportCount].trade(null);
				double classificationAndOlsenTrade = curveClassificationOlsen[reportCount]
						.trade(preprocessOlsen[reportCount]);
				double randomTradeOlsen = curveRandomOlsen[reportCount].trade(null);
				
				double noClassifierNoRegressionOlsenTrade = curveNoClassificationNoRegressionOlsen[reportCount]
						.trade(null);



				// let's sum up the provit from selected thresholds
				perfectForesightTradeResult = perfectForesightTradeResult + pFTrade;
				gPTradeResult = gPTradeResult + gPTrade;
				classificationAndGpTradeResult = classificationAndGpTradeResult + classificationAndGpTrade;
				randomAndGpTradeResult = randomAndGpTradeResult + randomTrade;
				
				ddcOnlyGPTradeResult = ddcOnlyGPTradeResult + noClassifierNoRegressionGPTrade;

				
				
				perfectForesightMFTradeResult = perfectForesightMFTradeResult + perfectForesightMFTrade;
				mFTradeResult = mFTradeResult + mFTrade;
				classificationAndMFTradeResult = classificationAndMFTradeResult + classificationAndMFTrade;
				randomAndMFTradeResult = randomAndMFTradeResult + randomTradeMF;
				
				ddcOnlyMFTradeResult = ddcOnlyMFTradeResult + noClassifierNoRegressionMFTrade;

				perfectForesightOlsenTradeResult = perfectForesightOlsenTradeResult + perfectForesightOlsenTrade;
				olsenTradeResult = olsenTradeResult + olsenTrade;
				classificationAndOlsenTradeResult = classificationAndOlsenTradeResult + classificationAndOlsenTrade;
				randomAndOlsenTradeResult = randomAndOlsenTradeResult + randomTradeOlsen;
				
				ddcOnlyOlsenTradeResult = ddcOnlyOlsenTradeResult + noClassifierNoRegressionOlsenTrade;

				

				rsiTechnicalAnalysisTradeResult = rsiTradeResult;
				emaTechnicalAnalysisTradeResult = emaTradeResult;
				macdTechnicalAnalysisTradeResult = mcadTradeResult;

				SimpleTradingResult = filename + "\t" + gPTrade + "\t" + classificationAndGpTrade
						+ "\t" + randomTrade + "\t" + noClassifierNoRegressionGPTrade + "\t" 
						+ mFTrade + "\t" + classificationAndMFTrade + "\t"
						+ randomTradeMF + "\t" + noClassifierNoRegressionMFTrade + "\t" 
						+ olsenTrade + "\t" + classificationAndOlsenTrade + "\t"
						+ randomTradeOlsen + "\t" + noClassifierNoRegressionOlsenTrade;

				Const.log.save("SimpleTradingResult.txt", SimpleTradingResult);

				// TODO continue here for DCConly and DCCAnd trail

				profit = filename + " \t" + +curvePerfectForesight[reportCount].getBaseCCyProfit() + "\t"
						+ curveCifre[reportCount].getBaseCCyProfit() + "\t"
						+ curveClassifcation[reportCount].getBaseCCyProfit() + "\t"
						+ curveRandomGP[reportCount].getBaseCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getBaseCCyProfit() + "\t"
						
						+ curveMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveClassifcationMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveRandomMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getBaseCCyProfit() + "\t"
						+ curveOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveClassificationOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveRandomOlsen[reportCount].getBaseCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getBaseCCyProfit() + "\t";
				Const.log.save("BaseCCyProfit.txt", profit);

				profit = filename + " \t" + curvePerfectForesight[reportCount].getQuoteCCyProfit() + "\t"
						+ curveCifre[reportCount].getQuoteCCyProfit() + "\t"
						+ curveClassifcation[reportCount].getQuoteCCyProfit() + "\t"
						+ curveRandomGP[reportCount].getQuoteCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getQuoteCCyProfit() + "\t"
						+ curveMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveRandomMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveClassifcationMF[reportCount].getQuoteCCyProfit() + "\t"
						+ curveOlsen[reportCount].getQuoteCCyProfit() + "\t"
						+ curveClassificationOlsen[reportCount].getQuoteCCyProfit() + "\t"
						+ curveRandomOlsen[reportCount].getQuoteCCyProfit() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getQuoteCCyProfit() ;
				Const.log.save("QuoteCCyProfit.txt", profit);

				profit = filename + " \t" + curvePerfectForesight[reportCount].getSharpRatio() + "\t"
						+ curveCifre[reportCount].getSharpRatio() + "\t"
						+ curveClassifcation[reportCount].getSharpRatio() + "\t"
						+ curveRandomGP[reportCount].getSharpRatio() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getSharpRatio() + "\t"
						
						+ curveMF[reportCount].getSharpRatio()
						+ "\t" + curveClassifcationMF[reportCount].getSharpRatio() + "\t"
						+ curveRandomMF[reportCount].getSharpRatio() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getSharpRatio() + "\t"
						+ curveOlsen[reportCount].getSharpRatio() + "\t"
						+ curveClassificationOlsen[reportCount].getSharpRatio() + "\t"
						+ curveRandomOlsen[reportCount].getSharpRatio() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getSharpRatio() ;
				Const.log.save("SharpRatio.txt", profit);

				if (preprocess[reportCount] != null && preprocessMF[reportCount] != null
						&& preprocessOlsen[reportCount] != null ) {

					String thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS[reportCount]);
					String classificationResult = preprocess[reportCount]
							.printPreprocessClassification(testEventsArray.get(thresholdStr));

					Const.log.save("ClassificationAnalysis.txt", classificationResult);

					classificationResult = preprocess[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisTraining.txt", classificationResult);

					thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_MF[reportCount]);
					String classificationResultMF = preprocessMF[reportCount]
							.printPreprocessClassification(testEventsArrayMF.get(thresholdStr));
					Const.log.save("ClassificationAnalysisMF.txt", classificationResultMF);

					classificationResult = preprocessMF[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArray.get(thresholdStr));
					Const.log.save("ClassificationAnalysisTrainingMF.txt", classificationResult);

					thresholdStr = String.format("%.8f", SELECTED_THRESHOLDS_OLSEN[reportCount]);
					String classificationResultOlsen = filename + "\t" + preprocessOlsen[reportCount]
							.printPreprocessClassification(testEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisOlsen.txt", classificationResultOlsen);

					classificationResult = preprocessOlsen[reportCount]
							.printPreprocessClassificationTraining(trainingEventsArrayOlsen.get(thresholdStr));
					Const.log.save("ClassificationAnalysisTrainingOlsen.txt", classificationResult);

					
					

				}

			
				
				mDD = filename + " \t" + curvePerfectForesight[reportCount].getMaxMddBase() + "\t"
						+ curveCifre[reportCount].getMaxMddBase() + "\t"
						+ curveClassifcation[reportCount].getMaxMddBase() + "\t"
						+ curveRandomGP[reportCount].getMaxMddBase() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getMaxMddBase() + "\t"
						
						+ curvePerfectForesightMF[reportCount].getMaxMddBase() + "\t"
						+ curveMF[reportCount].getMaxMddBase() + "\t"
						+ curveClassifcationMF[reportCount].getMaxMddBase() + "\t"
						+ curveRandomMF[reportCount].getMaxMddBase() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getMaxMddBase() + "\t"
						
						+ curvePerfectForesightOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveClassificationOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveRandomOlsen[reportCount].getMaxMddBase() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getMaxMddBase() ;
						
				Const.log.save("mddBaseCcy.txt", mDD);

				mDD = filename + " \t" + curvePerfectForesight[reportCount].getMaxMddQuote() + "\t"
						+ curveCifre[reportCount].getMaxMddQuote() + "\t"
						+ curveClassifcation[reportCount].getMaxMddQuote() + "\t"
						+ curveRandomGP[reportCount].getMaxMddQuote() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getMaxMddQuote() + "\t"
						
						+ curvePerfectForesightMF[reportCount].getMaxMddQuote() + "\t"
						+ curveMF[reportCount].getMaxMddQuote() + "\t"
						+ curveClassifcationMF[reportCount].getMaxMddQuote() + "\t"
						+ curveRandomMF[reportCount].getMaxMddBase() + "\t"
						+ curvePerfectForesightOlsen[reportCount].getMaxMddQuote() + "\t"
						+ curveOlsen[reportCount].getMaxMddQuote() + "\t"
						+ curveClassificationOlsen[reportCount].getMaxMddQuote() + "\t"
						+ curveRandomOlsen[reportCount].getMaxMddBase() + "\t"
						+ emaTrading.getMaxMddQuote() + "\t" + mcadTrading.getMaxMddQuote();
				Const.log.save("mddQuoteCcy.txt", mDD);

				transactions = filename + " \t" + +curvePerfectForesight[reportCount].getNumberOfQuoteCcyTransactions()
						+ "\t" + curveCifre[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveClassifcation[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveRandomGP[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curvePerfectForesightMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveClassifcationMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveRandomMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curvePerfectForesightOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveClassificationOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveRandomOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getNumberOfQuoteCcyTransactions() + "\t"
						+ rsiTrading.getNumberOfQuoteCcyTransactions() + "\t"
						+ emaTrading.getNumberOfQuoteCcyTransactions() + "\t"
						+ mcadTrading.getNumberOfQuoteCcyTransactions();
				Const.log.save("NumberOfQuoteCCyTransaction.txt", transactions);

				transactions = filename + " \t" + +curvePerfectForesight[reportCount].getNumberOfBaseCcyTransactions()
						+ "\t" + curveCifre[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveClassifcation[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveRandomGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curvePerfectForesightMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveClassifcationMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveRandomMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curvePerfectForesightOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveClassificationOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveRandomOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
						+ rsiTrading.getNumberOfBaseCcyTransactions() + "\t"
						+ emaTrading.getNumberOfBaseCcyTransactions() + "\t"
						+ mcadTrading.getNumberOfBaseCcyTransactions();
				Const.log.save("NumberOfBaseCCyTransaction.txt", transactions);

				if (preprocess[reportCount] != null)
					preprocess[reportCount].removeTempFiles();

				String tempFolderName = preprocess[reportCount].tempFilePath.get(0).substring(0,
						preprocess[reportCount].tempFilePath.get(0).lastIndexOf(File.separator));

				File dir = new File(tempFolderName);
				if (!dir.isDirectory())
					continue;

				File[] tempFile = dir.getParentFile().listFiles(new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.startsWith("autoweka" + filename);
					}
				});

				for (int tempFileCount = 0; tempFileCount < tempFile.length; tempFileCount++) {
					try {
						preprocess[reportCount].deleteDirectoryRecursionJava6(tempFile[tempFileCount]);
					} catch (IOException e) {

						System.out.println("Unable to delete one of the directory");
					}
				}
				currentProcessorCounter = reportCount;

				int maxTransaction = -1;

				maxTransaction = Math.max(curvePerfectForesight[reportCount].getMaxTransactionSize(),
						curveClassifcation[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveCifre[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveRandomGP[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveNoClassificationNoRegressionGP[reportCount].getMaxTransactionSize());
				
				maxTransaction = Math.max(maxTransaction, curvePerfectForesightMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveClassifcationMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveRandomMF[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveNoClassificationNoRegressionMF[reportCount].getMaxTransactionSize());

				maxTransaction = Math.max(maxTransaction,curvePerfectForesightOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction,curveClassificationOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveRandomOlsen[reportCount].getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, curveNoClassificationNoRegressionOlsen[reportCount].getMaxTransactionSize());


				maxTransaction = Math.max(maxTransaction, rsiTrading.getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, mcadTrading.getMaxTransactionSize());
				maxTransaction = Math.max(maxTransaction, emaTrading.getMaxTransactionSize());

				rsiTrading.getOrders();
				mcadTrading.getOrders();
				emaTrading.getOrders();

				profit = "Dataset \t PerfectForesight+GP \t GP  \t Classifier+GP \t Random+GP \t PerfectForesight+MF \t MF \t Classifier+MF \t Random+MF \t PerfectForesight+Olsen \t Olsen \t  Classifier+Olsen \t Random+Olsen \t PerfectForesight+H \t H \t Classifier+H \t Random+H \t PerfectForesight+HSplitDCUp_Down \t HSplitDCUp_Down\t Classifier+HSplitDCUp_Down \t Random+HSplitDCUp_Down \t rsi \t ema \t macd  ";
				FWriter TransactionWriter = new FWriter(Const.log.publicFolder + "TransactionCount.txt");
				Const.log.save("TransactionCount.txt", profit);

				for (int k = 0; k < maxTransaction; k++) {

					profit = filename + " \t" + curvePerfectForesight[reportCount].getTransanction(k) + "\t"
							+ curveCifre[reportCount].getTransanction(k) + "\t"
							+ curveClassifcation[reportCount].getTransanction(k) + "\t"
							+ curveRandomGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveNoClassificationNoRegressionGP[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curvePerfectForesightMF[reportCount].getTransanction(k) + "\t"
							+ curveMF[reportCount].getTransanction(k) + "\t"
							+ curveClassifcationMF[reportCount].getTransanction(k) + "\t"
							+ curveRandomMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveNoClassificationNoRegressionMF[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curvePerfectForesightOlsen[reportCount].getTransanction(k) + "\t"
							+ curveOlsen[reportCount].getTransanction(k) + "\t"
							+ curveClassificationOlsen[reportCount].getTransanction(k) + "\t"
							+ curveRandomOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ curveNoClassificationNoRegressionOlsen[reportCount].getNumberOfBaseCcyTransactions() + "\t"
							+ rsiTrading.getTransanction(k) + "\t" + emaTrading.getTransanction(k) + "\t"
							+ mcadTrading.getTransanction(k);
					Const.log.save("TransactionCount.txt", profit);

				}

				profit = "Dataset \t PerfectForesight+GP \t GP  \t Classifier+GP \t Random+GP \t PerfectForesight+MF \t MF \t Classifier+MF \t Random+MF \t PerfectForesight+Olsen \t Olsen \t  Classifier+Olsen \t Random+Olsen \t PerfectForesight+H \t H \t Classifier+H \t Random+H \t PerfectForesight+HSplitDCUp_Down \t HSplitDCUp_Down\t Classifier+HSplitDCUp_Down \t Random+HSplitDCUp_Down \t rsi \t ema \t macd  ";
				FWriter stdDevWriter = new FWriter(Const.log.publicFolder + "stdDev.txt");
				Const.log.save("stdDev.txt", profit);

				profit = filename + " \t" + curvePerfectForesight[reportCount].calculateSD() + "\t"
						+ curveCifre[reportCount].calculateSD() + "\t" 
						+ curveClassifcation[reportCount].calculateSD() + "\t" 
						+ curveRandomGP[reportCount].calculateSD() + "\t"
						+ curveNoClassificationNoRegressionGP[reportCount].calculateSD() + "\t"
						+ curvePerfectForesightMF[reportCount].calculateSD() + "\t" 
						+ curveMF[reportCount].calculateSD() + "\t" 
						+ curveClassifcationMF[reportCount].calculateSD() + "\t"
						+ curveRandomMF[reportCount].calculateSD() + "\t"
						+ curveNoClassificationNoRegressionMF[reportCount].calculateSD() + "\t"
						+ curvePerfectForesightOlsen[reportCount].calculateSD() + "\t"
						+ curveOlsen[reportCount].calculateSD() + "\t"
						+ curveClassificationOlsen[reportCount].calculateSD() + "\t"
						+ curveRandomOlsen[reportCount].calculateSD() + "\t"
						+ curveNoClassificationNoRegressionOlsen[reportCount].calculateSD() + "\t"
						+ "\t" + rsiTrading.calculateSD() + "\t"
						+ emaTrading.calculateSD() + "\t" + mcadTrading.calculateSD();

				Const.log.save("stdDev.txt", profit);
			}
		}

	}

	public static void main(String[] args) throws Exception {

		/*
		 * File dir = new File("c:\\Users\\atna3\\AppData\\Local\\Temp"); File[]
		 * foundFiles = dir.listFiles(new FilenameFilter() { public boolean
		 * accept(File dir, String name) { //System.out.println(
		 * name.startsWith("autoweka")); return name.startsWith("autoweka"); }
		 * });
		 * 
		 * for (int k =0; k< foundFiles.length; k++){
		 * System.out.println(foundFiles[k].toString());
		 * Runtime.getRuntime().exec(
		 * "cmd.exe /c for /d %x in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do rd /s /q \"%x\" "
		 * ); }
		 */
		// Split the long parameter file , according to the delimiter
		String s[] = args[0].split(":");
		if (s.length < 6) {
			System.out.println(
					"Expect 6 parameters: <file path:file name:training index start:training index end:test index start:test index end>");
			System.exit(1);
		}

		if (s[6] != null) {
			switch (Const.hashFunctionType(s[6])) {
			case eMichaelFernando:
				Const.OsFunctionEnum = Const.function_code.eMichaelFernando;
				break;
			case eGP:
				Const.OsFunctionEnum = Const.function_code.eGP;
				break;
			default:
				Const.OsFunctionEnum = Const.function_code.eMichaelFernando;
				break;
			}
		} else {
			Const.OsFunctionEnum = Const.function_code.eMichaelFernando;
		}

		try {
			if (s[7] != null) {
				try {
					if (s[7].compareToIgnoreCase("TRUE") == 0)
						Const.splitDatasetByTrendType = true;
				} catch (NumberFormatException nfe) {
					Const.splitDatasetByTrendType = false;
				}
			}
		} catch (ArrayIndexOutOfBoundsException aie) {
			Const.splitDatasetByTrendType = false;
		}

		if (s[8].compareToIgnoreCase("TRUE") == 0) {
			Const.LINEAR_FUNCTIONALITY_ONLY = true;
			Const.FUNCTION_NODE_DEFINITION = "LINEAR";
			// System.out.println("LINEAR_FUNCTIONALITY_ONLY is TRUE");
		} else {
			// System.out.println("LINEAR_FUNCTIONALITY_ONLY is FALSE");
			Const.FUNCTION_NODE_DEFINITION = "NON_LINEAR";
			Const.LINEAR_FUNCTIONALITY_ONLY = false;
		}
		Const.NEGATIVE_EXPRESSION_REPLACEMENT = Integer.parseInt(s[9]);

		try {
			if (s[10] != null) {
				try {
					Const.NUM_OF_PROCESSORS = Integer.parseInt(s[10]);
				} catch (NumberFormatException nfe) {
					Const.NUM_OF_PROCESSORS = 5;
				}
			}
		} catch (ArrayIndexOutOfBoundsException aie) {
			Const.NUM_OF_PROCESSORS = 5;
		}

		if (s[11].compareToIgnoreCase("TRUE") == 0) {
			Const.REUSE_EXISTING_TREE = true;
			// System.out.println("RECREATE_TREE will be recreated");
		} else {
			// System.out.println("RECREATE_TREE will not be recreated if
			// found");
			Const.REUSE_EXISTING_TREE = false;
		}

		double test = 0.0;

		if (s.length >= 12) {
			switch (Const.hashtreeStructurePostcreateType(s[12])) {
			case ePruneAndEqualERCAndExternalInputLeaf:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePruneAndEqualERCAndExternalInputLeaf;
				break;
			case ePrune:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.ePrune;
				break;
			case eEqualERCAndExternalInputLeaf:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eEqualERCAndExternalInputLeaf;
				break;
			case eRandom:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eRandom;
				break;
			default:
				TreeHelperClass.treeStructurePostcreateObj = treeStructurePostcreate.eRandom;
				break;
			}
		}

		Const.log = new Logger(s[1], s[3], s[4]);

		// TreeHelperClass.treeStructurePostcreateObj

		SymbolicRegression ga = new SymbolicRegression(s[0], Integer.parseInt(s[2]), Integer.parseInt(s[3]),
				Integer.parseInt(s[4]), Integer.parseInt(s[5]), Integer.parseInt(args[1]), Integer.parseInt(args[2]),
				Integer.parseInt(args[3]), Double.parseDouble(args[4]), Integer.parseInt(args[5]), test);
		/*
		 * 
		 * // Clear any remaining tmp files Process p1 = null; if
		 * (System.getProperty("os.name").contains("Windows")){
		 * //Runtime.getRuntime().exec(
		 * "cmd.exe /c for /d %i in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do echo %i "
		 * ); p1 = Runtime.getRuntime().exec(
		 * "cmd.exe /c for /d %x in (c:\\Users\\atna3\\AppData\\Local\\Temp\\autoweka*) do rd /s /q \"%x\" "
		 * ); } else { String[] cmd = { "/bin/sh", "-c", "rm -rf /tmp/autoweka*"
		 * }; p1 = Runtime.getRuntime().exec(cmd);
		 * 
		 * } try { p1.waitFor(); } catch (InterruptedException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 * 
		 */
		// TreeHelperClass.readFitnessFile(2);
	}

	void addGPTreeToArray(String gpFileName, int arrayPosition) {
		TreeOperation opr = null;
		try {
			opr = new TreeOperation(gpFileName, 0.0);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			System.exit(0);
		}
		if (opr != null) {
			opr.populateGpTreeVector();

			AbstractNode mytree = opr.getTree();
			// String testStr = TreeHelperClass.printTreeToString(mytree, 0);

			// System.out.println(testStr);
			Const.GP_TREES[arrayPosition] = mytree;
			Const.GP_TREES_STRING[arrayPosition] = mytree.printAsInFixFunction();
		}

	}

	private static <K, V extends Comparable<? super V>> List<Entry<K, V>> findGreatest(Map<K, V> map, int n) {
		Comparator<? super Entry<K, V>> comparator = new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e0, Entry<K, V> e1) {
				V v0 = e0.getValue();
				V v1 = e1.getValue();
				return v0.compareTo(v1);
			}
		};
		PriorityQueue<Entry<K, V>> highest = new PriorityQueue<Entry<K, V>>(n, comparator);
		for (Entry<K, V> entry : map.entrySet()) {
			highest.offer(entry);
			while (highest.size() > n) {
				highest.poll();
			}
		}

		List<Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>();
		while (highest.size() > 0) {
			result.add(highest.poll());
		}
		return result;
	}
	
	private static <K, V extends Comparable<? super V>> List<Entry<K, V>> findLeast(Map<K, V> map, int n) {
		Comparator<? super Entry<K, V>> comparator = new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e0, Entry<K, V> e1) {
				V v0 = e0.getValue();
				V v1 = e1.getValue();
				return (v0.compareTo(v1) * -1);
			}
		};
		PriorityQueue<Entry<K, V>> least = new PriorityQueue<Entry<K, V>>(n, comparator);
		for (Entry<K, V> entry : map.entrySet()) {
			least.offer(entry);
			while (least.size() > n) {
				least.poll();
			}
		}

		List<Entry<K, V>> result = new ArrayList<Map.Entry<K, V>>();
		while (least.size() > 0) {
			result.add(least.poll());
		}
		return result;
	}


	// Generic function to find the index of an element in an object array in
	// Java
	public static <T> int find(T[] a, T target) {
		return IntStream.range(0, a.length).filter(i -> target.equals(a[i])).findFirst().orElse(-1); // return
																										// -1
																										// if
																										// target
																										// is
																										// not
																										// found
	}

	// Function to find the index of an element in a primitive array in Java
	public static int find(double[] a, double target) {
		return IntStream.range(0, a.length).filter(i -> target == a[i]).findFirst().orElse(-1); // return
																								// -1
																								// if
																								// target
																								// is
																								// not
																								// found
	}
}

/*
 * Fix selectedThresholdsTraining.txt selectedThresholdsTest.txt
 * SimpleTradingTraining.txt not reporting all
 * 
 */
