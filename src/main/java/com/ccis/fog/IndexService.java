package com.ccis.fog;

import codeEdit.compile.StringSourceCompiler;
import codeEdit.execute.JavaClassExecutor;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.jcraft.jsch.JSchException;
import engine.DagGraphUtil;
import engine.DockerLink;
import engine.ParseXML;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.fog.entities.Controller;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.offloading.OffloadingStrategyAllinCloud;
import org.fog.offloading.OffloadingStrategyAllinFog;
import org.fog.offloading.OffloadingStrategySimple;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.workflowsim.*;
import org.workflowsim.scheduling.GASchedulingAlgorithm;
import org.workflowsim.scheduling.PsoScheduling;
import org.workflowsim.utils.ClusteringParameters;
import org.workflowsim.utils.OverheadParameters;
import org.workflowsim.utils.Parameters;
import org.workflowsim.utils.ReplicaCatalog;

import javax.annotation.Resource;
import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

//import static engine.DockerLink.setRealWorkLoad;


@Service
public class IndexService {
    final static String[] algrithmStr = new String[]{"MINMIN","MAXMIN","FCFS","ROUNDROBIN","PSO","GA"};

//    static boolean Flag = true;//表示需要画图
//    static boolean Flag1 = true;//判断FogEnvironmentUI需不需要重新绘制
    static List<FogDevice> fogDevices = new ArrayList<>();
    public static List<Double[]> record=new ArrayList<>();
//    public static List<Double[]> record_double = new ArrayList<>();
    final static int numOfDepts = 1;
    final static int numOfMobilesPerDept = 1;
    static int nodeSize;

    private static WorkflowEngine wfEngine;
    private static Controller controller;

    private static String strategy;
    private static String scheduler_method;
    private static String optimize_objective;
    private static List<String> alList = new ArrayList();

    private static String daxPath;
    private static String customPath;

    public static List<Map<String,String>> dagLinks = new ArrayList<>();
    private static int inputEdges;
    private static int outputEdges;
    public static Map<String , String> codeExecuteRes = new HashMap<>();
    public static List<Map<String,String>> codeExecuteElems = new ArrayList<>();
    public static String customType = "";
    public static Map<String , String> workTypeToImage = new HashMap<>();

    @Value("${sim.xml_path}")
    private String xml_path;

    @Value("${dagXmlPath}")
    private String dagxmlpath;

    @Value("${activitiXmlPath}")
    private String activitixmlpath;

    @Value("${storagePath}")
    private String storagePath;

//    static public Map<String, List<Job>> records = new HashMap<>();
    static public Map<String, List<OutputEntity>> outputMap = new HashMap<>();
    static public int cloudNumber = 1;
    static public int fogNumber = 1;
    static public int mobileNumber = 1;

    static public List<Long> cloudMipsList = new ArrayList<>();
    static public List<Double> cloudCostList = new ArrayList<>();
    static public List<Long> fogMipsList = new ArrayList<>();
    static public List<Double> fogCostList = new ArrayList<>();
    static public List<Long> mobileMipsList = new ArrayList<>();
    static public List<Double> mobileCostList = new ArrayList<>();

    static public String deadlineString;

    static public int pso_repeat;
    static public int ga_repeat;
    static public long pso_time;
    static public long ga_time;


    public static Map<String, String> devicesInfo = new HashMap<String, String>();


    static List<String> pi = new ArrayList<>();
    static List<String> kmp = new ArrayList<>();
    static List<String> levenshtein = new ArrayList<>();
    static List<String> selectsort = new ArrayList<>();






    public String initTypeList() {
        JSONObject jsonObject = new JSONObject();
        File file = new File(xml_path);
        File[] fs = file.listFiles();
        for (File f : fs) {
            String str = f.getName();
            String name = str.substring(0, str.lastIndexOf("."));
            int index = str.lastIndexOf("_");
            if (index != -1) {
                String type = name.substring(0, index);
                String size = name.substring(index+1, name.length());
                JSONArray list = jsonObject.getJSONArray(type);
                if (list == null) {
                    list = new JSONArray();
                }
                list.add(size);
                jsonObject.put(type, list);
            }
        }
        return jsonObject.toJSONString();
    }

    public void initAlSetting(JSONObject json) {
        JSONObject pso = json.getJSONObject("PSO");
        JSONObject ga = json.getJSONObject("GA");

        PsoScheduling.particleNum = pso.getInteger("PSO-particleNum");
        PsoScheduling.iterateNum = pso.getInteger("PSO-iterateNum");
        PsoScheduling.c1 = pso.getDouble("PSO-c1");
        PsoScheduling.c2 = pso.getDouble("PSO-c2");
        PsoScheduling.w = pso.getDouble("PSO-w");
        pso_repeat = pso.getInteger("PSO-repeat");

        GASchedulingAlgorithm.popsize = ga.getInteger("GA-popsize");
        GASchedulingAlgorithm.gmax = ga.getInteger("GA-gmax");
        GASchedulingAlgorithm.crossoverProb = ga.getDouble("GA-crossoverProb");
        GASchedulingAlgorithm.mutationRate = ga.getDouble("GA-mutationRate");
        ga_repeat = ga.getInteger("GA-repeat");
    }

    public void initFogSetting(JSONObject json) {
        cloudNumber = json.getInteger("cloud_number");
        fogNumber = json.getInteger("fog_number");
        mobileNumber = json.getInteger("mobile_number");

        JSONArray cml = JSON.parseArray(json.getString("cloud_mips_list"));
        JSONArray ccl = JSON.parseArray(json.getString("cloud_cost_list"));
        JSONArray fml = JSON.parseArray(json.getString("fog_mips_list"));
        JSONArray fcl = JSON.parseArray(json.getString("fog_cost_list"));
        JSONArray mml = JSON.parseArray(json.getString("mobile_mips_list"));

        cloudMipsList.clear();
        cloudCostList.clear();
        fogMipsList.clear();
        fogCostList.clear();
        mobileMipsList.clear();
        mobileCostList.clear();
        for (int i=0; i<cloudNumber; i++) {
            cloudMipsList.add(cml.getLong(i));
            cloudCostList.add(ccl.getDouble(i));
        }
        for (int i=0; i<fogNumber; i++) {
            fogMipsList.add(fml.getLong(i));
            fogCostList.add(fcl.getDouble(i));
        }
        for (int i=0; i<mobileNumber; i++) {
            mobileMipsList.add(mml.getLong(i));
            mobileCostList.add(0.0);
        }
    }

    /**
     *  清除上次仿真所有对象及标记
     */
    public static int clear()
    {
        try {
            if(controller==null)
                return 0;
            wfEngine.jobList.removeAll(wfEngine.jobList);
            controller.clear();
            wfEngine.clearFlag();
            fogDevices.removeAll(fogDevices);  //清除对象列表
            FogUtils.set1();
//            Object[][] rowData = {};

            // 创建一个表格，指定 所有行数据 和 表头
//            table = new JTable(rowData, columnNames);
//            table.getTableHeader().setForeground(Color.black);
////				FitTableColumns(table);
//            scrollPane.setViewportView(table);
            wfEngine.updatebest.clear();

        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 1;
    }

    private void parseParams(JSONObject json) {
        //主界面设置
        strategy = json.getString("strategy");
        optimize_objective = json.getString("optimize_objective");
        customPath = json.getString("custom");
        if (customPath == null || customPath.equals("")) {
            daxPath = xml_path + json.getString("daxPath");
        } else {
//            daxPath = xml_path + customPath;
//            daxPath = "E:\\dagXML\\" + customPath;
//            System.out.println(dagxmlpath +"llllllllllllll");
            daxPath = dagxmlpath +customPath;
            System.out.println(daxPath +"llllllllllllll");
        }
        nodeSize = json.getInteger("nodeSize");
        deadlineString = json.getString("deadline");
        JSONArray arr = json.getJSONArray("alSet");
        alList.clear();
        for (int i=0; i<arr.size(); i++) {
            alList.add(arr.getString(i));
        }

        initFogSetting(json.getJSONObject("setting_json"));
        initAlSetting(json.getJSONObject("setting_json"));

        JSONArray pi_json= json.getJSONArray("pi");
        JSONArray kmp_json = json.getJSONArray("kmp");
        JSONArray levenshtein_json = json.getJSONArray("levenshtein");
        JSONArray selectsort_json = json.getJSONArray("selectsort");

        for(int i = 0; i < pi_json.size() ; i++){
            pi.add(pi_json.getString(i));
        }
        for(int i = 0; i < kmp_json.size() ; i++){
            kmp.add(kmp_json.getString(i));
        }
        for(int i = 0; i < levenshtein_json.size() ; i++){
            levenshtein.add(levenshtein_json.getString(i));
        }
        for(int i = 0; i < selectsort_json.size() ; i++){
            selectsort.add(selectsort_json.getString(i));
        }

        /*System.out.println(pi);
        System.out.println(kmp);
        System.out.println(levenshtein);
        System.out.println(selectsort);*/
    }

    /**
     * 对list中的double类型元素分别求平均值
     * @param list
     * @return 求完平均值后的double类型数组
     */
    private Double[] GetMean(List<Double[]> list) {
        double a = 0;
        double b = 0;
        double c = 0;
        for(Double[] d : list){
            a += d[1];
            b += d[2];
            c += d[3];
        }
        a = a / list.size();
        b = b / list.size();
        c = c / list.size();
        Double[] r = {a,b,c};
        return r;
    }

    public Double getAlgorithm(String scheduler_method) {
        if(scheduler_method.equals(algrithmStr[0]))
            return 1.0;
        else if(scheduler_method.equals(algrithmStr[1]))
            return 2.0;
        else if(scheduler_method.equals(algrithmStr[2]))
            return 3.0;
        else if(scheduler_method.equals(algrithmStr[3]))
            return 4.0;
        else if(scheduler_method.equals(algrithmStr[4]))
            return 5.0;
        else if(scheduler_method.equals(algrithmStr[5]))
            return 6.0;
        return null;
    }

    private long GetAverageTime(List<Long> list){
        long sum = 0;
        for(long i : list){
            sum += i;
        }
        return sum/Math.max(1,list.size());
    }

    /**
     * 获得所输入的ga参数设置
     * @return 获取成功返回true 未输入返回false
     */
    public void getgasetting(WorkflowEngine wfEngine){
        wfEngine.fitnessForGA = new double[GASchedulingAlgorithm.popsize];
    }

    /**
     * 开始模拟
     * @param json
     * @return
     */
    public JSONObject doSim(JSONObject json) {
        //重置customType
        customType = "";

        System.out.println(json);

        outputMap.clear();
        parseParams(json);
        record.clear();
        pso_time = 0;
        ga_time = 0;
        for (String method : alList) {
            scheduler_method = method;
            if (scheduler_method.equals("PSO")) {
                getpsosetting(wfEngine);
                int repeat = Math.max(1, pso_repeat);
                List<Double[]> repeats = new ArrayList<>();
                List<Long> times = new ArrayList<>();
                for(int i = 0; i < repeat; i++){
                    System.out.println("---------------------------For the "+(i+1)+" pso--------------------------");
                    long time = StartAlgorithm(scheduler_method+i);
                    repeats.add(record.get((record.size()-1)));
                    record.remove(record.size()-1);
                    times.add(time);
                }
                Double[] mean = GetMean(repeats);
                Double[] algomean = new Double[4];
                algomean[0] = getAlgorithm(scheduler_method);System.out.println(scheduler_method+":");
                algomean[1] = mean[0];System.out.println("Average task execution time = "+mean[0]);
                algomean[2] = mean[1];System.out.println("Average energy consumption = "+mean[1]);
                algomean[3] = mean[2];System.out.println("Average cost = "+mean[2]);
                record.add(algomean);
                if(wfEngine.getoffloadingEngine().getOffloadingStrategy() != null)
                    System.out.println("Average offloading Strategy time = " + wfEngine.getAverageOffloadingTime());
                long averageTime = GetAverageTime(times);
                times=null;
                System.out.println("Average "+scheduler_method+" algorithm execution time = " + averageTime);
//                displayTime(averageTime);
                pso_time = averageTime;
                System.out.println("Drawing "+scheduler_method+" iteration figure......");
            } else if (scheduler_method.equals("GA")) {
                getgasetting(wfEngine);
                int repeat = Math.max(1, ga_repeat);
                List<Double[]> repeats = new ArrayList<Double[]>();
                List<Long> times = new ArrayList<Long>();
                for (int i = 0; i < repeat; i++) {
                    System.out.println("---------------------------For the " + (i + 1) + " ga--------------------------");
                    long time = StartAlgorithm(scheduler_method+i);
                    repeats.add(record.get((record.size() - 1)));
                    record.remove(record.size() - 1);
                    times.add(time);
                }
                //TODO:这块是干啥的?
                Double[] mean = GetMean(repeats);
                repeats = null;
                Double[] algomean = new Double[4];
                algomean[0] = getAlgorithm(scheduler_method);
                System.out.println(scheduler_method + ":");
                algomean[1] = mean[0];
                System.out.println("Average task execution time = " + mean[0]);
                algomean[2] = mean[1];
                System.out.println("Average energy consumption = " + mean[1]);
                algomean[3] = mean[2];
                System.out.println("Average cost = " + mean[2]);
                record.add(algomean);
                if (wfEngine.getoffloadingEngine().getOffloadingStrategy() != null)
                    System.out.println("Average offloading Strategy time = " + wfEngine.getAverageOffloadingTime());
                long averageTime = GetAverageTime(times);
                times = null;
                System.out.println("Average " + scheduler_method + " algorithm execution time = " + averageTime);
//                displayTime(averageTime);
                ga_time = averageTime;
                System.out.println("Drawing " + scheduler_method + " iteration figure......");
            } else {
                StartAlgorithm(scheduler_method);
            }
        }

        //得到工作流的结构
        String dagRelations = getDag(json.getString("daxPath"),  json.getString("custom"));
        System.out.println("dagRelations:" + dagRelations);

        JSONObject retJson = new JSONObject();
        retJson.put("outputMap", outputMap);
        retJson.put("x", wfEngine.iterateNum);
        retJson.put("y", wfEngine.updatebest);
        retJson.put("record", record);
        retJson.put("pso_time", pso_time);
        retJson.put("ga_time", ga_time);
        retJson.put("dagRelations" , dagRelations);

        retJson.put("mobileNumber", mobileNumber);
        retJson.put("fogNumber", fogNumber);
        retJson.put("cloudNumber", cloudNumber);
//        record_double.clear();
        DockerLink dockerLink = new DockerLink();
        dockerLink.setRealWorkLoad();
        /*if(realFlag.equals("1")){
            try {
                DockerLink.getSimulationDate();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSchException e) {
                e.printStackTrace();
            }
        }*/
//        retJson.put("record_double" , record_double);

        System.out.println("retJson:" + retJson);
/*        for (Double[] item: record_double) {
            System.out.println(item[0]);
            System.out.println(item[1]);
            System.out.println(item[2]);
            System.out.println(item[3]);
            System.out.println(item[4]);
            System.out.println(item[5]);
            System.out.println(item[6]);
        }*/

        return retJson;
    }
    /*public JSONObject doSim(JSONObject json) {
        outputMap.clear();
        parseParams(json);
        record.clear();
        pso_time = 0;
        ga_time = 0;
        for (String method : alList) {
            scheduler_method = method;
            if (scheduler_method.equals("PSO")) {
                getpsosetting(wfEngine);
                int repeat = Math.max(1, pso_repeat);
                List<Double[]> repeats = new ArrayList<>();
                List<Long> times = new ArrayList<>();
                for(int i = 0; i < repeat; i++){
                    System.out.println("---------------------------For the "+(i+1)+" pso--------------------------");
                    long time = StartAlgorithm(scheduler_method+i);
                    repeats.add(record.get((record.size()-1)));
                    record.remove(record.size()-1);
                    times.add(time);
                }
                Double[] mean = GetMean(repeats);
                Double[] algomean = new Double[4];
                algomean[0] = getAlgorithm(scheduler_method);System.out.println(scheduler_method+":");
                algomean[1] = mean[0];System.out.println("Average task execution time = "+mean[0]);
                algomean[2] = mean[1];System.out.println("Average energy consumption = "+mean[1]);
                algomean[3] = mean[2];System.out.println("Average cost = "+mean[2]);
                record.add(algomean);
                if(wfEngine.getoffloadingEngine().getOffloadingStrategy() != null)
                    System.out.println("Average offloading Strategy time = " + wfEngine.getAverageOffloadingTime());
                long averageTime = GetAverageTime(times);times=null;
                System.out.println("Average "+scheduler_method+" algorithm execution time = " + averageTime);
//                displayTime(averageTime);
                pso_time = averageTime;
                System.out.println("Drawing "+scheduler_method+" iteration figure......");
            } else if (scheduler_method.equals("GA")) {
                getgasetting(wfEngine);
                int repeat = Math.max(1, ga_repeat);
                List<Double[]> repeats = new ArrayList<Double[]>();
                List<Long> times = new ArrayList<Long>();
                for (int i = 0; i < repeat; i++) {
                    System.out.println("---------------------------For the " + (i + 1) + " ga--------------------------");
                    long time = StartAlgorithm(scheduler_method+i);
                    repeats.add(record.get((record.size() - 1)));
                    record.remove(record.size() - 1);
                    times.add(time);
                }
                //TODO:这块是干啥的?
                Double[] mean = GetMean(repeats);
                repeats = null;
                Double[] algomean = new Double[4];
                algomean[0] = getAlgorithm(scheduler_method);
                System.out.println(scheduler_method + ":");
                algomean[1] = mean[0];
                System.out.println("Average task execution time = " + mean[0]);
                algomean[2] = mean[1];
                System.out.println("Average energy consumption = " + mean[1]);
                algomean[3] = mean[2];
                System.out.println("Average cost = " + mean[2]);
                record.add(algomean);
                if (wfEngine.getoffloadingEngine().getOffloadingStrategy() != null)
                    System.out.println("Average offloading Strategy time = " + wfEngine.getAverageOffloadingTime());
                long averageTime = GetAverageTime(times);
                times = null;
                System.out.println("Average " + scheduler_method + " algorithm execution time = " + averageTime);
//                displayTime(averageTime);
                ga_time = averageTime;
                System.out.println("Drawing " + scheduler_method + " iteration figure......");
            } else {
                StartAlgorithm(scheduler_method);
            }
        }
        JSONObject retJson = new JSONObject();
        retJson.put("outputMap", outputMap);
        retJson.put("x", wfEngine.iterateNum);
        retJson.put("y", wfEngine.updatebest);
        retJson.put("record", record);
        retJson.put("pso_time", pso_time);
        retJson.put("ga_time", ga_time);
        return retJson;
    }*/

    /**
     * 获得所输入的pso参数设置
     * @return 获取成功返回true 未输入返回false
     */
    public void getpsosetting(WorkflowEngine wfEngine){
        wfEngine.fitness = new double[PsoScheduling.particleNum];
        wfEngine.fitness2 = new double[PsoScheduling.particleNum];
    }

    public long StartAlgorithm(String curMethod) {
        clear();
        Log.printLine("Starting FogWorkflowSim version 1.0");
        System.out.println("Optimization objective : "+optimize_objective);

        //TODO:自定义工作流xml文件待完成
//        if(XMLFile!=null){
//            //自定义工作流xml文件
//            daxPath = XMLFile.getPath();
//            String path = XMLFile.getName();
//            String str="";
//            if(path != null && !"".equals(path)){
//                for(int i=0;i<path.length();i++){
//                    if(path.charAt(i)>=48 && path.charAt(i)<=57){
//                        str+=path.charAt(i);
//                    }
//                }
//            }
//            nodeSize = Integer.parseInt(str);
//        }
//        else{//系统自带工作流xml文件
//            daxPath="config/dax/"+inputTypeCb.getSelectedItem()+"_"+nodeSizeCb.getSelectedItem()+".xml";
//            nodeSize = Integer.parseInt((String) nodeSizeCb.getSelectedItem());
//        }

        double deadline;
        try {
            deadline = Double.parseDouble(deadlineString);
        } catch (Exception e) {
            deadline = Double.MAX_VALUE;
        }

        simulate(deadline);
        CloudSim.startSimulation();
        List<Job> outputList0 = wfEngine.getJobsReceivedList();
        CloudSim.stopSimulation();
        Log.enable();
//        printJobList(scheduler_method, outputList0);

        List<OutputEntity> outputList = new ArrayList<>();
        for (int i=0; i<outputList0.size(); i++) {
            OutputEntity outputEntity = new OutputEntity();
            Job job = outputList0.get(i);
            outputEntity.setJobId(String.valueOf(job.getCloudletId()));
            if (job.getClassType() == Parameters.ClassType.STAGE_IN.value) {
                outputEntity.setTaskId("Stage-in");
                outputEntity.setWorkType("pi");
                System.out.println("ttttttt:" + outputEntity);
            } else {
                outputEntity.setTaskId(String.valueOf(job.getClassType()));
            }
//            for (Task task : job.getTaskList()) {
//                outputEntity.setJobId(String.valueOf(task.getCloudletId()));
//            }
            DecimalFormat dft = new DecimalFormat("######0.00");
            outputEntity.setDataCenterId(job.getResourceName(job.getResourceId()));
            outputEntity.setVmId(String.valueOf(job.getVmId()));
            outputEntity.setTime(String.valueOf(dft.format(job.getActualCPUTime())));
            outputEntity.setStartTime(String.valueOf(dft.format(job.getExecStartTime())));
            outputEntity.setFinishTime(String.valueOf(dft.format(job.getFinishTime())));
            outputEntity.setDepth(String.valueOf(job.getDepth()));
            outputEntity.setCost(String.valueOf(dft.format(job.getProcessingCost())));
            List<Task> l = job.getParentList();
            String parents ="";
            for(Task task : l)
                parents += task.getCloudletId()+",";
            outputEntity.setParents(parents);
            if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                outputEntity.setStatus("SUCCESS");
            } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                outputEntity.setStatus("FAILED");
            }
            //初始化真实执行状态
            outputEntity.setRealStatus("FAILED");

            /*System.out.println("kmp");
            System.out.println(pi);
            System.out.println(kmp);
            System.out.println(levenshtein);
            System.out.println(selectsort);*/

            //设置任务类型
            String JobId = outputEntity.getJobId();


            if (customType.equals("custom")){
                if (!outputEntity.getTaskId().equals("Stage-in")){
                    outputEntity.setWorkType(workTypeToImage.get(JobId));
//                    System.out.println("JobId:" + JobId);
//                    System.out.println("workTypeToImage: " + workTypeToImage);
                }
            }else{
                for (String piItem: pi) {
                    if(JobId.equals(piItem)){
                        outputEntity.setWorkType("pi");
                    }
                }
                for (String kmpItem: kmp) {
                    if(JobId.equals(kmpItem)){
                        outputEntity.setWorkType("kmp");
                    }
                }
                for (String levenshteinItem: levenshtein) {
                    if(JobId.equals(levenshteinItem)){
                        outputEntity.setWorkType("levenshtein");
                    }
                }
                for (String selectsortItem: selectsort) {
                    if(JobId.equals(selectsortItem)){
                        outputEntity.setWorkType("selectsort");
                    }
                }
            }


            outputList.add(outputEntity);
        }
        //对outputEntityList排序
        Collections.sort(outputList, new Comparator<OutputEntity>() {
            @Override
            public int compare(OutputEntity outputEntity1, OutputEntity outputEntity2) {
                int jobId1 = Integer.parseInt(outputEntity1.getJobId());
                int jobId2 = Integer.parseInt(outputEntity2.getJobId());
                int diff = jobId1 - jobId2;
                if(diff > 0){
                    return 1;
                }else if(diff < 0){
                    return -1;
                }else{
                    return 0;
                }

            }
        });
//        System.out.println("ddddd");
        System.out.println(outputList);
        outputMap.put(curMethod, outputList);

//            printJobList(outputList0);
        controller.print();
        double totalExecutionTime = 0.0;
        for (OutputEntity outputEntity : outputList) {
            totalExecutionTime += Double.parseDouble(outputEntity.getTime());
        }

        Double[] a = {getAlgorithm(scheduler_method), controller.TotalExecutionTime, controller.TotalEnergy, controller.TotalCost};
//        Double[] a = {getAlgorithm(scheduler_method), totalExecutionTime, controller.TotalEnergy, controller.TotalCost};
        record.add(a);
        return wfEngine.algorithmTime;
    }


    public void simulate(double deadline) {
        System.out.println("Starting Task...");

        try {
            //Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events

            CloudSim.init(num_user, calendar, trace_flag);

            String appId = "workflow"; // identifier of the application

            createFogDevices(1,appId);//(broker.getId(), appId);

            List<? extends Host> hostlist = new ArrayList<Host>();
            int hostnum = 0;
            for(FogDevice device : fogDevices){
                hostnum += device.getHostList().size();
                hostlist.addAll(device.getHostList());
            }
            int vmNum = hostnum;//number of vms;

            File daxFile = new File(daxPath);
            if (!daxFile.exists()) {
                System.out.println("Warning: Please replace daxPath with the physical path in your working environment!");
                return;
            }

            /**
             * Since we are using MINMIN scheduling algorithm, the planning
             * algorithm should be INVALID such that the planner would not
             * override the result of the scheduler
             */
            Parameters.SchedulingAlgorithm sch_method =Parameters.SchedulingAlgorithm.valueOf(scheduler_method);
            Parameters.Optimization opt_objective = Parameters.Optimization.valueOf(optimize_objective);
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.INVALID;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.SHARED;
            /**
             * No overheads
             */
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);

            /**
             * No Clustering
             */
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);

            /**
             * Initialize static parameters
             */
            Parameters.init(vmNum, daxPath, null,
                    null, op, cp, sch_method, opt_objective,
                    pln_method, null, 0);
            ReplicaCatalog.init(file_system);

            /**
             * Create a WorkflowPlanner with one schedulers.
             */
            WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);
            /**
             * Create a WorkflowEngine.
             */
            wfEngine = wfPlanner.getWorkflowEngine();
            /**
             * Set a offloading Strategy for OffloadingEngine
             */
            switch (strategy) {
//                case "All-in-Fog":
                case "All-in-Edge":
                    wfEngine.getoffloadingEngine().setOffloadingStrategy(new OffloadingStrategyAllinFog());
                    break;
                case "All-in-Cloud":
                    wfEngine.getoffloadingEngine().setOffloadingStrategy(new OffloadingStrategyAllinCloud());
                    break;
                case "Energy-Optimal":
                    System.out.println("Energy-Optimal");
                    wfEngine.getoffloadingEngine().setOffloadingStrategy(new OffloadingStrategySimple());
                    break;
                case "Simple":
                    wfEngine.getoffloadingEngine().setOffloadingStrategy(new OffloadingStrategySimple());
                    break;
                default:
                    wfEngine.getoffloadingEngine().setOffloadingStrategy(null);
                    break;
            }
            /**
             * Set a deadline of workflow for WorkflowEngine
             */
            wfEngine.setDeadLine(deadline);
            /**
             * Create a list of VMs.The userId of a vm is basically the id of
             * the scheduler that controls this vm.
             */
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), Parameters.getVmNum(), hostlist);
            hostlist = null;//清空，释放内存
            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmlist0, 0);
            vmlist0 = null;

            controller = new Controller("master-controller", fogDevices, wfEngine);

            /**
             * Binds the data centers with the scheduler.
             */
            List<PowerHost> list;
            for(FogDevice fogdevice:controller.getFogDevices()){
                wfEngine.bindSchedulerDatacenter(fogdevice.getId(), 0);
                list = fogdevice.getHostList();  //输出设备上的主机
                System.out.println(fogdevice.getName()+": ");
                for (PowerHost host : list){
                    System.out.print(host.getId()+":Mips("+host.getTotalMips()+"),"+"cost("+host.getcostPerMips()+")  ");
                    devicesInfo.put((host.getId() - 1) + "",host.getTotalMips() + "");
                }
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Unwanted errors happen");
        }
    }

    protected static List<CondorVM> createVM(int userId, int vms, List<? extends Host> devicelist) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<CondorVM> list = new LinkedList<>();

        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 512; //vm memory (MB)
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        CondorVM[] vm = new CondorVM[vms];
        for (int i = 0; i < vms; i++) {
            double ratio = 1.0;
            int mips = devicelist.get(i).getTotalMips();
            vm[i] = new CondorVM(i, userId, mips * ratio, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }

    private void createFogDevices(int userId, String appId) {

        double ratePerMips = 0.96;
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1; // the cost of using storage in this resource
        double costPerBw = 0.2;//每带宽的花费

//        List<Long> GHzList = new ArrayList<>();//云中的主机
//        List<Double> CostList = new ArrayList<>();
//        for(JTextField textField : FEframe.DCMipsMap.get("cloud")){
//            if(textField.getText().isEmpty())
//                GHzList.add((long)10000);
//            else
//                GHzList.add(Long.valueOf(textField.getText()));
//        }
//        for(JTextField textField : FEframe.DCCostMap.get("cloud")){
//            if(textField.getText().isEmpty())
//                CostList.add(0.96);
//            else
//                CostList.add(Double.valueOf(textField.getText()));
//        }
//        cloudNumCb.setSelectedItem(String.valueOf(GHzList.size()));

//        GHzList.add((long)1600);
//        CostList.add(0.96);

        FogDevice cloud = createFogDevice("cloud", cloudMipsList.size(), cloudMipsList, cloudCostList,
                40000, 100, 10000, 0, ratePerMips, 16*103, 16*83.25,costPerMem,costPerStorage,costPerBw); // creates the fog device Cloud at the apex of the hierarchy with level=0
        cloud.setParentId(-1);

        fogDevices.add(cloud);
        for(int i=0;i<numOfDepts;i++){
            addFogNode(i+"", userId, appId, fogDevices.get(0).getId()); // adding a fog device for every Gateway in physical topology. The parent of each gateway is the Proxy Server
        }
    }

    private  FogDevice addFogNode(String id, int userId, String appId, int parentId){

        double ratePerMips = 0.48;
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1; // the cost of using storage in this resource
        double costPerBw = 0.1;//每带宽的花费

//        List<Long> GHzList = new ArrayList<>();//雾中的主机
//        List<Double> CostList = new ArrayList<>();
//        for(JTextField textField : FEframe.DCMipsMap.get("fog")){
////            if(textField.getText().isEmpty())
////                GHzList.add((long)5000);
////            else
////                GHzList.add(Long.valueOf(textField.getText()));
////        }
////        for(JTextField textField : FEframe.DCCostMap.get("fog")){
////            if(textField.getText().isEmpty())
////                CostList.add(0.48);
////            else
////                CostList.add(Double.valueOf(textField.getText()));
////        }
////        edgeNumCb.setSelectedItem(String.valueOf(GHzList.size()));

//        GHzList.add((long)1300);
//        CostList.add(0.48);

        FogDevice dept = createFogDevice("f-"+id, fogMipsList.size(), fogMipsList, fogCostList,
                4000, 10000, 10000, 1, ratePerMips, 700, 30,costPerMem,costPerStorage,costPerBw);
        fogDevices.add(dept);
        dept.setParentId(parentId);
        dept.setUplinkLatency(4); // latency of connection between gateways and server is 4 ms
        for(int i=0;i<numOfMobilesPerDept;i++){
            String mobileId = id+"-"+i;
            FogDevice mobile = addMobile(mobileId, userId, appId, dept.getId()); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 4 ms
            fogDevices.add(mobile);
        }
        return dept;
    }

    private  FogDevice addMobile(String id, int userId, String appId, int parentId){
        double costPerMem = 0.05; // the cost of using memory in this resource
        double costPerStorage = 0.1; // the cost of using storage in this resource
        double costPerBw = 0.3;//每带宽的花费

//        List<Long> GHzList = new ArrayList<>();
//        List<Double> CostList = new ArrayList<>();
//        for(JTextField textField : FEframe.DCMipsMap.get("mobile")){
//            CostList.add(0.0);
//            if(textField.getText().isEmpty())
//                GHzList.add((long)1000);
//            else
//                GHzList.add(Long.valueOf(textField.getText()));
//        }
//        mobileNumCb.setSelectedItem(String.valueOf(GHzList.size()));

//        GHzList.add((long)1000);
//        CostList.add(0.0);

        FogDevice mobile = createFogDevice("m-"+id, mobileMipsList.size(), mobileMipsList, mobileCostList,
                10000, 20*1024, 40*1024, 3, 0, 700, 30,costPerMem,costPerStorage,costPerBw);
        mobile.setParentId(parentId);
        return mobile;
    }

    /**
     * Creates a vanilla fog device
     * @param nodeName name of the device to be used in simulation
     * @param hostnum the number of the host of device
     * @param mips the list of host'MIPS
     * @param costPerMips the list of host'cost per mips
     * @param ram RAM
     * @param upBw uplink bandwidth (Kbps)
     * @param downBw downlink bandwidth (Kbps)
     * @param level hierarchy level of the device
     * @param ratePerMips cost rate per MIPS used
     * @param busyPower(mW)
     * @param idlePower(mW)
     * @return
     */
    private FogDevice createFogDevice(String nodeName, int hostnum, List<Long> mips, List<Double> costPerMips,
                                      int ram, long upBw, long downBw, int level, double ratePerMips,
                                      double busyPower, double idlePower,
                                      double costPerMem,double costPerStorage,double costPerBw) {

        List<Host> hostList = new ArrayList<Host>();

        for ( int i = 0 ;i < hostnum; i++ )
        {
            List<Pe> peList = new ArrayList<Pe>();
            // Create PEs and add these into a list.
            peList.add(new Pe(0, new PeProvisionerSimple(mips.get(i)))); // need to store Pe id and MIPS Rating
            int hostId = FogUtils.generateEntityId();
            long storage = 1000000; // host storage
            int bw = 10000;

            PowerHost host = new PowerHost(
                    hostId,
                    costPerMips.get(i),
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    storage,
                    peList,
                    new VmSchedulerTimeShared(peList),
                    new FogLinearPowerModel(busyPower, idlePower)//默认发送功率100mW 接收功率25mW
            );

            hostList.add(host);
        }

        // Create a DatacenterCharacteristics object
        String arch = "x86"; // system architecture
        String os = "Linux"; // operating system
        String vmm = "Xen";
        double time_zone = 10.0; // time zone this resource located
        double cost = 3.0; // the cost of using processing in this resource每秒的花费
			/*double costPerMem = 0.05; // the cost of using memory in this resource
			double costPerStorage = 0.1; // the cost of using storage in this resource
			double costPerBw = 0.1; // the cost of using bw in this resource每带宽的花费*/
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem,
                costPerStorage, costPerBw);

        FogDevice fogdevice = null;

        // Finally, we need to create a storage object.
        try {
            HarddriveStorage s1 = new HarddriveStorage(nodeName, 1e12);
            storageList.add(s1);
            fogdevice = new FogDevice(nodeName, characteristics,
                    new VmAllocationPolicySimple(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fogdevice.setLevel(level);
        return fogdevice;
    }

    public String upload(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (!file.isEmpty()) {
            File filepath = new File(xml_path);
            if (!filepath.exists())
                filepath.mkdirs();
            try {
                // 转存文件
                file.transferTo(new File(xml_path + fileName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return fileName;
    }
    public String uploadgetloaction(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (!file.isEmpty()) {
            File filepath = new File(xml_path);
            if (!filepath.exists())
                filepath.mkdirs();
            try {
                // 转存文件
                file.transferTo(new File(xml_path + fileName));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String location=xml_path+fileName;

        return location;
    }


    //@Resource ：自动注入，项目启动后会实例化一个JdbcTemplate对象,省去初始化工作。
    @Resource
    private JdbcTemplate jdbcTemplate;
    //用户登录校验
    public int loginCheck(User user) {
        int flag = 0;
        String sql = "SELECT * FROM userinfo WHERE email ='" + user.getEmail() + "' AND password = '" + user.getPassword() +"'";
//        System.out.println("sql:" + sql);
        List<User> userList = (List<User>) jdbcTemplate.query(sql, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setOrganization(rs.getString("organization"));
                user.setSubscribe(rs.getString("subscribe"));
                return user;
            }
        });
//        System.out.println(userList);
        //用户名，密码都正确
        if(userList.size() > 0){
            flag = 2;
        }
        else{//用户名正确
            sql = "SELECT * FROM userinfo WHERE email = '" + user.getEmail() + "'";
            userList = (List<User>) jdbcTemplate.query(sql, new RowMapper<User>() {
                @Override
                public User mapRow(ResultSet rs, int i) throws SQLException {
                    User user = new User();
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setOrganization(rs.getString("organization"));
                    return user;
                }
            });
            if(userList.size() > 0){
                flag = 1;
            }
            else{//用户名密码都不正确
                flag = 0;
            }
        }

        return flag;
    }

//    用户注册
    public String register(User user) {
        String username = user.getUsername();
        String password = user.getPassword();
        String email = user.getEmail();
//        String address = user.getAddress();
        String organization =user.getOrganization();
//        String telnumber = user.getTelnumber();
        String subscribe = user.getSubscribe();
        JSONArray xmlfiles =new JSONArray();
        JSONObject plan = new JSONObject();

        String sql = "SELECT * FROM userinfo WHERE username = '" + user.getUsername() + "'";

        List<User>  userList = (List<User>) jdbcTemplate.query(sql, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setOrganization(rs.getString("organization"));
                user.setSubscribe(rs.getString("subscribe"));
                user.setXmlfiles(rs.getString("xmlfiles"));
                return user;
            }
        });
        if (userList.size() > 0){
            return "existed";
        }
        sql = "INSERT INTO userinfo (username,password,email,organization,subscribe,xmlfiles,plan) VALUES ('"
                + username +"','"+ password + "','" + email + "','"
                + organization+ "','" + subscribe +"','" + xmlfiles.toString() + "','" + plan.toString() +"')";
//        System.out.println(sql);
        int insert_flag = jdbcTemplate.update(sql);
//        System.out.println(insert_flag);
        if (insert_flag == 1){
            return "success";

        }else{
            return "failed";
        }
    }

    //查询用户信息
    public User getUser(String email) {


        String sql = "SELECT * FROM userinfo WHERE email = '" + email + "'";
//        System.out.println("sql:"+sql);
        List<User>  userList = (List<User>) jdbcTemplate.query(sql, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setOrganization(rs.getString("organization"));
                user.setSubscribe(rs.getString("subscribe"));
                user.setXmlfiles(rs.getString("xmlfiles"));
                user.setPlan(rs.getString("plan"));
                return user;
            }
        });
//        System.out.println("userlist" + userList);
        User user= userList.get(0);
       /* String xmlfiles = user.getXmlfiles();
        JSONArray files= new JSONArray();
        JSONArray xmlfiles_json = JSONArray.parseArray(xmlfiles);
        for (int i = 0;i< xmlfiles_json.size(); i++){
            files.add(xmlfiles_json.get(i).toString());
            System.out.println(xmlfiles_json.get(i).toString());
        }

        files.add("file1");
        files.remove("file2");//file2为指定名称
        String xmlfiles_update = "update userinfo set xmlfiles ='" + files.toString() + "' where email='" + email +"'";;
        System.out.println(xmlfiles_update);
        int insert_flag = jdbcTemplate.update(xmlfiles_update);
        System.out.println(insert_flag);
        System.out.println(files);*/
        return user;
    }

    //记录访问次数
    public String updateCount(String ipAddress , String currentTime) {
//        String username = visitCount.getUserName();
//        String visitip = visitCount.getVisitIp();
//        String visitaddress = visitCount.getVisitAddress();
//        String visitdate = visitCount.getVisitDate();
//
//        String sql = "insert into visitcount(username,visitip,visitdate,visitaddress) values('" + username + "','" + visitip + "','" + visitdate + "','" + visitaddress + "')";
//        int insert_flag = jdbcTemplate.update(sql);
////        System.out.println("insert flag" + insert_flag);
//        if (insert_flag == 1){
//            return "success";
//
//        }else{
//            return "failed";
//        }
        String sql = "insert into accesscount(visitip,visitdate) values('" + ipAddress + "','" + currentTime + "')";
        int insert_flag = jdbcTemplate.update(sql);
        if (insert_flag == 1) {
            return "success";

        }else{
            return "failed";
        }

    }
    public JSONObject getVisitCount() {
//        今天的日期
        Date date=new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(calendar.DATE, 0);//如果把0修改为-1就代表昨天
        date = calendar.getTime();
        SimpleDateFormat format= new SimpleDateFormat("yyyy-MM-dd");
        String today = format.format(date);
//        System.out.println("dateString:" + dateString);
//        String today = dateString;

//        总访问次数
        String sql_sum = "SELECT count(*) FROM accesscount";
//        今日总访问次数
        String sql_sum_today = "SELECT count(*) FROM accesscount WHERE visitdate LIKE '" + today +"%'";

//        System.out.println(sql_sum);
//        System.out.println(sql_sum_today);
        int sum = jdbcTemplate.queryForObject(sql_sum,Integer.class);
        int sum_today = jdbcTemplate.queryForObject(sql_sum_today,Integer.class);



        JSONObject json =new JSONObject();
        json.put("sum", sum);
        json.put("sum_today", sum_today);
//        System.out.println("json:" + json.toJSONString());
        return json;
    }

    //获取系统版本信息
    public String getVersions() {
       String sql = " SELECT * FROM systemversion";
        List<SystemVersion>  versionList = (List<SystemVersion>) jdbcTemplate.query(sql, new RowMapper<SystemVersion>() {
            @Override
            public SystemVersion mapRow(ResultSet rs, int i) throws SQLException {
                SystemVersion version = new SystemVersion();
                version.setUpdateTime(rs.getString("updateTime"));
                version.setNote(rs.getString("note"));
                version.setVersionNum(rs.getString("versionNum"));
                return version;
            }
        });

//        System.out.println(versionList.toString());
        return JSON.toJSONString(versionList);

    }

    //查看系统当前版本号
    public String getCurrentVersion() {
        String sql = "SELECT * FROM systemversion ORDER BY id DESC limit 1";
//        System.out.println("sql:"+sql);
        List<SystemVersion>  versionList = (List<SystemVersion>) jdbcTemplate.query(sql, new RowMapper<SystemVersion>() {
            @Override
            public SystemVersion mapRow(ResultSet rs, int i) throws SQLException {
                SystemVersion systemVersion = new SystemVersion();
                systemVersion.setVersionNum(rs.getString("versionNum"));
                systemVersion.setNote(rs.getString("note"));
                systemVersion.setUpdateTime(rs.getString("updateTime"));
                return systemVersion;
            }
        });
        SystemVersion systemVersion= versionList.get(0);
        return JSON.toJSONString(systemVersion);

    }

    //开发人员信息
    public String getDevelopers() {
        String sql = "SELECT * FROM developers";
        List<Developer>  developerList = (List<Developer>) jdbcTemplate.query(sql, new RowMapper<Developer>() {
            @Override
            public Developer mapRow(ResultSet rs, int i) throws SQLException {
                Developer developer = new Developer();
                developer.setName(rs.getString("name"));
                developer.setUniversity(rs.getString("university"));
                developer.setEmail(rs.getString("email"));
                return developer;
            }
        });

//        System.out.println(JSON.toJSONString(developerList));
        return JSON.toJSONString(developerList);
    }

    public String updateAdvices(Advices advices) {
        String date = advices.getDate();
        String context = advices.getAdvices();
        String sql = "insert into advices(date,advices) values('" + date + "','" + context + "')";
        int insert_flag = jdbcTemplate.update(sql);
        if(insert_flag == 1){
            return "success";
        }
        else{
            return "false";
        }
    }

    public String getRecommendations() {
        String sql = "SELECT * FROM advices";
        List<Advices>  advicesList = (List<Advices>) jdbcTemplate.query(sql, new RowMapper<Advices>() {
            @Override
            public Advices mapRow(ResultSet rs, int i) throws SQLException {
                Advices advices = new Advices();
                advices.setDate(rs.getString("date"));
                advices.setAdvices(rs.getString("advices"));
                return advices;
            }
        });

        System.out.println(JSON.toJSONString(advicesList));
        return JSON.toJSONString(advicesList);
    }

    //重置密码
    public String resetPsw(String emailAddress , String password) {
        String sql_select = "SELECT * FROM userinfo where email='" + emailAddress +"'";
        List<User>  userList = (List<User>) jdbcTemplate.query(sql_select, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setOrganization(rs.getString("organization"));
                user.setSubscribe(rs.getString("subscribe"));
                return user;
            }
        });
        if(userList.size() == 0){
            return "none";
        }
        else{
            String sql = "update userinfo set password='" + password + "' where email='" + emailAddress +"'";
            int insert_flag = jdbcTemplate.update(sql);
            if(insert_flag == 1){
                return "success";
            }
            else{
                return "failure";
            }
        }

    }

    //注册邮箱检查
    public int registerEmailCheck(String emailAddress) {
        String sql_select = "SELECT * FROM userinfo where email='" + emailAddress +"'";
        List<User>  userList = (List<User>) jdbcTemplate.query(sql_select, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setOrganization(rs.getString("organization"));
                user.setSubscribe(rs.getString("subscribe"));
                return user;
            }
        });
        return userList.size();
    }

    //查询用户创建的xml文件名称
    public String getXmlFile(User user) {
        String email = user.getEmail();
        String password = user.getPassword();

        String sql_select = "SELECT * FROM userinfo where email = '"+ email + "' and password = '" + password + "'";
        List<User>  userList = (List<User>) jdbcTemplate.query(sql_select, new RowMapper<User>() {
            @Override
            public User mapRow(ResultSet rs, int i) throws SQLException {
                User user = new User();
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                user.setOrganization(rs.getString("organization"));
                user.setSubscribe(rs.getString("subscribe"));
                user.setXmlfiles(rs.getString("xmlfiles"));
                return user;
            }
        });
        User user_return = userList.get(0);
        System.out.println(user_return);
        return "";
    }

    //在真实环境中执行单个任务
    public String realOperate(OutputEntity outputEntity) throws IOException, JSchException {
        DockerLink dockerLink = new DockerLink();
        String res_real = dockerLink.getSingleData(outputEntity);
        return res_real;
    }

    //创建单个任务的Container
    public String createSingleContainer(JSONObject outputParams){

        JSONObject containerParam = outputParams.getJSONObject("containerParam");
        OutputEntity outputEntity = outputParams.getObject("outputEntity" , OutputEntity.class);
        System.out.println("containerParam: " + containerParam);
        System.out.println("outputEntity: " + outputEntity);


        String username = containerParam.getString("username");
        String planName = containerParam.getString("planName");
        String workflowName = containerParam.getString("workflowName");
        String imageName = outputEntity.getWorkType();

        //如果taskId = "Stage-in",不创建container，使用标准container，反之创建自定义容器
        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        //编译java文件,生成class文件
        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
        String javacCommand = "cd " + path + "\n" +
                "javac " + imageName +".java";
        System.out.println(javacCommand);
        String javacRes = "";
        try {
            javacRes = dockerLink.exeCommand(host , port , user , password , javacCommand);
        } catch (JSchException e) {
            e.printStackTrace();
            return "failed";
        } catch (IOException e) {
            e.printStackTrace();
            return "failed";
        }
        System.out.println("javac res:" + javacRes);


        String className = imageName + ".class";

        //创建dockerfile文件
        String dockerfile = "dockerFile";
        String dockerContext = "FROM java:8 \n" +
                "COPY " + className +" " + className + "\n" +
                "ENTRYPOINT [\"java\",\"" + imageName + "\"] \n";
        boolean write_flag = false;
        try{//异常处理
            BufferedWriter bw=new BufferedWriter(new FileWriter(path + dockerfile));
            bw.write(dockerContext);//在创建好的文件中写入代码
            bw.close();//一定要关闭文件
            write_flag = true;
        }catch(IOException e){
            e.printStackTrace();
            return "failed";
        }
        //返回标志
        if(write_flag){
            System.out.println("create dockerfile success!");
        }else{
            System.out.println("create dockerfile failed!");
            return "failed";
        }

        //生成docker image
        String image_command = "cd " + path + "\n"
                + "docker build -f " + dockerfile + " -t " + imageName + " .";
        System.out.println("image_command:");
        System.out.println(image_command);
        String image_res = "";
        try {
            image_res = dockerLink.exeCommand(host , port , user , password , image_command);
        } catch (JSchException e) {
            e.printStackTrace();
            return "failed";
        } catch (IOException e) {
            e.printStackTrace();
            return "failed";
        }
        System.out.println("image res: " + image_res);

        //删除class文件
        String del_command = "cd " + path + "\n" +
                "rm -rf " + className + ".class";
        System.out.println(del_command);
        String del_res = "";
        try {
            del_res = dockerLink.exeCommand(host , port , user , password , del_command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("delete Class res:");
        System.out.println(del_res);

        return "success";

    }

    //执行单个任务的Container
    public String deleteSingleContainer(OutputEntity outputEntity){

        String imageName = outputEntity.getWorkType();

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        String delContainerCommand = "docker rmi -f " + imageName;
        System.out.println("delContainerCommand: " + delContainerCommand);
        String delContainerRes = "";
        try {
            delContainerRes = dockerLink.exeCommand(host , port , user , password , delContainerCommand);
        } catch (JSchException e) {
            e.printStackTrace();
            return "failed";
        } catch (IOException e) {
            e.printStackTrace();
            return "failed";
        }
        System.out.println("delContainerRes: " + delContainerRes);
        return "success";
    }

    //获取真实环境中执行的总时间，能耗，成本
    public String getRealTotal(JSONObject outputJson) {
        System.out.println(outputJson);

        List<OutputEntity> outputEntityList = new ArrayList<>();

        int count = outputJson.getInteger("count");
        for(int i = 0 ; i < count ; i++){
            OutputEntity outputItem = outputJson.getObject(i + "" , OutputEntity.class);
            System.out.println(outputItem);
            outputEntityList.add(outputItem);
        }
        System.out.println("outputEntityList:" + outputEntityList);
        //统计真实环境下的执行总时间和终端设备执行时间和执行总成本
        int realTotalTime = 0;
        int mobileRealTotalTime = 0;
        double realTotalCost = 0;

        for (OutputEntity outputEntity: outputEntityList) {

            Double realTime_item = Double.parseDouble(outputEntity.getRealTime());
            Double realCost_item = Double.parseDouble(outputEntity.getRealCost());

            //计算执行总时间
            realTotalTime += realTime_item;
            //计算执行总成本
            realTotalCost += realCost_item;

            //筛选出终端设备执行时间
            String dataCenterId = outputEntity.getDataCenterId();
            if(dataCenterId.contains("m")){
                mobileRealTotalTime += realTime_item;
            }
        }
        //计算终端执行能耗
        int energy = 1;
        int mobileTotalEnergy = mobileRealTotalTime * energy;
        JSONObject resultThree = new JSONObject();
        resultThree.put("realTotalTime" , realTotalTime);
        resultThree.put("realTotalCost" , realTotalCost);
        resultThree.put("mobileTotalEnergy" , mobileTotalEnergy);

        return resultThree.toJSONString();
    }

    //更新用户的plan
    public String  updatePlan(JSONObject planJson){
        System.out.println(planJson);
        String email = planJson.getString("email");
        JSONObject plan_json = planJson.getJSONObject("plan");
        String planName = plan_json.getString("planName");

        System.out.println("email:" + email);
        System.out.println("plan:" + plan_json);
        System.out.println("planName:" + planName);

        //查询当前用户的所有plan
        String sql_select = "SELECT plan FROM userinfo WHERE email ='" + email + "'";
//        System.out.println("sql_select:" + sql_select);
        String planResult_string =  jdbcTemplate.queryForObject(sql_select,String.class);

        JSONObject planResult_json = JSONObject.parseObject(planResult_string);
        System.out.println(planResult_string);
        System.out.println("keys" + planResult_json.keySet());

        //添加新增的plan
        planResult_json.put(planName, plan_json);

        String sql_update = "update userinfo set plan='" + planResult_json + "' where email='" + email +"'";
        int insert_flag = jdbcTemplate.update(sql_update);
        System.out.println(insert_flag);

        return planResult_json.toJSONString();
    }

    //删除用户的plan
    public String delPlan(JSONObject planJson){
        System.out.println(planJson);
        String email = planJson.getString("email");
        JSONObject plan_json = planJson.getJSONObject("plan");
        String planName = plan_json.getString("planName");

        System.out.println("email:" + email);
        System.out.println("plan:" + plan_json);
        System.out.println("planName:" + planName);

        //查询当前用户的所有plan
        String sql_select = "SELECT plan FROM userinfo WHERE email ='" + email + "'";
        System.out.println("sql_select:" + sql_select);
        String planResult_string =  jdbcTemplate.queryForObject(sql_select,String.class);

        JSONObject planResult_json = JSONObject.parseObject(planResult_string);
        System.out.println(planResult_string);
        System.out.println("keys" + planResult_json.keySet());

        //删除plan
        planResult_json.remove(planName);

        String sql_update = "update userinfo set plan='" + planResult_json + "' where email='" + email +"'";
        int del_flag = jdbcTemplate.update(sql_update);
        System.out.println(del_flag);
        System.out.println("keys" + planResult_json.keySet());

        return planResult_json.toJSONString();
    }

    //获取用户所有的plan
    public String getPlans(String email){
        //查询当前用户的所有plan
        String sql_select = "SELECT plan FROM userinfo WHERE email ='" + email + "'";
        System.out.println("sql_select:" + sql_select);
        String planResult_string =  jdbcTemplate.queryForObject(sql_select,String.class);

        JSONObject planResult_json = JSONObject.parseObject(planResult_string);
        System.out.println(planResult_json);
        return planResult_json.toJSONString();
    }

    //获得Dag图片
    public String getDag(String dagXml , String customXml){
//        System.out.println("dagXml:" + dagXml);
//        System.out.println("customXml:" + customXml);
        String filePath = "";

        List<String> points = new ArrayList<>();
        List<Map<String,String>> links = new ArrayList<>();
        if(customXml.equals("")){
            filePath = xml_path + dagXml;
            points = ParseXML.getPoints(filePath);
            links = ParseXML.getLinks(filePath);
        }else{
            filePath = dagxmlpath + customXml;
            points = ParseXML.getCustomPoints(filePath);
            links = ParseXML.getCustomLinks(filePath);

        }

        // 得到XML文件
//        String filePath = "E:\\workflow\\Montage_20.xml";
//        List<String> points = ParseXML.getPoints(filePath);
//        List<Map<String,String>> links = ParseXML.getLinks(filePath);
//        System.out.println("points: " + points);
//        System.out.println("links:" + links);

        DagGraphUtil dagGraphUtil = new DagGraphUtil(points, links);

//        System.out.println(dagGraphUtil.drawDagGraph());
//        System.out.println(dagGraphUtil.drawDagGraph().toString());

        JSONObject jsonObject = new JSONObject(dagGraphUtil.drawDagGraph());
//        System.out.println(jsonObject.toJSONString());
        return jsonObject.toJSONString();


    }


    /**
     *执行用户提交代码
     */
    /* 客户端发来的程序的运行时间限制 */
    private static final int RUN_TIME_LIMITED = 15;

    /* N_THREAD = N_CPU + 1，因为是 CPU 密集型的操作 */
    private static final int N_THREAD = 5;

    /* 负责执行客户端代码的线程池，根据《Java 开发手册》不可用 Executor 创建，有 OOM 的可能 */
    private static final ExecutorService pool = new ThreadPoolExecutor(N_THREAD, N_THREAD,
            0L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(N_THREAD));

    private static final String WAIT_WARNING = "服务器忙，请稍后提交";
    private static final String NO_OUTPUT = "Nothing.";

    public String execute(String source, String systemIn) {
        String sourceList[] = source.split("\n");
        String className = sourceList[0].split(" ")[2];
        System.out.println("javaName:" + className);

        String path = "/home/dr/Music/";
        System.out.println("submitCode:" + path);
        File file = new File(path);
        if(!file.exists()){//如果文件夹不存在
            file.mkdirs();//创建文件夹
        }

        String javaName = className + ".java";
        String code = source;
        boolean write_flag = false;
        try{//异常处理
            //如果Qiju_Li文件夹下没有Qiju_Li.txt就会创建该文件
            BufferedWriter bw=new BufferedWriter(new FileWriter(path + javaName));
            bw.write(code);//在创建好的文件中写入代码
            bw.close();//一定要关闭文件
            write_flag = true;
        }catch(IOException e){
            e.printStackTrace();
        }

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        //编译java文件,生成class文件
        String javacCommand = "cd " + path + "\n" +
                "javac " + javaName;
        System.out.println(javacCommand);
        String javacRes = "";
        try {
            javacRes = dockerLink.exeCommand(host , port , user , password , javacCommand);
        } catch (JSchException e) {
            System.out.println(e);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }
        System.out.println("javac res:");
        System.out.println(javacRes);

        if (javacRes.equals("")){
            //执行java文件生成的class文件
            String args = "";
            for (int i = 0; i < inputEdges; i++) {
                args += " test" + i;
            }
            String javaCommand = "cd " + path + "\n" +
                    "java " + className + args;
            System.out.println(javaCommand);
            String javaRes = "";
            try {
                javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("java res:");
            System.out.println(javaRes);
            /*if (inputEdges == 0){
                System.out.println("className: " + className);
                codeExecuteRes.put(className , javaRes);
                System.out.println("codeExecuteRes： " + codeExecuteRes);
            }*/

            //删除 class文件
            String deleteCommand = "cd " + path + "\n" +
                    "rm *.*";
            System.out.println(deleteCommand);
            String deleteRes = "";
            try {
                javacRes = dockerLink.exeCommand(host , port , user , password , deleteCommand);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("delete res:");
            System.out.println(deleteRes);


            return javaRes;

        }else{
            System.out.println(javacRes);
            return javacRes;
        }



        /*DiagnosticCollector<JavaFileObject> compileCollector = new DiagnosticCollector<>(); // 编译结果收集器

        // 编译源代码
        byte[] classBytes = StringSourceCompiler.compile(source, compileCollector);

        // 编译不通过，获取并返回编译错误信息
        if (classBytes == null) {
            // 获取编译错误信息
            List<Diagnostic<? extends JavaFileObject>> compileError = compileCollector.getDiagnostics();
            StringBuilder compileErrorRes = new StringBuilder();
            for (Diagnostic diagnostic : compileError) {
                compileErrorRes.append("Compilation error at ");
                compileErrorRes.append(diagnostic.getLineNumber());
                compileErrorRes.append(".");
                compileErrorRes.append(System.lineSeparator());
            }
            return compileErrorRes.toString();
        }

        // 运行字节码的main方法
        Callable<String> runTask = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return JavaClassExecutor.execute(classBytes, systemIn);
            }
        };

        Future<String> res = null;
        try {
            res = pool.submit(runTask);
        } catch (RejectedExecutionException e) {
            return WAIT_WARNING;
        }

        // 获取运行结果，处理非客户端代码错误
        String runResult;
        try {
            runResult = res.get(RUN_TIME_LIMITED, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            runResult = "Program interrupted.";
        } catch (ExecutionException e) {
            runResult = e.getCause().getMessage();
        } catch (TimeoutException e) {
            runResult = "Time Limit Exceeded.";
        } finally {
            res.cancel(true);
        }
        return runResult != null ? runResult : NO_OUTPUT;*/

    }

    //存储用户提交的代码
    public String submitCode(String codeJson){

        System.out.println(codeJson);
        JSONObject jsonObject = JSON.parseObject(codeJson);

        String username = jsonObject.getString("username");
        String planName = jsonObject.getString("planName");
        String workflowName = jsonObject.getString("workflowName");
        String taskName = jsonObject.getString("taskName");
        String code = jsonObject.getString("code");

//        System.out.println("username:" + username);
//        System.out.println("planName:" + planName);
//        System.out.println("workflowName:" + workflowName);
//        System.out.println("taskName:" + taskName);
//        System.out.println("code:" + code);

        //校验代码
        String executeRes = this.execute(code , "");
        System.out.println("executeRes:" + executeRes);
        if(executeRes.equals("")){
            return "failed";
        }
        String resList[] = executeRes.split("\n");
        int outputCount = resList.length;
        System.out.println(outputCount);
        boolean write_flag = false;
        //如果输出参数数量符合要求，即生成java文件，反之，不生成。
        if((outputEdges == outputCount) || ((outputCount == 1) &&(outputEdges == 0))){
            //path--> 盘符\\用户名\\执行方案名\\工作流名称\\任务代码.java
            //String path = "F:\\" + username + "\\" + planName + "\\" + workflowName + "\\";
            String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
            System.out.println("submitCode:" + path);
            File file = new File(path);
            if(!file.exists()){//如果文件夹不存在
                file.mkdirs();//创建文件夹
            }

            String javaName = taskName.toLowerCase() + ".java";


            try{//异常处理
                //如果Qiju_Li文件夹下没有Qiju_Li.txt就会创建该文件
                BufferedWriter bw=new BufferedWriter(new FileWriter(path + javaName));
                bw.write(code);//在创建好的文件中写入代码
                bw.close();//一定要关闭文件
                write_flag = true;
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        //返回插入标志
        if(write_flag){
            return "success";
        }else{
            return "failed";
        }
    }

    //获取工作流中DAG图已编辑任务节点的所有节点名称
    public String getTaskList(String taskListParam){
//        System.out.println(taskListParam);
        JSONObject jsonObject = JSON.parseObject(taskListParam);

        String username = jsonObject.getString("username");
        String planName = jsonObject.getString("planName");
        String workflowName = jsonObject.getString("workflowName");

        //path--> 盘符\\用户名\\执行方案名\\工作流名称\\任务代码.java
//        String path = "F:\\" + username + "\\" + planName + "\\" + workflowName + "\\";
        String path = storagePath + "/" + username + "/" + planName + "/" + workflowName + "/";
        File file = new File(path);

        if(!file.exists()){//如果文件夹不存在
            file.mkdirs();//创建文件夹
        }
        //读取所有文件名
        String nameList[] = file.list();
        for (int i = 0; i < nameList.length; i++) {
//            nameList[i] = ;
            String temp = nameList[i].replaceAll("task_" , "").replaceAll(".java" , "");
            nameList[i] = temp;
        }

        JSONObject result = new JSONObject();
        result.put("taskList" , nameList);
//        System.out.println(result);
        return result.toJSONString();
    }

    /**
     * 验证用户提交代码
     */
    private  String defaultSource = "public class Run {\n"
            + "    public static void main(String[] args) {\n"
            + "        \n"
            + "    }\n"
            + "}";

    //初始化代碼
    public String initCode(String taskName , String workflowName){
        String className = "task_" + taskName.toLowerCase();
        String code_model = defaultSource.replace("Run",className);
//        System.out.println(code_model)

        dagLinks.clear();

//        String filePath = storagePath + "dax/" + "Montage_20.xml";
        String filePath = dagxmlpath + workflowName;
        dagLinks = ParseXML.getCustomLinks(filePath);

//        String filePath = storagePath + "dagXML/" + "line3_3.xml";
//        links = ParseXML.getCustomLinks(filePath);
        String itemId = taskName;
        inputEdges = 0;
        outputEdges = 0;
        for (int i = 0; i < dagLinks.size(); i++) {
            Map<String , String> linkmap = dagLinks.get(i);
            String source = linkmap.get("source");
            String target = linkmap.get("target");
//            System.out.println("source:" + source);
//            System.out.println("target:" + target);
            if (source.equals(itemId)){
                outputEdges++;
            }
            if (target.equals(itemId)){
                inputEdges++;
            }

        }
//        System.out.println("links:" + dagLinks);
//        System.out.println("input:" + inputEdges);
//        System.out.println("output:" + outputEdges);

        if ((inputEdges == 0) && (outputEdges == 0)){
            code_model = defaultSource.replace("Run",className);
        }else{

            String customSource = "";
            customSource += "public class Run {\n";
            customSource += "    public static void main(String[] args) {\n";
            customSource += "        \n";

            if(inputEdges > 0){
                for (int i = 0; i < inputEdges; i++) {
                    customSource += "        String args" + i +" = args[" + i + "];\n";
                }
                customSource += "        \n";
            }

            if(outputEdges > 0){
                for (int i = 0; i < outputEdges; i++) {
                    customSource += "        String elem" + i +" = \"elem" + i + "\";\n";
                }
                for (int i = 0; i < outputEdges; i++) {
                    customSource += "        System.out.println(elem" + i +");\n";
                }
                customSource += "        \n";
            }

            if(outputEdges == 0){
                    customSource += "        String elem" + 0 +" = \"elem" + 0 + "\";\n";
                    customSource += "        System.out.println(elem" + 0 +");\n";
                customSource += "        \n";
            }
            customSource += "    }\n";
            customSource += "}";
            code_model = customSource.replace("Run",className);
        }



        return code_model;
    }


    //初始化已有代码
    public String initExitCode(String codeJson){
        JSONObject jsonObject = JSON.parseObject(codeJson);

        String username = jsonObject.getString("username");
        String planName = jsonObject.getString("planName");
        String workflowName = jsonObject.getString("workflowName");
        String taskName = jsonObject.getString("taskName");
        String fileName = "task_" + taskName + ".java";

        dagLinks.clear();
//        String filePath = storagePath + "dax/" + "Montage_20.xml";
        String filePath = dagxmlpath + workflowName;
        dagLinks = ParseXML.getCustomLinks(filePath);
        String itemId = taskName;
        inputEdges = 0;
        outputEdges = 0;
        for (int i = 0; i < dagLinks.size(); i++) {
            Map<String , String> linkmap = dagLinks.get(i);
            String source = linkmap.get("source");
            String target = linkmap.get("target");
//            System.out.println("source:" + source);
//            System.out.println("target:" + target);
            if (source.equals(itemId)){
                outputEdges++;
            }
            if (target.equals(itemId)){
                inputEdges++;
            }

        }
//        System.out.println("links:" + dagLinks);
//        System.out.println("input:" + inputEdges);
//        System.out.println("output:" + outputEdges);

        //path--> 盘符\\用户名\\执行方案名\\工作流名称\\任务代码.java
//        String path = "F:\\" + username + "\\" + planName + "\\" + workflowName + "\\" + fileName;
        String path = storagePath + "/" + username + "/" + planName + "/" + workflowName + "/" + fileName;
//        System.out.println(path);
        File file = new File(path);

        if(!file.exists()){//如果文件夹不存在
            file.mkdirs();//创建文件夹
        }
        //读取文本中内内容
        BufferedReader bufferedReader = null;
        StringBuffer stringBuffer = new StringBuffer();
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String content;
            while ((content = bufferedReader.readLine()) != null) {
                stringBuffer.append(content).append("\r\n");
            }
            bufferedReader.close();
            return stringBuffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
//        System.out.println(stringBuffer.toString());
        return stringBuffer.toString();
    }

    //生成容器测试
    /*public String createContainer(String containerParam){
        //参数
        System.out.println(containerParam);
        JSONObject jsonObject = JSON.parseObject(containerParam);

        String username = jsonObject.getString("username");
        String planName = jsonObject.getString("planName");
        String workflowName = jsonObject.getString("workflowName");

        System.out.println(username);
        System.out.println(planName);
        System.out.println(workflowName);


        //path--> 盘符\\用户名\\执行方案名\\工作流名称\\任务代码.java
        //String path = "F:\\" + username + "\\" + planName + "\\" + workflowName + "\\";
        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
        System.out.println("path:" + path);

        File file = new File(path);
        if(!file.exists()){//如果文件夹不存在
            file.mkdirs();//创建文件夹
        }
        //连接docker
        DockerLink dockerLink = new DockerLink();

        String host =  "127.0.0.1";
        int port = 22;
        String user = "root";
        String password = "root";

        String nameList[] = file.list();
        for (int i = 0; i < nameList.length; i++) {
            System.out.println(nameList[i]);
        }

        //编译java文件,生成class文件
        String command = "cd " + path + "\n" +
                "javac " + "*.java";
        System.out.println(command);
        String res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("javac res:");
        System.out.println(res);

        //创建dockerfile文件
        String className = nameList[0].replace("java" , "class");
        String imageName = nameList[0].replace(".java" , "").toLowerCase();
        String dockerfile = "dockerFile";
        String dockerContext = "FROM java:8 \n" +
                "COPY " + className +" " + className + "\n" +
                "ENTRYPOINT [\"java\",\"" + imageName + "\"] \n";
        boolean write_flag = false;
        try{//异常处理
            BufferedWriter bw=new BufferedWriter(new FileWriter(path + dockerfile));
            bw.write(dockerContext);//在创建好的文件中写入代码
            bw.close();//一定要关闭文件
            write_flag = true;
        }catch(IOException e){
            e.printStackTrace();
        }
        //返回插入标志
        if(write_flag){
            System.out.println("create dockerfile success!");
        }else{
            System.out.println("create dockerfile failed!");
        }

        //生成docker image
        String image_command = "cd " + path + "\n"
                + "docker build -f " + dockerfile + " -t " + imageName + " .";
        System.out.println("image_command:");
        System.out.println(image_command);
        String image_res = "";
        try {
            image_res = dockerLink.exeCommand(host , port , user , password , image_command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("image res:");
        System.out.println(image_res);
        return "success";
    }*/

    public String createContainer(String containerParam){
        //获取参数
        System.out.println(containerParam);
        JSONObject jsonObject = JSON.parseObject(containerParam);

        String username = jsonObject.getString("username");
        String planName = jsonObject.getString("planName");
        String workflowName = jsonObject.getString("workflowName");

        System.out.println(username);
        System.out.println(planName);
        System.out.println(workflowName);

        //path--> 盘符\\用户名\\执行方案名\\工作流名称\\任务代码.java
        //String path = "F:\\" + username + "\\" + planName + "\\" + workflowName + "\\";
        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
        System.out.println("path:" + path);

        File file = new File(path);
        if(!file.exists()){//如果文件夹不存在
            file.mkdirs();//创建文件夹
        }

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        //编译java文件,生成class文件
        String command = "cd " + path + "\n" +
                "javac " + "*.java";
        System.out.println(command);
        String res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("javac res:");
        System.out.println(res);

        //获得所有文件,筛选出class文件
        String fileList[] = file.list();
        List<String> classList = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            String classFile = fileList[i];
            if(classFile.endsWith(".class")){
                classList.add(classFile);
            }
        }
        workTypeToImage.clear();
        List<String> points = new ArrayList<>();
        points = ParseXML.getCustomPoints(dagxmlpath + workflowName);
        for (int i = 0; i < points.size(); i++) {
            String className = "task_" + points.get(i).toLowerCase() + ".class";

            //创建dockerfile文件
            String imageName = className.replace(".class" , "").toLowerCase();
            String dockerfile = "dockerFile";
            String dockerContext = "FROM java:8 \n" +
                    "COPY " + className +" " + className + "\n" +
                    "ENTRYPOINT [\"java\",\"" + imageName + "\"] \n";
            boolean write_flag = false;
            try{//异常处理
                BufferedWriter bw=new BufferedWriter(new FileWriter(path + dockerfile));
                bw.write(dockerContext);//在创建好的文件中写入代码
                bw.close();//一定要关闭文件
                write_flag = true;
            }catch(IOException e){
                e.printStackTrace();
            }
            //返回标志
            if(write_flag){
                System.out.println("create dockerfile success!");
            }else{
                System.out.println("create dockerfile failed!");
            }

            //生成docker image
            String image_command = "cd " + path + "\n"
                    + "docker build -f " + dockerfile + " -t " + imageName + " .";
            System.out.println("image_command:");
            System.out.println(image_command);
            String image_res = "";
            try {
                image_res = dockerLink.exeCommand(host , port , user , password , image_command);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("image res:");
            System.out.println(image_res);
            workTypeToImage.put(i + "" , imageName);
        }
//        System.out.println("workTypeToImage:" + workTypeToImage);
        //生成容器镜像
        /*for (String className : classList) {
            System.out.println(className);

            //创建dockerfile文件
            String imageName = className.replace(".class" , "").toLowerCase();
            String dockerfile = "dockerFile";
            String dockerContext = "FROM java:8 \n" +
                    "COPY " + className +" " + className + "\n" +
                    "ENTRYPOINT [\"java\",\"" + imageName + "\"] \n";
            boolean write_flag = false;
            try{//异常处理
                BufferedWriter bw=new BufferedWriter(new FileWriter(path + dockerfile));
                bw.write(dockerContext);//在创建好的文件中写入代码
                bw.close();//一定要关闭文件
                write_flag = true;
            }catch(IOException e){
                e.printStackTrace();
            }
            //返回标志
            if(write_flag){
                System.out.println("create dockerfile success!");
            }else{
                System.out.println("create dockerfile failed!");
            }

            //生成docker image
            String image_command = "cd " + path + "\n"
                    + "docker build -f " + dockerfile + " -t " + imageName + " .";
            System.out.println("image_command:");
            System.out.println(image_command);
            String image_res = "";
            try {
                image_res = dockerLink.exeCommand(host , port , user , password , image_command);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("image res:");
            System.out.println(image_res);
        }*/

        //删除class文件
        String del_command = "cd " + path + "\n" +
                "rm -rf " + "*.class";
        System.out.println(del_command);
        String del_res = "";
        try {
            del_res = dockerLink.exeCommand(host , port , user , password , del_command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("delete Class res:");
        System.out.println(del_res);

        return "success";
    }

    //容器仿真
    public String containerSimulation(String json){

        System.out.println("json:" + json);

        JSONObject containerParam = JSON.parseObject(json);
        String assignType = containerParam.getString("assignType");
        String workflowPathParam = containerParam.getString("workflowPathParam");

        JSONObject workflowPath = JSON.parseObject(workflowPathParam);
        String username = workflowPath.getString("username");
        String planName = workflowPath.getString("planName");
        String workflowName = workflowPath.getString("workflowName");
        String custom = containerParam.getString("custom");

        System.out.println("assignType:" + assignType);
        System.out.println("username:" + username);
        System.out.println("planName:" + planName);
        System.out.println("workflowName:" + workflowName);
        System.out.println("custom:" + custom);

        customType = assignType;
        System.out.println("customType: " + customType);
        if(!custom.equals("")){
            workflowName = custom;
        }

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
        System.out.println("path:" + path);

        //获得所有文件,筛选出java文件
        File file = new File(path);
        String fileList[] = file.list();
        List<String> javaList = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            String javaFile = fileList[i];
            if(javaFile.endsWith(".java")){
                javaList.add(javaFile);
            }
        }
        Collections.sort(javaList);
        System.out.println(javaList);

        //编译java文件,生成class文件
        String command = "cd " + path + "\n" +
                "javac " + "*.java";
        System.out.println(command);
        String res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("javac res:");
        System.out.println(res);

        //获得所有文件,筛选出class文件
        List<String> classList = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            String classFile = fileList[i];
            if(classFile.endsWith(".class")){
                classList.add(classFile);
            }
        }

        //执行每个class文件，保存执行时间
        Map<String , String> executeTime = new HashMap<String , String>();
        for (int i = 0; i < javaList.size(); i++) {
            String javaItem = javaList.get(i).toString().replace(".java" , "");
            command = "cd " + path + "\n" +
                    "java " + javaItem + "";
            System.out.println(command);
            res = "";

            long startTime = System.currentTimeMillis();
            try {
                res = dockerLink.exeCommand(host , port , user , password , command);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            long endTime = System.currentTimeMillis();
            float seconds = (endTime - startTime) / 1000F;

            executeTime.put(javaItem , seconds + "");
            System.out.println(Float.toString(seconds) + " seconds.");
        }
        System.out.println(executeTime);

        //复制临时xml文件
        String tempXmlPath = storagePath + "tempXml/";
        file = new File(tempXmlPath);
        if(!file.exists()){//如果文件夹不存在
            file.mkdir();//创建文件夹
        }

        String copyCommand = "cp" + " " + dagxmlpath + workflowName + " " + tempXmlPath;
        System.out.println("copyCommand:" + copyCommand);
        res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , copyCommand);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //更新DAG xml的runtime属性
//        String tempXmlPath = "/home/dr/sim/" + "tempXml/";
        try {
            String domPath = tempXmlPath + workflowName;
            SAXBuilder builder = new SAXBuilder();
            //parse using builder to get DOM representation of the XML file
            Document dom = builder.build(domPath);
            Element root = dom.getRootElement();
            List<Element> domItemList = root.getChildren();
            System.out.println("domItemList:" + domItemList);
            for (Element node : domItemList) {
                switch (node.getName().toLowerCase()) {
                    case "job":
                        String id = node.getAttributeValue("id");
//                        String id_int = id.replace("ID" , "");
                        String name = node.getAttributeValue("name");
                        String runtime = node.getAttributeValue("runtime");
                        System.out.println("id:" + id);
                        System.out.println("name:" + name);
                        System.out.println("runtime:" + runtime);

                        runtime = executeTime.get("task_"+ id.toLowerCase());
                        System.out.println("executeTimeItem:" + runtime);
                        node.setAttribute("runtime" , runtime);

                        XMLOutputter  outputter = new XMLOutputter();
                        Format format = outputter.getFormat();
                        format.setEncoding("GBK");
                        format.setIndent("    ");
                        outputter.setFormat(format);
                        outputter.output(dom , new FileWriter(tempXmlPath + "temp.xml"));

                        break;

                }
            }

            //更改xml runtime属性

        }catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Parsing Exception");
        }

        //得到工作流执行的结果
        Map<String , String> codeResult = dagCodeRunRes(username , planName , workflowName);
        System.out.println("codeResult:" + codeResult);

        //得到工作流的结构
        String dagRelations = getDag("" ,  workflowName);

        //运行容器镜像
        /*System.out.println("-----------------------------------------");
        for (String javaName : javaList) {
            System.out.println(javaName);

            //获得image名称
            String imageName = javaName.replace(".java" , "").toLowerCase();
            //执行容器命令
            String command = "docker run " + imageName;
            System.out.println(command);

            String res = "";
            try {
                res = dockerLink.exeCommand(host , port , user , password , command);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("command res:");
            System.out.println(res);

        }*/

        //删除容器镜像
        /*System.out.println("-----------------------------------------");
        for (String javaName : javaList) {
            System.out.println(javaName);

            //获得image名称
            String imageName = javaName.replace(".java" , "").toLowerCase();
            //执行容器命令
            String command = "docker rmi -f " + imageName;
            System.out.println(command);

            String res = "";
            try {
                res = dockerLink.exeCommand(host , port , user , password , command);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("command res:");
            System.out.println(res);

        }*/

        //删除所有镜像
        /*System.out.println("-----------------------------------------");
        String command = "docker rm -f $(docker ps -aq)";
        System.out.println(command);

        String res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("command res:");
        System.out.println(res);*/



        //开始仿真
//        String json_string = "{\"nodeSize\":\"20\",\"daxPath\":\"Montage_20.xml\",\"planName\":\"example1\",\"custom\":\"\",\"pi\":[],\"kmp\":[\"0\",\"1\",\"2\",\"3\",\"4\",\"5\",\"6\",\"7\",\"8\",\"9\",\"10\",\"11\",\"12\",\"13\",\"14\",\"15\",\"16\",\"17\",\"18\",\"19\",\"20\"],\"levenshtein\":[],\"selectsort\":[],\"cloudServer\":1,\"fogServer\":1,\"mobile\":1,\"setting_json\":{\"cloud_mips_list\":[1600],\"cloud_cost_list\":[0.96],\"fog_mips_list\":[1300],\"fog_cost_list\":[0.48],\"mobile_mips_list\":[1000],\"cloud_number\":1,\"fog_number\":1,\"mobile_number\":1,\"GA\":{\"GA-popsize\":20,\"GA-gmax\":100,\"GA-crossoverProb\":0.8,\"GA-mutationRate\":0.01,\"GA-repeat\":1},\"PSO\":{\"PSO-particleNum\":20,\"PSO-iterateNum\":100,\"PSO-c1\":1.37,\"PSO-c2\":1.37,\"PSO-w\":0.73,\"PSO-repeat\":1}},\"strategy\":\"\",\"alSet\":[\"MINMIN\",\"MAXMIN\",\"FCFS\",\"ROUNDROBIN\"],\"optimize_objective\":\"Time\",\"deadline\":\"\",\"editTime\":\"2020-11-06 10:42:01\"}";
        String json_string = "{\"nodeSize\":\"20\",\"daxPath\":\"Montage_20.xml\",\"planName\":\"a\",\"custom\":\"\",\"assignType\":\"custom\",\"workflowPathParam\":{\"username\":\"me\",\"planName\":\"a\",\"workflowName\":\"Montage_20.xml\"},\"cloudServer\":1,\"fogServer\":1,\"mobile\":1,\"setting_json\":{\"cloud_mips_list\":[1600],\"cloud_cost_list\":[0.96],\"fog_mips_list\":[1300],\"fog_cost_list\":[0.48],\"mobile_mips_list\":[1000],\"cloud_number\":1,\"fog_number\":1,\"mobile_number\":1,\"GA\":{\"GA-popsize\":20,\"GA-gmax\":100,\"GA-crossoverProb\":0.8,\"GA-mutationRate\":0.01,\"GA-repeat\":1},\"PSO\":{\"PSO-particleNum\":20,\"PSO-iterateNum\":100,\"PSO-c1\":1.37,\"PSO-c2\":1.37,\"PSO-w\":0.73,\"PSO-repeat\":1}},\"strategy\":\"\",\"alSet\":[\"MINMIN\"],\"optimize_objective\":\"Time\",\"deadline\":\"\",\"editTime\":\"2021-01-23 12:17:19\"}";
        json_string = json;
        JSONObject json1 = JSON.parseObject(json_string);
        System.out.println(json1);

        outputMap.clear();
        containerSimParseParams(json1);
        record.clear();
        pso_time = 0;
        ga_time = 0;
        for (String method : alList) {
            scheduler_method = method;
            if (scheduler_method.equals("PSO")) {
                getpsosetting(wfEngine);
                int repeat = Math.max(1, pso_repeat);
                List<Double[]> repeats = new ArrayList<>();
                List<Long> times = new ArrayList<>();
                for(int i = 0; i < repeat; i++){
                    System.out.println("---------------------------For the "+(i+1)+" pso--------------------------");
                    long time = StartAlgorithm(scheduler_method+i);
                    repeats.add(record.get((record.size()-1)));
                    record.remove(record.size()-1);
                    times.add(time);
                }
                Double[] mean = GetMean(repeats);
                Double[] algomean = new Double[4];
                algomean[0] = getAlgorithm(scheduler_method);System.out.println(scheduler_method+":");
                algomean[1] = mean[0];System.out.println("Average task execution time = "+mean[0]);
                algomean[2] = mean[1];System.out.println("Average energy consumption = "+mean[1]);
                algomean[3] = mean[2];System.out.println("Average cost = "+mean[2]);
                record.add(algomean);
                if(wfEngine.getoffloadingEngine().getOffloadingStrategy() != null)
                    System.out.println("Average offloading Strategy time = " + wfEngine.getAverageOffloadingTime());
                long averageTime = GetAverageTime(times);
                times=null;
                System.out.println("Average "+scheduler_method+" algorithm execution time = " + averageTime);
//                displayTime(averageTime);
                pso_time = averageTime;
                System.out.println("Drawing "+scheduler_method+" iteration figure......");
            } else if (scheduler_method.equals("GA")) {
                getgasetting(wfEngine);
                int repeat = Math.max(1, ga_repeat);
                List<Double[]> repeats = new ArrayList<Double[]>();
                List<Long> times = new ArrayList<Long>();
                for (int i = 0; i < repeat; i++) {
                    System.out.println("---------------------------For the " + (i + 1) + " ga--------------------------");
                    long time = StartAlgorithm(scheduler_method+i);
                    repeats.add(record.get((record.size() - 1)));
                    record.remove(record.size() - 1);
                    times.add(time);
                }
                //TODO:这块是干啥的?
                Double[] mean = GetMean(repeats);
                repeats = null;
                Double[] algomean = new Double[4];
                algomean[0] = getAlgorithm(scheduler_method);
                System.out.println(scheduler_method + ":");
                algomean[1] = mean[0];
                System.out.println("Average task execution time = " + mean[0]);
                algomean[2] = mean[1];
                System.out.println("Average energy consumption = " + mean[1]);
                algomean[3] = mean[2];
                System.out.println("Average cost = " + mean[2]);
                record.add(algomean);
                if (wfEngine.getoffloadingEngine().getOffloadingStrategy() != null)
                    System.out.println("Average offloading Strategy time = " + wfEngine.getAverageOffloadingTime());
                long averageTime = GetAverageTime(times);
                times = null;
                System.out.println("Average " + scheduler_method + " algorithm execution time = " + averageTime);
//                displayTime(averageTime);
                ga_time = averageTime;
                System.out.println("Drawing " + scheduler_method + " iteration figure......");
            } else {
                StartAlgorithm(scheduler_method);
            }
        }

        JSONObject retJson = new JSONObject();
        retJson.put("outputMap", outputMap);
        retJson.put("x", wfEngine.iterateNum);
        retJson.put("y", wfEngine.updatebest);
        retJson.put("record", record);
        retJson.put("pso_time", pso_time);
        retJson.put("ga_time", ga_time);

        retJson.put("mobileNumber", mobileNumber);
        retJson.put("fogNumber", fogNumber);
        retJson.put("cloudNumber", cloudNumber);

        retJson.put("codeExecuteElems" , codeExecuteElems);
        retJson.put("codeResult" , codeResult);
        retJson.put("dagRelations" , dagRelations);
        dockerLink.setRealWorkLoad();
//        System.out.println("workTypeToImage" + workTypeToImage);

        System.out.println("retJson:" + retJson);

        //删除class文件
        deleteClass(path);

        //删除容器镜像
//        deleteContainer(path);

        return retJson.toJSONString();
    }

    //删除class文件
     private void deleteClass(String path){

         //连接docker
         DockerLink dockerLink = new DockerLink();
         String host =  "127.0.0.1";
         int port = 22;
         String user = "dr";
         String password = "dr";

        //获得所有文件,筛选出class文件
         File file = new File(path);
         String fileList[] = file.list();
         List<String> classList = new ArrayList<String>();
         for (int i = 0; i < fileList.length; i++) {
             String classFile = fileList[i];
             if(classFile.endsWith(".class")){
                 classList.add(classFile);
             }
         }
         //删除class文件
         String delClassCommand = "cd " + path + "\n" +
                 "rm -rf " + "*.class";
         System.out.println(delClassCommand);
         String delClassRes = "";
         try {
             delClassRes = dockerLink.exeCommand(host , port , user , password , delClassCommand);
         } catch (JSchException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
         System.out.println("delete Class res:");
         System.out.println(delClassRes);
     }

     //删除容器和镜像
     private void deleteContainer(String path){
        //连接docker
         DockerLink dockerLink = new DockerLink();
         String host =  "127.0.0.1";
         int port = 22;
         String user = "dr";
         String password = "dr";

        //获得所有文件,筛选出java文件
        File file = new File(path);
        String fileList[] = file.list();
        List<String> javaList = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            String javaFile = fileList[i];
            if(javaFile.endsWith(".java")){
                javaList.add(javaFile);
            }
        }
        Collections.sort(javaList);
        System.out.println(javaList);

        String containerNames = "";
         for (String javaName : javaList) {
             //获得container名称
             String containerName = javaName.replace(".java" , "").toLowerCase();
             containerNames += containerName + " ";

         }
         containerNames = containerNames.substring(0 , containerNames.length() - 1);
         System.out.println("containerNames:" + containerNames);

         //执行容器命令
         String delContainerCommand = "docker rmi -f " + containerNames;
         System.out.println(delContainerCommand);

         String delContainerRes = "";
         try {
             delContainerRes = dockerLink.exeCommand(host , port , user , password , delContainerCommand);
         } catch (JSchException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
         System.out.println("delContainerCommand res:");
         System.out.println(delContainerRes);

         //删除镜像
         String delImageCommand = "docker rm -f $(docker ps -aq)";
         System.out.println(delImageCommand);

         String delImageRes = "";
         try {
             delImageRes = dockerLink.exeCommand(host , port , user , password , delImageCommand);
         } catch (JSchException e) {
             e.printStackTrace();
         } catch (IOException e) {
             e.printStackTrace();
         }
         System.out.println("delImageCommand res:");
         System.out.println(delImageRes);
     }

    //容器执行过程中参数解析
    private void containerSimParseParams(JSONObject json) {
        //主界面设置
        strategy = json.getString("strategy");
        optimize_objective = json.getString("optimize_objective");
        customPath = json.getString("custom");

        String workflowPathParam_string = json.getString("workflowPathParam");
        JSONObject workflowPathParam = JSON.parseObject(workflowPathParam_string);
        String workflowName = workflowPathParam.getString("workflowName");

        daxPath = storagePath + "tempXml/" + "temp.xml";
        System.out.println("daxPath:" + daxPath);
        /*if (customPath == null || customPath.equals("")) {
            daxPath = storagePath + json.getString("daxPath");
        } else {
//            daxPath = xml_path + customPath;
//            daxPath = "E:\\dagXML\\" + customPath;
//            System.out.println(dagxmlpath +"llllllllllllll");
            daxPath = dagxmlpath +customPath;
            System.out.println(daxPath +"llllllllllllll");
        }*/
        nodeSize = json.getInteger("nodeSize");
        deadlineString = json.getString("deadline");
        JSONArray arr = json.getJSONArray("alSet");
        alList.clear();
        for (int i=0; i<arr.size(); i++) {
            alList.add(arr.getString(i));
        }

        initFogSetting(json.getJSONObject("setting_json"));
        initAlSetting(json.getJSONObject("setting_json"));

    }

    //执行工作流代码，返回结果
    private Map<String , String> dagCodeRunRes(String username , String planName , String workflowName){
//        String assignType = "custom";
//        username = "me";
//        planName = "aa";
//        workflowName = "Montage_20.xml";

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
//        System.out.println("path:" + path);

        List<Map<String,String>> links = new ArrayList<>();
        List<String> points = new ArrayList<>();
        codeExecuteRes.clear();
        dagLinks.clear();
        codeExecuteElems.clear();
//        String filePath = storagePath + "dax/" + "Montage_20.xml";
        String filePath = dagxmlpath + workflowName;
        links = ParseXML.getCustomLinks(filePath);
        points = ParseXML.getCustomPoints(filePath);
        dagLinks = links;


//        String filePath = storagePath + "dagXML/" + "line3_3.xml";
//        links = ParseXML.getCustomLinks(filePath);
        /*String itemId = "0";
        int input = 0;
        int output = 0;
        for (int i = 0; i < links.size(); i++) {
            Map<String , String> linkmap = links.get(i);
            String source = linkmap.get("source");
            String target = linkmap.get("target");
//            System.out.println("source:" + source);
//            System.out.println("target:" + target);
            if (source.equals(itemId)){
                output++;
            }
            if (target.equals(itemId)){
                input++;
            }

        }*/
//        System.out.println("links:" + links);
//        System.out.println("input:" + input);
//        System.out.println("output:" + output);
//        System.out.println(points);

        DagGraphUtil dagGraphUtil = new DagGraphUtil(points, links);
        JSONObject jsonObject = new JSONObject(dagGraphUtil.drawDagGraph());

        //point集合
        List<Map<String,Integer>> points_map = new ArrayList<>();
        points_map = (List)jsonObject.get("points");

        //所有x坐标
        ArrayList<Integer> x_arrays = new ArrayList<>();

        //每个任务的运行结果
        Map<String , String> codeResult = new HashMap<String , String>();

        //先编译java文件
        String javacCommand = "cd " + path + "\n" +
                "javac *.java";

//        System.out.println(javacCommand);
        String javacRes = "";

        try {
            javacRes = dockerLink.exeCommand(host , port , user , password , javacCommand);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String itemId = "";
        int input = 0;
        int output = 0;
//        System.out.println(links);
//        System.out.println(points_map);
        workTypeToImage.clear();
        for (int i = 0; i < points_map.size(); i++) {
            Map<String , Integer> map = points_map.get(i);
            Set<String> keys = map.keySet();
            String name = "";

            //获得当前节点的name属性
            for (String key: keys) {
                if(key.equals("name")){
                    name = (map.get(key) + "").toLowerCase();
                }

                if(key.equals("x")) {
                    x_arrays.add(map.get(key));
                }
            }

            workTypeToImage.put(i + "" , "task_" + name);
            //获得当前节点的输入参数数量
            itemId = name;
            input = 0;
            output = 0;
            for (int j = 0; j < links.size(); j++) {
                Map<String , String> linkmap = links.get(j);
                String source = linkmap.get("source").toLowerCase();
                String target = linkmap.get("target").toLowerCase();
                if (source.equals(itemId)){
                    output++;
                }
                if (target.equals(itemId)){
                    input++;
                }

            }
//            System.out.println("input:" + input);


            //没有输入参数input==0
            if (input == 0){
                String javaCommand = "cd " + path + "\n" +
                        "java " + "task_" + name;
//                System.out.println("javaCommand:" + javaCommand);

                String javaRes = "";
                try {
                    javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                codeResult.put(name , javaRes);
                codeExecuteRes.put(name.toLowerCase() , javaRes);
            }else{//有输入参数
                List<String> sources = new ArrayList<>();
                String elems = "";
                for (int j = 0; j < links.size(); j++) {
                    Map<String , String> linkmap = links.get(j);
                    String source = linkmap.get("source").toLowerCase();
                    String target = linkmap.get("target").toLowerCase();

                    if (target.equals(itemId)){
                        sources.add(source);
                    }
                }
//                System.out.println(itemId);
                sources.sort(new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s1.compareTo(s2);
                    }
                });
//                System.out.println("sources:" + sources);
                String elem = "";
                for (int j = 0; j < sources.size(); j++) {
                    String sourceItem = sources.get(j);
                    String value = codeResult.get(sourceItem);
                    String temp = value.replaceAll(System.lineSeparator() , ",");

                    List<String> targetArray = new ArrayList<>();

                    for (int k = 0; k < links.size(); k++) {
                        Map<String , String> linkmap = links.get(k);
                        String source = linkmap.get("source").toLowerCase();
                        String target = linkmap.get("target").toLowerCase();

                        if (source.equals(sourceItem)){
                            targetArray.add(target);
                        }
                    }
                    targetArray.sort(new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            //如果是数字，按照数字排序，如果是字符串按照字符串排序
                            Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                            boolean num1 = pattern.matcher(s1).matches();
                            boolean num2 = pattern.matcher(s2).matches();
                            if(num1 && num2){
                                int o1 = Integer.parseInt(s1);
                                int o2 = Integer.parseInt(s2);
                                return o1 - o2;
                            }else{
                                return s1.compareTo(s2);
                            }
                        }
                    });
//                    System.out.println(targetArray);

                    int index = targetArray.indexOf(itemId);
                    elem = temp.split(",")[index];
                    elems += " " + elem;

                    Map<String , String> linkResult = new HashMap<>();
                    linkResult.put("source" , sourceItem);
                    linkResult.put("target" , itemId);
                    linkResult.put("elem" , elem);
                    codeExecuteElems.add(linkResult);

                }
//                System.out.println("elems:" + elems);
                System.out.println("codeExecuteElems:" + codeExecuteElems);

                String javaCommand = "cd " + path + "\n" +
                        "java " + "task_" + name + elems;
//                    System.out.println("javaCommand:" + javaCommand);

                String javaRes = "";
                try {
                    javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    System.out.println(javacRes);
                codeResult.put(name , javaRes);
                codeExecuteRes.put(name.toLowerCase() , javaRes);
            }

        }
//        System.out.println("codeResult:" + codeResult);
        String dagCodeRunRes = "";
        for (int i = 0; i < points_map.size(); i++) {
            Map<String, Integer> map = points_map.get(i);
            Set<String> keys = map.keySet();
            String name = "";

            //获得当前节点的name属性
            for (String key : keys) {
                if (key.equals("name")) {
                    name = (map.get(key) + "").toLowerCase();
                }
            }
            //判断是否为最终节点output=0
            output = input = 0;
            for (int j = 0; j < links.size(); j++) {
                Map<String , String> linkmap = links.get(j);
                String source = linkmap.get("source").toLowerCase();
                String target = linkmap.get("target").toLowerCase();

                if (source.equals(name)){
                    output++;
                }
                if (target.equals(name)){
                    input++;
                }

            }
            if (output == 0){
                dagCodeRunRes = codeResult.get(name);
            }
        }

        System.out.println("codeResult:" + codeResult);
        System.out.println("dagCodeRunRes:" + dagCodeRunRes);;

//        System.out.println("workTypeToImage:" + workTypeToImage);
//        System.out.println("customType:" + customType);
//        return dagCodeRunRes;
        return codeResult;
    }

    @Test
    public void containerSimTest(){

        String assignType = "custom";
        String username = "me";
        String planName = "aa";
        String workflowName = "Montage_20.xml";

//        System.out.println(assignType);
//        System.out.println(username);
//        System.out.println(planName);
//        System.out.println(workflowName);

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        String storagePath = "/home/dr/sim/";
        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
//        System.out.println("path:" + path);

        //获得所有文件,筛选出java文件
        /*File file = new File(path);
        String fileList[] = file.list();
        List<String> javaList = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            String javaFile = fileList[i];
            if(javaFile.endsWith(".java")){
                javaList.add(javaFile);
            }
        }
        Collections.sort(javaList);
        System.out.println(javaList);*/

        //编译java文件,生成class文件
        /*String command = "cd " + path + "\n" +
                "javac " + "*.java";
        System.out.println(command);
        String res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , command);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("javac res:");
        System.out.println(res);*/

        //获得所有文件,筛选出class文件
        /*List<String> classList = new ArrayList<String>();
        for (int i = 0; i < fileList.length; i++) {
            String classFile = fileList[i];
            if(classFile.endsWith(".class")){
                classList.add(classFile);
            }
        }*/

        //执行每个class文件，保存执行时间
        /*Map<String , String> executeTime = new HashMap<String , String>();
        for (int i = 0; i < javaList.size(); i++) {
            String javaItem = javaList.get(i).toString().replace(".java" , "");
            command = "cd " + path + "\n" +
                    "java " + javaItem + "";
            System.out.println(command);
            res = "";

            long startTime = System.currentTimeMillis();
            try {
                res = dockerLink.exeCommand(host , port , user , password , command);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            long endTime = System.currentTimeMillis();
            float seconds = (endTime - startTime) / 1000F;

            executeTime.put(javaItem , seconds + "");
            System.out.println(Float.toString(seconds) + " seconds.");
        }
        System.out.println(executeTime);*/

        //复制临时xml文件
        /*String tempXmlPath = "/home/dr/sim/" + "tempXml/";
        file = new File(tempXmlPath);
        if(!file.exists()){//如果文件夹不存在
            file.mkdir();//创建文件夹
        }

        String copyCommand = "cp" + " " + "/home/dr/sim/dax/" + workflowName + " " + tempXmlPath;
        System.out.println(copyCommand);
        res = "";
        try {
            res = dockerLink.exeCommand(host , port , user , password , copyCommand);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        //更新DAG xml的runtime属性
        /*try {
            String domPath = tempXmlPath + workflowName;
            SAXBuilder builder = new SAXBuilder();
            //parse using builder to get DOM representation of the XML file
            Document dom = builder.build(domPath);
            Element root = dom.getRootElement();
            List<Element> domItemList = root.getChildren();
            System.out.println("domItemList:" + domItemList.toString());
            for (Element node : domItemList) {
                switch (node.getName().toLowerCase()) {
                    case "job":
                        String id = node.getAttributeValue("id");
                        int id_int = Integer.parseInt(id.replace("ID" , ""));
                        String name = node.getAttributeValue("name");
                        String runtime = node.getAttributeValue("runtime");
                        System.out.println("id_int:" + id_int);
                        System.out.println("name:" + name);
                        System.out.println("runtime:" + runtime);

                        runtime = executeTime.get("task_"+ id_int);
                        System.out.println("executeTimeItem:" + runtime);
                        node.setAttribute("runtime",runtime);
                        System.out.println(node.getAttributeValue("runtime"));

                        break;

                }
            }
            XMLOutputter  outputter = new XMLOutputter();
            Format format = outputter.getFormat();
            format.setEncoding("GBK");
            format.setIndent("    ");
            outputter.setFormat(format);
//            outputter.output(dom , new FileWriter(tempXmlPath + workflowName));
            outputter.output(dom , new FileWriter(tempXmlPath + "temp.xml"));
            System.out.println("xml edit success");
            //更改xml runtime属性

        }catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Parsing Exception");
        }*/

        List<Map<String,String>> links = new ArrayList<>();
        List<String> points = new ArrayList<>();

        String filePath = storagePath + "dax/" + "Montage_20.xml";
        links = ParseXML.getLinks(filePath);
        points = ParseXML.getPoints(filePath);


//        String filePath = storagePath + "dagXML/" + "line3_3.xml";
//        links = ParseXML.getCustomLinks(filePath);
        String itemId = "0";
        int input = 0;
        int output = 0;
        for (int i = 0; i < links.size(); i++) {
            Map<String , String> linkmap = links.get(i);
            String source = linkmap.get("source");
            String target = linkmap.get("target");
//            System.out.println("source:" + source);
//            System.out.println("target:" + target);
            if (source.equals(itemId)){
                output++;
            }
            if (target.equals(itemId)){
                input++;
            }

        }
//        System.out.println("links:" + links);
//        System.out.println("input:" + input);
//        System.out.println("output:" + output);

//        System.out.println("------------------------------------------------------");
//        System.out.println(points);

        DagGraphUtil dagGraphUtil = new DagGraphUtil(points, links);

        JSONObject jsonObject = new JSONObject(dagGraphUtil.drawDagGraph());

        //point集合
        List<Map<String,Integer>> points_map = new ArrayList<>();
        points_map = (List)jsonObject.get("points");

        //所有x坐标
        ArrayList<Integer> x_arrays = new ArrayList<>();

        //每个任务的运行结果
        Map<String , String> codeResult = new HashMap<String , String>();

        //先编译java文件
        String javacCommand = "cd " + path + "\n" +
                "javac *.java";

//        System.out.println(javacCommand);
        String javacRes = "";

        try {
            javacRes = dockerLink.exeCommand(host , port , user , password , javacCommand);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < points_map.size(); i++) {
            Map<String , Integer> map = points_map.get(i);
            Set<String> keys = map.keySet();
            String name = "";

            //获得当前节点的name属性
            for (String key: keys) {
                if(key.equals("name")){
                    name = map.get(key) + "";
                }

                if(key.equals("x")) {
                    x_arrays.add(map.get(key));
                }
            }

            //获得当前节点的输入参数数量
            itemId = name;
            input = 0;
            output = 0;
            for (int j = 0; j < links.size(); j++) {
                Map<String , String> linkmap = links.get(j);
                String source = linkmap.get("source");
                String target = linkmap.get("target");
                if (source.equals(itemId)){
                    output++;
                }
                if (target.equals(itemId)){
                    input++;
                }

            }
//            System.out.println("input:" + input);


            //没有输入参数input==0
            if (input == 0){
                String javaCommand = "cd " + path + "\n" +
                        "java " + "task_" + name;
//                System.out.println("javaCommand:" + javaCommand);

                String javaRes = "";
                try {
                    javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                codeResult.put(name , javaRes);
            }else{//有输入参数
                List<String> sources = new ArrayList<>();
                String elems = "";
                for (int j = 0; j < links.size(); j++) {
                    Map<String , String> linkmap = links.get(j);
                    String source = linkmap.get("source");
                    String target = linkmap.get("target");

                    if (target.equals(itemId)){
                        sources.add(source);
                    }
                }
//                System.out.println(itemId);
                sources.sort(new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s1.compareTo(s2);
                    }
                });
//                System.out.println("sources:" + sources);
                String elem = "";
                for (int j = 0; j < sources.size(); j++) {
                    String sourceItem = sources.get(j);
                    String value = codeResult.get(sourceItem);
                    String temp = value.replaceAll(System.lineSeparator() , ",");

                    List<String> targetArray = new ArrayList<>();

                    for (int k = 0; k < links.size(); k++) {
                        Map<String , String> linkmap = links.get(k);
                        String source = linkmap.get("source");
                        String target = linkmap.get("target");

                        if (source.equals(sourceItem)){
                            targetArray.add(target);
                        }
                    }
                    targetArray.sort(new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            //如果是数字，按照数字排序，如果是字符串按照字符串排序
                            Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                            boolean num1 = pattern.matcher(s1).matches();
                            boolean num2 = pattern.matcher(s2).matches();
                            if(num1 && num2){
                                int o1 = Integer.parseInt(s1);
                                int o2 = Integer.parseInt(s2);
                                return o1 - o2;
                            }else{
                                return s1.compareTo(s2);
                            }
                        }
                    });
//                    System.out.println(targetArray);

                    int index = targetArray.indexOf(itemId);
                    elem = temp.split(",")[index];
                    elems += " " + elem;

                }
//                System.out.println("elems:" + elems);


                    String javaCommand = "cd " + path + "\n" +
                            "java " + "task_" + name + elems;
//                    System.out.println("javaCommand:" + javaCommand);

                    String javaRes = "";
                    try {
                        javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
                    } catch (JSchException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
//                    System.out.println(javacRes);
                    codeResult.put(name , javaRes);

                /*String javaCommand = "cd " + path + "\n" +
                        "java " + "task_" + name + elems;
                System.out.println("javaCommand:" + javaCommand);

                String javaRes = "";
                try {
                    javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                codeResult.put(name , javaRes);*/
            }

        }
        System.out.println("codeResult:" + codeResult);
        String dagCodeRunRes = "";
        for (int i = 0; i < points_map.size(); i++) {
            Map<String, Integer> map = points_map.get(i);
            Set<String> keys = map.keySet();
            String name = "";

            //获得当前节点的name属性
            for (String key : keys) {
                if (key.equals("name")) {
                    name = map.get(key) + "";
                }
            }
            //判断是否为最终节点output=0
            output = input = 0;
            for (int j = 0; j < links.size(); j++) {
                Map<String , String> linkmap = links.get(j);
                String source = linkmap.get("source");
                String target = linkmap.get("target");

                if (source.equals(name)){
                    output++;
                }
                if (target.equals(name)){
                    input++;
                }

            }
            if (output == 0){
                dagCodeRunRes = codeResult.get(name);
                /*System.out.println("name :" + name);
                System.out.println("run result: " + codeResult.get(name));*/
            }
        }

        System.out.println(dagCodeRunRes);;
//        System.out.println(x_arrays);
//        List<Integer> x_arrays_single = x_arrays.stream().distinct().collect(Collectors.toList());
//        System.out.println(x_arrays_single);

//        System.out.println(jsonObject.toJSONString());
    }


    @Test
    public void Test(){
        String host =  "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";



        DockerLink dockerLink = new DockerLink();
        String path = "/home/dr/sim/";
        for (int i = 0; i < 20; i++) {
            String taskName = "task_" + i;
            String javaCommand = "cd " + path + "\n" +
                    "docker rmi -f " + taskName;
//                    System.out.println("javaCommand:" + javaCommand);

            String javaRes = "";
            try {
                javaRes = dockerLink.exeCommand(host , port , user , password , javaCommand);
            } catch (JSchException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void dagRunResult() {
        String username = "ding";
        String planName = "aa";
        String workflowName = "Epigenomics_24.xml";

        //连接docker
        DockerLink dockerLink = new DockerLink();
        String host = "127.0.0.1";
        int port = 22;
        String user = "dr";
        String password = "dr";

        String storagePath = "/home/dr/sim/";
        String path = storagePath + username + "/" + planName + "/" + workflowName + "/";
//        System.out.println("path:" + path);


        List<Map<String, String>> links = new ArrayList<>();
        List<String> points = new ArrayList<>();

        String dagxmlpath = "/home/dr/sim/dagXML/";
        String filePath = dagxmlpath + workflowName;
        links = ParseXML.getCustomLinks(filePath);
        points = ParseXML.getCustomPoints(filePath);


        String itemId = "0";
        int input = 0;
        int output = 0;
        for (int i = 0; i < links.size(); i++) {
            Map<String, String> linkmap = links.get(i);
            String source = linkmap.get("source");
            String target = linkmap.get("target");
//            System.out.println("source:" + source);
//            System.out.println("target:" + target);
            if (source.equals(itemId)) {
                output++;
            }
            if (target.equals(itemId)) {
                input++;
            }

        }
//        System.out.println("links:" + links);
//        System.out.println("input:" + input);
//        System.out.println("output:" + output);

//        System.out.println("------------------------------------------------------");
//        System.out.println(points);

        DagGraphUtil dagGraphUtil = new DagGraphUtil(points, links);

        JSONObject jsonObject = new JSONObject(dagGraphUtil.drawDagGraph());

        //point集合
        List<Map<String, Integer>> points_map = new ArrayList<>();
        points_map = (List) jsonObject.get("points");

        //所有x坐标
        ArrayList<Integer> x_arrays = new ArrayList<>();

        //每个任务的运行结果
        Map<String, String> codeResult = new HashMap<String, String>();

        //先编译java文件
        String javacCommand = "cd " + path + "\n" +
                "javac *.java";

//        System.out.println(javacCommand);
        String javacRes = "";

        try {
            javacRes = dockerLink.exeCommand(host, port, user, password, javacCommand);
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < points_map.size(); i++) {
            Map<String, Integer> map = points_map.get(i);
            Set<String> keys = map.keySet();
            String name = "";

            //获得当前节点的name属性
            for (String key : keys) {
                if (key.equals("name")) {
                    name = map.get(key) + "";
                }

                if (key.equals("x")) {
                    x_arrays.add(map.get(key));
                }
            }

            //获得当前节点的输入参数数量
            itemId = name;
            input = 0;
            output = 0;
            for (int j = 0; j < links.size(); j++) {
                Map<String, String> linkmap = links.get(j);
                String source = linkmap.get("source");
                String target = linkmap.get("target");
                if (source.equals(itemId)) {
                    output++;
                }
                if (target.equals(itemId)) {
                    input++;
                }

            }
//            System.out.println("input:" + input);


            //没有输入参数input==0
            if (input == 0) {
                String javaCommand = "cd " + path + "\n" +
                        "java " + "task_" + name;
//                System.out.println("javaCommand:" + javaCommand);

                String javaRes = "";
                try {
                    javaRes = dockerLink.exeCommand(host, port, user, password, javaCommand);
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                codeResult.put(name, javaRes);
            } else {//有输入参数
                List<String> sources = new ArrayList<>();
                String elems = "";
                for (int j = 0; j < links.size(); j++) {
                    Map<String, String> linkmap = links.get(j);
                    String source = linkmap.get("source");
                    String target = linkmap.get("target");

                    if (target.equals(itemId)) {
                        sources.add(source);
                    }
                }
//                System.out.println(itemId);
                sources.sort(new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        return s1.compareTo(s2);
                    }
                });
//                System.out.println("sources:" + sources);
                String elem = "";
                for (int j = 0; j < sources.size(); j++) {
                    String sourceItem = sources.get(j);
                    String value = codeResult.get(sourceItem);
                    String temp = value.replaceAll(System.lineSeparator(), ",");

                    List<String> targetArray = new ArrayList<>();

                    for (int k = 0; k < links.size(); k++) {
                        Map<String, String> linkmap = links.get(k);
                        String source = linkmap.get("source");
                        String target = linkmap.get("target");

                        if (source.equals(sourceItem)) {
                            targetArray.add(target);
                        }
                    }
                    targetArray.sort(new Comparator<String>() {
                        @Override
                        public int compare(String s1, String s2) {
                            //如果是数字，按照数字排序，如果是字符串按照字符串排序
                            Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                            boolean num1 = pattern.matcher(s1).matches();
                            boolean num2 = pattern.matcher(s2).matches();
                            if (num1 && num2) {
                                int o1 = Integer.parseInt(s1);
                                int o2 = Integer.parseInt(s2);
                                return o1 - o2;
                            } else {
                                return s1.compareTo(s2);
                            }
                        }
                    });
//                    System.out.println(targetArray);

                    int index = targetArray.indexOf(itemId);
                    elem = temp.split(",")[index];
                    elems += " " + elem;

                }
//                System.out.println("elems:" + elems);


                String javaCommand = "cd " + path + "\n" +
                        "java " + "task_" + name + elems;
//                    System.out.println("javaCommand:" + javaCommand);

                String javaRes = "";
                try {
                    javaRes = dockerLink.exeCommand(host, port, user, password, javaCommand);
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                    System.out.println(javacRes);
                codeResult.put(name, javaRes);
            }

        }
//        System.out.println("codeResult:" + codeResult);
        String dagCodeRunRes = "";
        for (int i = 0; i < points_map.size(); i++) {
            Map<String, Integer> map = points_map.get(i);
            Set<String> keys = map.keySet();
            String name = "";

            //获得当前节点的name属性
            for (String key : keys) {
                if (key.equals("name")) {
                    name = map.get(key) + "";
                }
            }
            //判断是否为最终节点output=0
            output = input = 0;
            for (int j = 0; j < links.size(); j++) {
                Map<String, String> linkmap = links.get(j);
                String source = linkmap.get("source");
                String target = linkmap.get("target");

                if (source.equals(name)) {
                    output++;
                }
                if (target.equals(name)) {
                    input++;
                }

            }
            if (output == 0) {
                dagCodeRunRes = codeResult.get(name);
            }
        }

        System.out.println(dagCodeRunRes);
    }

    @Test
    public void daglinks(){
        List<String> points = new ArrayList<>();
        List<Map<String,String>> links = new ArrayList<>();

        String dagxmlpath = "/home/dr/sim/dagXML/";
        String workflowName = "";
        workflowName = "Montage_20.xml";
        String filePath = dagxmlpath + workflowName;
        links = ParseXML.getCustomLinks(filePath);
        points = ParseXML.getCustomPoints(filePath);
        System.out.println(links);
        System.out.println(points);
    }

}
