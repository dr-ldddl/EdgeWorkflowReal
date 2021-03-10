$(document).ready(function(){
    var flag_userLogin = false;
    function showCustomWorkflow() {
        $("#custom2").empty();
        var strings = parent.$('#customXmlFile').val();
        var cusWorkflow = strings.split(",");
        for (var i=0; i<cusWorkflow.length; i++) {
            $("#custom2").append('<option value="'+i+'">'+cusWorkflow[i]+'</option>');
        }
    }
    // 判断用户是否登录,以便判断是否显示custom workflow选项
    var email = parent.$('#emailAddress').val();
    if(email == null || email.trim() == ""){// 用户未登录
        $("#selectCustom").attr("disabled", true);
    } else{
        $("#selectCustom").attr("disabled", false);
        showCustomWorkflow();
        flag_userLogin = true;
    }
    // 初始化参数
    var typeJson = new Object();
    Type = ["Small","Middle","Large"];
    CloudMips = [1600,3200,4800];
    CloudCost = [0.96,1.66,2.36];
    FogMips = [1300,2600,3900];
    FogCost = [0.48,0.78,1.08];
    MobileMips = [1000,2000,3000];
    //获取algorithms参数
    $.ajax({
        url:"/getAlgorithms",
        type:"POST",
        // data:"",
        dataType:"text",
        async: false,
        contentType: "application/json",
        success:function (res) {
            //console.log(res);
            typeJson = eval("("+res+")");

        },
        error:function () {
            layer.msg("import error!");
        }
    });
    function sortNumber(a,b) {
        return parseInt(a) - parseInt(b);
    }
    function showAmountSelect(workflow_type) {
        $("#strategys").empty();
        var typeName = workflow_type;
        var list = typeJson[typeName];
        list = list.sort(sortNumber);
        for (var i=0; i<list.length; i++) {
            $("#strategys").append('<option value="'+i+'">'+list[i]+'</option>');
        }
    }
    showAmountSelect("Montage");
    var workflow_type;
    $("input[name='radioGroup1']").change(function () {
        if(this.value == "Montage"){
            workflow_type = "Montage";
        }else if(this.value == "CyberShake"){
            workflow_type = "CyberShake";
        }else if(this.value == "Epigenomics"){
            workflow_type = "Epigenomics";
        }else if(this.value == "Inspiral"){
            workflow_type = "Inspiral";
        }else if(this.value == "Sipht"){
            workflow_type = "Sipht";
        }
        showAmountSelect(workflow_type);
    })
    //showAmountSelect();

    var json1 = new Object();
    var json =new Object();
    cloud_mips_list = [1600];
    cloud_cost_list = [0.96];
    fog_mips_list = [1300];
    fog_cost_list = [0.48];
    mobile_mips_list = [1000];
    cloud_number = 1;
    fog_number = 1;
    mobile_number = 1;
    var cloud_list = ["Small"];//存放"Small"、"Large"、"Super"
    var fog_list = ["Small"];//存放"Small"、"Large"、"Super"
    var mobile_list = ["Small"];//存放"Small"、"Large"、"Super"

    json1["cloud_mips_list"] = cloud_mips_list;
    json1["cloud_cost_list"] = cloud_cost_list;
    json1["fog_mips_list"] = fog_mips_list;
    json1["fog_cost_list"] = fog_cost_list;
    json1["mobile_mips_list"] = mobile_mips_list;
    json1["cloud_number"] = cloud_number;
    json1["fog_number"] = fog_number;
    json1["mobile_number"] = mobile_number;

    var pso_json = new Object();
    var ga_json = new Object();

    pso_json["PSO-particleNum"] = 20;
    pso_json["PSO-iterateNum"] = 100;
    pso_json["PSO-c1"] = 1.37;
    pso_json["PSO-c2"] = 1.37;
    pso_json["PSO-w"] = 0.73;
    pso_json["PSO-repeat"] = 1;

    ga_json["GA-popsize"] = 20;
    ga_json["GA-gmax"] = 100;
    ga_json["GA-crossoverProb"] = 0.8;
    ga_json["GA-mutationRate"] = 0.01;
    ga_json["GA-repeat"] = 1;

    json1["GA"] = ga_json;
    json1["PSO"] = pso_json;
    $('#parent_cloud_tips').text(JSON.stringify(json1));

    $("input[name='radioGroup2']").click(function(){
        var userSelect = $("input[name='radioGroup2']:checked").val();
        if(userSelect == "selectCustom"){
            $("#custom2").attr("disabled",false);
            $("#strategys").attr("disabled",true);
            $("input[name='radioGroup1']").attr("disabled",true);
        } else if(userSelect == "selectScience"){
            $("#custom2").attr("disabled",true);
            $("#strategys").attr("disabled",false);
            $("input[name='radioGroup1']").attr("disabled",false);
        }
    });


    //可视化工作流
    $("#draw_workflow").click(function(){
        if(!flag_userLogin){
            layer.msg("Please log in！",
                {time:1000}
            );
            return false;
        }
        var uSelect = $("input[name='radioGroup2']:checked").val();
        if(uSelect == "selectScience"){
            layer.msg("Please select 'Custom Workflow'");
        } else{
            var email = parent.$('#emailAddress').val();
            //var password = userJson['password'];
            $.cookie('email', null);
            //$.cookie('password', null);
            $.cookie("email",email,{ expires: 7});
            //$.cookie("password",password,{ expires: 7});

            parent.window.location.href = "http://127.0.0.1:8089/index";
            // parent.window.location.href = 'http://www.iseclab.org.cn:8089/index';
            // parent.window.location.href = 'http://47.98.222.243:8089/index';
        }
    });

    //点击next3
    var planName;
    $("#next3").click(function () {
        //获取窗口索引
        var index = parent.layer.getFrameIndex(window.name);
        parent.$("#childrenIndex").val(index);

        planName = $("input[name='planName']").val();
        if(planName==null || planName.trim()==""){
            layer.msg("Please enter PlanName");
        }
        var patt = /^[a-zA-Z_][a-zA-Z0-9_]*/;
        var flag = patt.test(planName);


        if(flag){
            var customTaskNum = 0;
            var userSelect = $("input[name='radioGroup2']:checked").val();
            if(userSelect == "selectCustom"){
                var cusSelectXml = $("#custom2 option:selected").text();
                customTaskNum = cusSelectXml.substring(cusSelectXml.lastIndexOf("_")+1,cusSelectXml.lastIndexOf("."));
            } else if(userSelect == "selectScience"){
                customTaskNum = $("#strategys option:selected").text();
            }

            /*保存数据*/
            var custom = $("#custom2 option:selected").text(); ///////////////////////存放自定义xml信息
            if(custom == null || custom.trim() == ""){
                custom = "";
            }
            workflow_type =$("input[name='radioGroup1']:checked").val();  ////////////////////存放工作流信息
            var nodeSize = $("#strategys option:selected").text();////////////////////存放任务量信息
            var daxPath = workflow_type + "_" + nodeSize + ".xml";// xml文件路径
            json.nodeSize = nodeSize;
            json.daxPath = daxPath;
            json.planName = planName;
            if(userSelect == "selectCustom"){
                json.custom = custom;
                $("#assignType").removeAttr("disabled");
            } else if(userSelect == "selectScience"){
                json.custom = "";
                $("#assignType").attr("disabled","disabled");
            }

            /*关闭sets3,打开sets4*/
            $("#sets3").css('display','none');
            $("#sets4").css('display','block');

            /*sets4的js事件*/
            var html = "";
            $("#main_left").html("");
            $("#pi").html("");
            $("#kmp").html("");
            $("#levenshtein").html("");
            $("#selectsort").html("");
            for(var i = 0; i< customTaskNum ; i++){
                html += "<div class=\"layui-col-xs3 item noSelect\">" + i + "</div>";
            }
            $("#main_left").html(html);

            if (userSelect == "selectScience"){
                $("#dagEditDiv").hide();

                $("#content4").show();
                $(".unassigned").show();
                $(".assigned").show();

                $("#assignType").val("standard");

            }else{
                //更新dag图
                var username = parent.$("#username_a").text();
                var custom = json.custom;
                var dagXml;
                if(custom != ""){
                    dagXml = custom;
                }else{
                    dagXml = json.daxPath;
                }
                var planName = json.planName;
                var customXml = json.custom;
                var dagparams = new Object();
                dagparams.username = username;
                dagparams.dagXml = dagXml;
                dagparams.planName = planName;
                dagparams.customXml = customXml;
                printDag(dagparams);
            }

        }else{
            layer.msg("The plan name format is incorrect!",
                {icon: 2,offset:['40%', '30%'],time:2000,area:['300px','70px']});

        }
    });

    /*previous按钮*/
    $("#pre4").click(function () {
        /*关闭sets4,打开sets3*/
        $("#sets4").css('display','none');
        $("#sets3").css('display','block');
    });

    /*next按钮*/
    $("#submit").click(function(){
        var assignType = $("#assignType option:selected").val();
        json.assignType = assignType;
        if(assignType == "standard"){
            var pi = [];
            var kmp = [];
            var levenshtein = [];
            var selectsort = [];
            var workItem = $("#main_left").find(".item").length;
            if(workItem != 0){
                layer.msg("Please assign the task type first!",
                    {icon: 2,offset:['40%', '30%'],time:3000,area:['300px','70px']});
                return;
            }

            $("#pi").find(".item").each(function(){
                var jobId = $(this).text();
                pi.push(jobId);
            });

            $("#kmp").find(".item").each(function(){
                var jobId = $(this).text();
                kmp.push(jobId);
            });

            $("#levenshtein").find(".item").each(function(){
                var jobId = $(this).text();
                levenshtein.push(jobId);
            });

            $("#selectsort").find(".item").each(function(){
                var jobId = $(this).text();
                selectsort.push(jobId);
            });

            // console.log(pi);
            // console.log(kmp);
            // console.log(levenshtein);
            // console.log(selectsort);

            /*保存数据*/
            json.pi = pi;
            json.kmp = kmp;
            json.levenshtein = levenshtein;
            json.selectsort = selectsort;
        }
        if(assignType == "custom"){
            var allTaskNum = 0;
            var userSelect = $("input[name='radioGroup2']:checked").val();
            if(userSelect == "selectCustom"){
                var cusSelectXml = $("#custom2 option:selected").text();
                allTaskNum = cusSelectXml.substring(cusSelectXml.lastIndexOf("_")+1,cusSelectXml.lastIndexOf("."));
            } else if(userSelect == "selectScience"){
                allTaskNum = $("#strategys option:selected").text();
            }

            var username = parent.$("#username_a").text();
            var planName = json.planName;
            var custom = json.custom;
            var workflowName;
            if(custom != ""){
                workflowName = custom;
            }else{
                workflowName = json.daxPath;
            }
            var taskListParam = {"username": username , "planName" : planName , "workflowName" : workflowName};
            var exitTasks = getExistTasks(taskListParam);
            var exitTasksNum = exitTasks.length;
            json.workflowPathParam = taskListParam;
            // console.log(allTaskNum);
            // console.log("exitTasksNum:" + exitTasksNum);
            if(exitTasksNum < allTaskNum){
                layer.msg("Please assign the task type first!",
                    {icon: 2,offset:['40%', '30%'],time:3000,area:['300px','70px']});
                return;
            }
        }
        /*关闭sets4,打开sets1*/
        $("#sets4").css('display','none');
        $("#sets1").css('display','block');

    });

    //云服务器数量控制
    var cloudNum = 1;
    var count_cloud = 0;
    /*reduce按钮*/
    $("#down1").click(function () {
        var cloud = $("input[name='cloudNum']");
        if(parseInt(cloud.val()) > 0){
            cloudNum = parseInt(cloud.val());
        }else{
            cloud.val("1");
            cloudNum = 1;
        }
        var cloudType = $("#clType option:selected").val();
        for (var i = 0; i < cloud_list.length; i++) {
            if(cloud_list[i] == cloudType){
                count_cloud = count_cloud + 1;
            }
        }
        if(count_cloud >= cloudNum){// 可以删减
            var temp_cloud = cloudNum;
            // 1 参数数组中数据首先发生变化
            for (var i = 0; i < cloud_list.length; i++) {
                if((cloud_list[i] == cloudType) && (temp_cloud > 0)){
                    var lastIndex = cloud_list.length - 1;
                    str = cloud_list[i];
                    cloud_list[i] = cloud_list[lastIndex];
                    cloud_list[lastIndex] = str;
                    cloud_list.length = cloud_list.length - 1;
                    str = cloud_mips_list[i];
                    cloud_mips_list[i] = cloud_mips_list[lastIndex];
                    cloud_mips_list[lastIndex] = cloud_mips_list[i];
                    cloud_mips_list.length = cloud_mips_list.length - 1;
                    str = cloud_cost_list[i];
                    cloud_cost_list[i] = cloud_cost_list[lastIndex];
                    cloud_cost_list[lastIndex] = cloud_cost_list[i];
                    cloud_cost_list.length = cloud_cost_list.length - 1;
                    i = i - 1;
                    temp_cloud = temp_cloud - 1;
                }
            }
            // 2 图片发生变化
            $("#cloudServerImgs").empty();
            for (var i = 0; i < cloud_list.length; i++) {
                $("#cloudServerImgs").append($("<div class=\"imag1\">\n" +
                    "                                <img src=\"images/cloudServer.png\"/>\n" +
                    "                                <div class=\"title\">"+cloud_list[i]+"</div>\n" +
                    "                            </div>"));
            }
            // 3 云服务器总数发生变化
            cloud_number = cloud_list.length;
            $("#cloud_number").text(cloud_number);
            count_cloud = 0;
        }else{
            count_cloud = 0;
            layer.msg("The quantity you want to delete exceeds the total amount of this type!");
        }
    });
    /*add按钮*/
    $("#up1").click(function () {
        var cloud = $("input[name='cloudNum']");
        if(parseInt(cloud.val()) > 0){
            cloudNum = parseInt(cloud.val());
        }else{
            cloud.val("1");
            cloudNum = 1;
        }

        var cloudType = $("#clType option:selected").val();
        // 1 参数数组中数据首先发生变化
        for (var j = 0; j < Type.length; j++) {
            if(cloudType == Type[j]){
                for (var i = 0; i < cloudNum; i++) {
                    cloud_list.push(cloudType);
                    cloud_mips_list.push(CloudMips[j]);
                    cloud_cost_list.push(CloudCost[j]);
                }
            }
        }
        // 2 图片发生变化
        $("#cloudServerImgs").empty();
        for (var i = 0; i < cloud_list.length; i++) {
            $("#cloudServerImgs").append($("<div class=\"imag1\">\n" +
                "                                <img src=\"images/cloudServer.png\"/>\n" +
                "                                <div class=\"title\">"+cloud_list[i]+"</div>\n" +
                "                            </div>"));
        }
        // 3 云服务器总数发生变化
        cloud_number = cloud_list.length;
        $("#cloud_number").text(cloud_number);
    });

    //边缘服务器数量变化控制
    var edgeNum = 1;
    var count_edge = 0;
    /*reduce按钮*/
    $("#down2").click(function () {
        var edge = $("input[name='edgeNum']");
        if(parseInt(edge.val()) > 0){
            edgeNum = parseInt(edge.val());
        }else{
            edge.val("1");
            edgeNum = 1;
        }

        var edgeType = $("#edType option:selected").val();
        for (var i = 0; i < fog_list.length; i++) {
            var str;
            if(fog_list[i] == edgeType){
                count_edge = count_edge + 1;
            }
        }
        if(count_edge >= edgeNum){// 可以删减
            var temp_edge = edgeNum;
            // 1 参数数组中数据首先发生变化
            for (var i = 0; i < fog_list.length; i++) {
                if((fog_list[i] == edgeType) && (temp_edge > 0)){
                    var lastIndex = fog_list.length - 1;
                    str = fog_list[i];
                    fog_list[i] = fog_list[lastIndex];
                    fog_list[lastIndex] = str;
                    fog_list.length = fog_list.length - 1;
                    str = fog_mips_list[i];
                    fog_mips_list[i] = fog_mips_list[lastIndex];
                    fog_mips_list[lastIndex] = fog_mips_list[i];
                    fog_mips_list.length = fog_mips_list.length - 1;
                    str = fog_cost_list[i];
                    fog_cost_list[i] = fog_cost_list[lastIndex];
                    fog_cost_list[lastIndex] = fog_cost_list[i];
                    fog_cost_list.length = fog_cost_list.length - 1;
                    i = i - 1;
                    temp_edge = temp_edge - 1;
                }
            }
            // 2 图片发生变化
            $("#fogServerImgs").empty();
            for (var i = 0; i < fog_list.length; i++) {
                $("#fogServerImgs").append($("<div class=\"imag2\">\n" +
                    "                                <img src=\"images/fogServer.png\"/>\n" +
                    "                                <div class=\"title\">"+fog_list[i]+"</div>\n" +
                    "                            </div>"));
            }
            // 3 边缘服务器总数发生变化
            fog_number = fog_list.length;
            $("#fog_number").text(fog_number);
            count_edge = 0;
        }else{
            count_edge = 0;
            layer.msg("The quantity you want to delete exceeds the total amount of this type!");
        }
    });
    /*add按钮*/
    $("#up2").click(function () {
        var edge = $("input[name='edgeNum']");
        if(parseInt(edge.val()) > 0){
            edgeNum = parseInt(edge.val());
        }else{
            edge.val("1");
            edgeNum = 1;
        }

        var edgeType = $("#edType option:selected").val();
        // 1 参数数组中数据首先发生变化
        for (var j = 0; j < Type.length; j++) {
            if(edgeType == Type[j]){
                for (var i = 0; i < edgeNum; i++) {
                    fog_list.push(edgeType);
                    fog_mips_list.push(FogMips[j]);
                    fog_cost_list.push(FogCost[j]);
                }
            }
        }
        // 2 图片发生变化
        $("#fogServerImgs").empty();
        for (var i = 0; i < fog_list.length; i++) {
            $("#fogServerImgs").append($("<div class=\"imag2\">\n" +
                "                                <img src=\"images/fogServer.png\"/>\n" +
                "                                <div class=\"title\">"+fog_list[i]+"</div>\n" +
                "                            </div>"));
        }
        // 3 边缘服务器总数发生变化
        fog_number = fog_list.length;
        $("#fog_number").text(fog_number);
    });

    //终端设备数量变化控制
    var mobileNum = 1;
    var count_mobile = 0;
    /*reduce按钮*/
    $("#down3").click(function () {
        var mobile = $("input[name='mobileNum']");
        if(parseInt(mobile.val()) > 0){
            mobileNum = parseInt(mobile.val());
        }else{
            mobile.val("1");
            mobileNum = 1;
        }

        var mobileType = $("#moType option:selected").val();
        for (var i = 0; i < mobile_list.length; i++) {
            var str;
            if(mobile_list[i] == mobileType){
                count_mobile = count_mobile + 1;
            }
        }
        if(count_mobile >= mobileNum){// 可以删减
            var temp_mobile = mobileNum;
            // 1 参数数组中数据首先发生变化
            for (var i = 0; i < mobile_list.length; i++) {
                if((mobile_list[i] == mobileType) && (temp_mobile > 0)){
                    var lastIndex = mobile_list.length - 1;
                    str = mobile_list[i];
                    mobile_list[i] = mobile_list[lastIndex];
                    mobile_list[lastIndex] = str;
                    mobile_list.length = mobile_list.length - 1;
                    str = mobile_mips_list[i];
                    mobile_mips_list[i] = mobile_mips_list[lastIndex];
                    mobile_mips_list[lastIndex] = mobile_mips_list[i];
                    mobile_mips_list.length = mobile_mips_list.length - 1;
                    i = i - 1;
                    temp_mobile = temp_mobile - 1;
                }
            }
            // 2 图片发生变化
            $("#mobileImgs").empty();
            for (var i = 0; i < mobile_list.length; i++) {
                $("#mobileImgs").append($("<div class=\"imag3\">\n" +
                    "                                <img src=\"images/mobile.png\"/>\n" +
                    "                                <div class=\"title\">"+mobile_list[i]+"</div>\n" +
                    "                            </div>"));
            }
            // 3 终端设备总数发生变化
            mobile_number = mobile_list.length;
            $("#mobile_number").text(mobile_number);
            count_mobile = 0;
        }else{
            count_mobile = 0;
            layer.msg("The quantity you want to delete exceeds the total amount of this type!");
        }
    });
    /*add按钮*/
    $("#up3").click(function () {
        var mobile = $("input[name='mobileNum']");
        if(parseInt(mobile.val()) > 0){
            mobileNum = parseInt(mobile.val());
        }else{
            mobile.val("1");
            mobileNum = 1;
        }
        var mobileType = $("#moType option:selected").val();
        // 1 参数数组中数据首先发生变化
        for (var j = 0; j < Type.length; j++) {
            if(mobileType == Type[j]){
                for (var i = 0; i < mobileNum; i++) {
                    mobile_list.push(mobileType);
                    mobile_mips_list.push(MobileMips[j]);
                }
            }
        }
        // 2 图片发生变化
        $("#mobileImgs").empty();
        for (var i = 0; i < mobile_list.length; i++) {
            $("#mobileImgs").append($("<div class=\"imag3\">\n" +
                "                                <img src=\"images/mobile.png\"/>\n" +
                "                                <div class=\"title\">"+mobile_list[i]+"</div>\n" +
                "                            </div>"));
        }
        // 3 终端设备总数发生变化
        mobile_number = mobile_list.length;
        $("#mobile_number").text(mobile_number);
    });


    /*上一页*/
    $("#pre1").click(function () {
        /*关闭sets1,打开sets4*/
        $("#sets1").css('display','none');
        $("#sets4").css('display','block');
    });

    /*下一页*/
    $("#next1").click(function(){
        /*保存sets1数据*/
        cloud_number = parseInt($("#cloud_number").text());  /////////////////// 存放云服务器数量
        fog_number = parseInt($("#fog_number").text());   /////////////////// 存放雾服务器数量
        mobile_number = parseInt($("#mobile_number").text()); /////////////////// 存放终端数量

        if((cloud_number > 0) && (fog_number > 0) && (mobile_number > 0)){
            var data = $("#parent_cloud_tips").text();          /*setting_json开始*/
            var json1 = eval("("+data+")");

            json1["cloud_number"] = cloud_number;
            json1["fog_number"] = fog_number;
            json1["mobile_number"] = mobile_number;
            json1["cloud_mips_list"] = cloud_mips_list;
            json1["cloud_cost_list"] = cloud_cost_list;
            json1["fog_mips_list"] = fog_mips_list;
            json1["fog_cost_list"] = fog_cost_list;
            json1["mobile_mips_list"] = mobile_mips_list;
            $('#parent_cloud_tips').text(JSON.stringify(json1));
            var data = $("#parent_cloud_tips").text();
            setting_json = eval("("+data+")");          /*setting_json结束*/

            json.cloudServer = cloud_number;
            json.fogServer = fog_number;
            json.mobile = mobile_number;
            json.setting_json = setting_json;
            /*关闭sets1，打开sets2*/
            $("#sets1").css('display','none');
            $("#sets2").css('display','block');
        }else{
            layer.msg("The number of each device shall be at least one!");
        }
    });

    /*上一页*/
    $("#pre2").click(function () {
        /*关闭sets2，打开sets1*/
        $("#sets2").css('display','none');
        $("#sets1").css('display','block');
    });

    var alSet = new Set();
    alSet.add("MINMIN");
    //根据选择的算法校验优化目标
    function checkChange(ele) {
        var value = $(ele).val();
        if ($(ele).prop("checked")) {
            alSet.add(value);
            $("#output").append("<option value='"+value+"'>"+value+"</option>")
            if (alSet.has("MINMIN") || alSet.has("MAXMIN") || alSet.has("FCFS") || alSet.has("ROUNDROBIN")) {
                $("#energy").attr("disabled", true);
                $("#cost").attr("disabled", true);
                $("#time").prop("checked", "checked");
            }
        } else {
            $("#output option[value='"+value+"']").remove();
            alSet.delete(value);
            if (alSet.size == 0) {
                $("#energy").attr("disabled", false);
                $("#cost").attr("disabled", false);
            } else if (!alSet.has("MINMIN") && !alSet.has("MAXMIN") && !alSet.has("FCFS") && !alSet.has("ROUNDROBIN")) {
                $("#energy").attr("disabled", false);
                $("#cost").attr("disabled", false);
            }
        }
    }
    // Scheduling Algorithms的复选框改变触发的事件
    $(".al_check").change(function(){
        checkChange(this);
    });

    // Algorithms Setting按钮
    $("#setting").click(function(){
        layer.open({
            type: 2
            ,shade:0
            , offset: "140px"
            , title: "Algorithms Setting"
            , content: "/alg/PSO"
            , skin: 'title-style'
            , area: ['600px', '520px']
            ,cancel: function(){
                // feedSetting();
            }
        });
    });

    /*complete按钮*/
    $("#complete").click(function(){

        var data = $("#parent_cloud_tips").text();
        // console.log("setting_json" + data);
        var setting_json = eval("("+data+")");
        json.setting_json = setting_json;
        /*保存数据*/
        var strategy = $("#strategy option:selected").text();////////////////////存放卸载策略信息
        alSet =[];                                    ////////////////////存放调度算法信息
        $('input[name="alg"]:checked').each(function(){
            alSet.push($(this).val());
        });
        var objList = ['Time','Energy','Cost'];
        var optimize_objective = objList[$("input[name='radioGroup']:checked").val()];///////存放优化目标信息
        var deadline = $("input[name='deadline']").val();     /////////////////存放截止日期信息
        if(deadline == null){
            deadline = "";
        }
        // var real = 1;                                         //////////////////// 是否选择real环境
        json.strategy = strategy;
        json.alSet = alSet;
        json.optimize_objective = optimize_objective;
        json.deadline = deadline;

        // json.real = real;
        var editTime = getCurrentTime();
        json.editTime = editTime;
        // console.log("json:" + JSON.stringify(json));
        // 判断用户是否登录
        var email = parent.$('#emailAddress').val();
        // console.log("email:" + email);
        if(email == null || email == undefined || email == ""){// 用户未登录--存cookie

            // $.cookie("plans",null);
            var planName = $("input[name='planName']").val();
            // console.log("planName" + planName);
            var plan = json;
            var plan_json = new Object();
            plan_json[planName] = plan;
            // console.log("plan_json:" + plan_json);

            var cookiePlans = $.cookie("plan");
            var cookiePlans_json = eval("("+cookiePlans+")");

            if(cookiePlans_json == undefined){
                cookiePlans_json = new Object();
            }
            cookiePlans_json[planName] = plan;
            $.cookie("plan",JSON.stringify(cookiePlans_json),{ expires: 1});

            // console.log("cookiePlan:" + $.cookie("plan"));

            parent.$("#flushPlan").click();

        } else{//用户已登录--存数据库
            var param = {"email": email,"plan":json};
            // console.log("add plan：" + JSON.stringify(param));
            $.ajax({
                type: "POST",
                url: "updatePlan",
                data: {json: JSON.stringify(param)},
                async: true,
                dataType:"text",
                success: function (res) {
                    // var planJson = eval("("+ res +")");
                    //刷新父页面的plan表格
                    parent.$("#flushPlan").click();
                },
            });
        }
        console.log("json" + JSON.stringify(json));
        //获取窗口索引
        var index = parent.layer.getFrameIndex(window.name);
        //关闭当前页面
        parent.layer.close(index);

    });

    //workTask对象点击
    $(document).on("click", ".item", function(){
        if($(this).hasClass("noSelect")){
            $(this).removeClass("noSelect");
            $(this).addClass("isSelect");
        }else{
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
        }

    });

    //pi移动
    $("#pi_left").click(function(){
        $("#pi").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#main_left").append($(this));
            // console.log($(this).text());
        });
    });
    $("#pi_right").click(function(){
        $("#main_left").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#pi").append($(this));
            // console.log($(this).text());
        });
    });

    //kmp移动
    $("#kmp_left").click(function(){
        $("#kmp").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#main_left").append($(this));
            // console.log($(this).text());
        });
    });
    $("#kmp_right").click(function(){
        $("#main_left").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#kmp").append($(this));
            // console.log($(this).text());
        });
    });

    //levenshtein移动
    $("#levenshtein_left").click(function(){
        $("#levenshtein").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#main_left").append($(this));
            // console.log($(this).text());
        });
    });
    $("#levenshtein_right").click(function(){
        $("#main_left").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#levenshtein").append($(this));
            // console.log($(this).text());
        });
    });

    //selectsort移动
    $("#selectsort_left").click(function(){
        $("#selectsort").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#main_left").append($(this));
            // console.log($(this).text());
        });
    });
    $("#selectsort_right").click(function(){
        $("#main_left").find(".isSelect").each(function(){
            $(this).remove();
            $(this).removeClass("isSelect");
            $(this).addClass("noSelect");
            $("#selectsort").append($(this));
            // console.log($(this).text());
        });
    });

    //全选按钮
    $("#left_selectAll").click(function(){
        // console.log("#left_selectAll");
        var item = $("#main_left").find(".item").length;
        var isSelect = $("#main_left").find(".isSelect").length;
        var noSelect = $("#main_left").find(".noSelect").length;
        // console.log("left item:" + item);
        // console.log("left isSelect:" + isSelect);
        // console.log("left noSelect:" + noSelect);
        if(item > isSelect){
            $("#main_left").find(".noSelect").each(function(){
                $(this).removeClass("noSelect");
                $(this).addClass("isSelect");
            });
        }
        if(item == isSelect){
            $("#main_left").find(".isSelect").each(function(){
                $(this).removeClass("isSelect");
                $(this).addClass("noSelect");
            });
        }
    });
    $("#pi_selectAll").click(function(){
        // console.log("#pi_selectAll");
        var item = $("#pi").find(".item").length;
        var isSelect = $("#pi").find(".isSelect").length;
        var noSelect = $("#pi").find(".noSelect").length;
        // console.log("pi item:" + item);
        // console.log("pi isSelect:" + isSelect);
        // console.log("pi noSelect:" + noSelect);
        if(item > isSelect){
            $("#pi").find(".noSelect").each(function(){
                $(this).removeClass("noSelect");
                $(this).addClass("isSelect");
            });
        }
        if(item == isSelect){
            $("#pi").find(".isSelect").each(function(){
                $(this).removeClass("isSelect");
                $(this).addClass("noSelect");
            });
        }
    });
    $("#kmp_selectAll").click(function(){
        // console.log("#kmp_selectAll");
        var item = $("#kmp").find(".item").length;
        var isSelect = $("#kmp").find(".isSelect").length;
        var noSelect = $("#kmp").find(".noSelect").length;
        // console.log("kmp item:" + item);
        // console.log("kmp isSelect:" + isSelect);
        // console.log("kmp noSelect:" + noSelect);
        if(item > isSelect){
            $("#kmp").find(".noSelect").each(function(){
                $(this).removeClass("noSelect");
                $(this).addClass("isSelect");
            });
        }
        if(item == isSelect){
            $("#kmp").find(".isSelect").each(function(){
                $(this).removeClass("isSelect");
                $(this).addClass("noSelect");
            });
        }
    });
    $("#levenshtein_selectAll").click(function(){
        // console.log("#levenshtein_selectAll");
        var item = $("#levenshtein").find(".item").length;
        var isSelect = $("#levenshtein").find(".isSelect").length;
        var noSelect = $("#levenshtein").find(".noSelect").length;
        // console.log("levenshtein item:" + item);
        // console.log("levenshtein isSelect:" + isSelect);
        // console.log("levenshtein noSelect:" + noSelect);
        if(item > isSelect){
            $("#levenshtein").find(".noSelect").each(function(){
                $(this).removeClass("noSelect");
                $(this).addClass("isSelect");
            });
        }
        if(item == isSelect){
            $("#levenshtein").find(".isSelect").each(function(){
                $(this).removeClass("isSelect");
                $(this).addClass("noSelect");
            });
        }
    });
    $("#selectsort_selectAll").click(function(){
        // console.log("#selectsort_selectAll");
        var item = $("#selectsort").find(".item").length;
        var isSelect = $("#selectsort").find(".isSelect").length;
        var noSelect = $("#selectsort").find(".noSelect").length;
        // console.log("selectsort item:" + item);
        // console.log("selectsort isSelect:" + isSelect);
        // console.log("selectsort noSelect:" + noSelect);
        if(item > isSelect){
            $("#selectsort").find(".noSelect").each(function(){
                $(this).removeClass("noSelect");
                $(this).addClass("isSelect");
            });
        }
        if(item == isSelect){
            $("#selectsort").find(".isSelect").each(function(){
                $(this).removeClass("isSelect");
                $(this).addClass("noSelect");
            });
        }
    });

    //全选按钮的鼠标进入事件
    $("#left_selectAll").mouseenter(function(){
        $(this).css("background-color", "#75dbed");
        $(this).css("color", "#FFFFFF");
    });
    $("#pi_selectAll").mouseenter(function(){
        $(this).css("background-color", "#75dbed");
        $(this).css("color", "#FFFFFF");
    });
    $("#kmp_selectAll").mouseenter(function(){
        $(this).css("background-color", "#75dbed");
        $(this).css("color", "#FFFFFF");
    });
    $("#levenshtein_selectAll").mouseenter(function(){
        $(this).css("background-color", "#75dbed");
        $(this).css("color", "#FFFFFF");
    });
    $("#selectsort_selectAll").mouseenter(function(){
        $(this).css("background-color", "#75dbed");
        $(this).css("color", "#FFFFFF");
    });

    //全选按钮的鼠标离开事件
    $("#left_selectAll").mouseleave(function(){
        $(this).css("background-color", "#FFFFFF");
        $(this).css("color", "#000000");
    });
    $("#pi_selectAll").mouseleave(function(){
        $(this).css("background-color", "#FFFFFF");
        $(this).css("color", "#000000");
    });
    $("#kmp_selectAll").mouseleave(function(){
        $(this).css("background-color", "#FFFFFF");
        $(this).css("color", "#000000");
    });
    $("#levenshtein_selectAll").mouseleave(function(){
        $(this).css("background-color", "#FFFFFF");
        $(this).css("color", "#000000");
    });
    $("#selectsort_selectAll").mouseleave(function(){
        $(this).css("background-color", "#FFFFFF");
        $(this).css("color", "#000000");
    });

    //View Dag鼠标进入事件
    $("#left_viewDag").mouseenter(function(){
        $(this).css("background-color", "#75dbed");
        $(this).css("color", "#FFFFFF");

        var userSelect = $("input[name='radioGroup2']:checked").val();
        var customXml = $("#custom2 option:selected").text(); ///////////////////////存放自定义xml信息
        if(customXml == null || customXml.trim() == ""){
            customXml = "";
        }
        if(userSelect == "selectScience"){
            customXml = "";
        }
        workflow_type =$("input[name='radioGroup1']:checked").val();  ////////////////////存放工作流信息
        var nodeSize = $("#strategys option:selected").text();////////////////////存放任务量信息
        var dagXml = workflow_type + "_" + nodeSize + ".xml";// xml文件路径

        if(nodeSize > 100){
            layer.msg("You select too many tasks, please select fewer tasks!",
                {icon: 2,offset:['40%', '30%'],time:3000,area:['450px','70px']});
            return false;
        }
        if((nodeSize > 50) && (nodeSize <= 100)){
            layer.msg("You select too many tasks, please click View Dag!",
                {icon: 6,offset:['40%', '30%'],time:3000,area:['450px','70px']});
            return false;
        }
        // console.log("enter");
        $("#dagHelpShow").css('display','block');
        $.ajax({
            type: "POST",
            url: "/getDag",
            data: {dagXml : dagXml , customXml :customXml},
            dataType: "text",
            async: false,
            success: function (data) {
                // console.log(data);
                var dagData = eval("("+data+")");
                var points = dagData["points"];
                var links = dagData["links"];
                // console.log(JSON.stringify(points));
                // console.log(JSON.stringify(links));
                loadDag(points, links);
            },
            error: function(data){
                console.log("error...");
            }
        });

    });
    //View Dag鼠标离开事件
    $("#left_viewDag").mouseleave(function(){
        $(this).css("background-color", "#FFFFFF");
        $(this).css("color", "#000000");
        $("#dagHelpShow").css('display','none');
    });
    //View Dag鼠标点击事件
    $("#left_viewDag").click(function(){
        var userSelect = $("input[name='radioGroup2']:checked").val();

        var customXml = $("#custom2 option:selected").text(); ///////////////////////存放自定义xml信息
        if(customXml == null || customXml.trim() == ""){
            customXml = "";
        }
        if(userSelect == "selectScience"){
            customXml = "";
        }
        workflow_type =$("input[name='radioGroup1']:checked").val();  ////////////////////存放工作流信息
        var nodeSize = $("#strategys option:selected").text();////////////////////存放任务量信息
        var dagXml = workflow_type + "_" + nodeSize + ".xml";// xml文件路径

        if(nodeSize > 100){
            return false;
        }

        var data = {dagXml : dagXml , customXml :customXml};
        parent.$("#dagParam").val(JSON.stringify(data));
        parent.$("#showDag").click();
    });

//    *************************用户自定义代码开始*************************
    $("#assignType").change(function(){
        var assignType = $("#assignType option:selected").val();
        // console.log(assignType);
        if(assignType == "standard"){
            // $("#dagEditDiv").html("");
            $("#dagEditDiv").hide();

            $("#content4").show();
            $(".unassigned").show();
            $(".assigned").show();
        }

        if (assignType == "custom"){
            $("#dagEditDiv").show();

            $("#content4").hide();
            $(".unassigned").hide();
            $(".assigned").hide();

            var username = parent.$("#username_a").text();
            var custom = json.custom;
            var dagXml;
            if(custom != ""){
                dagXml = custom;
            }else{
                dagXml = json.daxPath;
            }
            var planName = json.planName;
            var customXml = json.custom;
            var dagparams = new Object();
            dagparams.username = username;
            dagparams.dagXml = dagXml;
            dagparams.planName = planName;
            dagparams.customXml = customXml;
            printDag(dagparams);
        }
    });

    //刷新DAG
    $("#flushDag").click(function(){
        // console.log("flush dag success!");
        var username = parent.$("#username_a").text();
        var custom = json.custom;
        var dagXml;
        if(custom != ""){
            dagXml = custom;
        }else{
            dagXml = json.daxPath;
        }
        var planName = json.planName;
        var customXml = json.custom;
        var dagparams = new Object();
        dagparams.username = username;
        dagparams.dagXml = dagXml;
        dagparams.planName = planName;
        dagparams.customXml = customXml;
        printDag(dagparams);
    });
//    *************************用户自定义代码结束*************************


});


//判断是否在前面加0
function getNow(s) {
    return s < 10 ? '0' + s: s;
}

//获取系统当前时间
function getCurrentTime(){
    var myDate = new Date();

    var year=myDate.getFullYear();      //获取当前年
    var month=myDate.getMonth()+1;      //获取当前月
    var date=myDate.getDate();          //获取当前日


    var h=myDate.getHours();            //获取当前小时数(0-23)
    var m=myDate.getMinutes();          //获取当前分钟数(0-59)
    var s=myDate.getSeconds();          //获取当前秒数(0-59)
    var now=year+'-'+getNow(month)+"-"+getNow(date)+" "+getNow(h)+':'+getNow(m)+":"+getNow(s);

    return now;
}

//加载dag图
function loadDag(points, links){
    var option = {
        title: {
            text: 'DAG Structure'
        },
        tooltip: {},
        animationDurationUpdate: 1500,
        animationEasingUpdate: 'quinticInOut',
        series: [
            {
                type: 'graph',
                layout: 'none',
                symbolSize: 20,
                roam: true,
                label: {
                    show: true
                },
                edgeSymbol: ['circle', 'arrow'],
                edgeSymbolSize: [4, 10],
                edgeLabel: {
                    fontSize: 20
                },
                data: points,
                links: links,
                lineStyle: {
                    opacity: 0.9,
                    width: 2,
                    curveness: 0
                }
            }
        ]
    };

    var dagChart = echarts.init(document.getElementById('dagHelpShow'));
    dagChart.clear();
    dagChart.setOption(option);

}


//*************************用户自定义代码开始*************************
var taskList;
var points;
var links;
var dagFlag = false;
var taskFlag = false;

function printDag(dagparams){
    var dagXml = dagparams.dagXml;
    var planName = dagparams.planName;
    var customXml = dagparams.customXml;
    var username = dagparams.username;

    // console.log("dagXml:" + dagXml);
    // console.log("planName:" + planName);
    // console.log("customXml:" + customXml);
    // console.log("username:" + username);
    /*dagXml = "Montage_20.xml";
    customXml = "";
    username = "dr";
    planName = "drtest";*/
    parent.$("#workflowName").val(dagXml);
    parent.$("#planName").val(planName);

    var dagJson = {dagXml:dagXml,customXml:customXml};
    var taskListParam = {"username": username , "planName" : planName , "workflowName" : dagXml};

    //获得所有已经存在编辑代码文件的工作流节点。
    getTaskList(taskListParam);

    //绘制DAG图
    getDag(dagJson);

    //绘制工作流DAG
    // console.log("dagFlag:" + dagFlag);
    // console.log("taskFlag:" + taskFlag);
    if(dagFlag && taskFlag){
        customDagCode(points , links);
    }

}

//获得所有已经存在编辑代码文件的工作流节点。
function getTaskList(taskListParam) {
    $.ajax({
        type: "POST",
        url: "/getTaskList",
        data: {taskListParam: JSON.stringify(taskListParam)},
        async: false,
        dataType:"text",
        success: function (res) {
            // console.log(res);
            res = eval("("+ res +")");
            taskList = res["taskList"];
            // console.log(taskList);
            // console.log("getTaskList");
            taskFlag = true;
        },
    });
}

//获得所有已经存在编辑代码文件的工作流节点。
function getExistTasks(taskListParam) {
    var taskList;
    $.ajax({
        type: "POST",
        url: "/getTaskList",
        data: {taskListParam: JSON.stringify(taskListParam)},
        async: false,
        dataType:"text",
        success: function (res) {
            res = eval("("+ res +")");
            taskList = res["taskList"];
            // console.log(taskList);
        },
    });
    return taskList;
}

//绘制DAG图
function getDag(dagJson){
    $.ajax({
        type: "POST",
        url: "/getDag",
        data: dagJson,
        dataType: "text",
        async: false,
        success: function (data) {
            // console.log(data);
            var dagData = eval("("+data+")");
            points = dagData["points"];
            links = dagData["links"];
            // loadDag(points, links);
            // console.log("getDag");
            dagFlag = true;
            // console.log("points:" + JSON.stringify(points));
            // console.log("links:" + JSON.stringify(links));

        },
        error: function(data){
            console.log("error...");
        }
    });
}

//加载dag图
function customDagCode(/*points, links*/){
    // console.log(points);
    // console.log(links);
    var option = {
        title: {
            text: 'DAG Structure'
        },
        tooltip: {},
        animationDurationUpdate: 1500,
        animationEasingUpdate: 'quinticInOut',
        series: [
            {
                itemStyle: {//配置节点的颜色已及尺寸
                    normal: {
                        color: function (params) {
                            var colorList = ['#FF0000','#28cad8'];
                            // console.log(params.dataIndex);
                            var taskName = params.name;
                            var exitFlag = $.inArray(taskName , taskList);
                            if(exitFlag >= 0){
                                return colorList[1];
                            }else{
                                return colorList[0];
                            }
                        },

                    }
                },
                type: 'graph',
                layout: 'none',
                symbolSize: 20,
                roam: true,
                label: {
                    show: true
                },
                edgeSymbol: ['circle', 'arrow'],
                edgeSymbolSize: [4, 10],
                edgeLabel: {
                    fontSize: 20
                },
                data: points,
                // links: [],
                links: links,
                lineStyle: {
                    opacity: 0.9,
                    width: 2,
                    curveness: 0
                }
            }
        ]
    };

    var dagChart = echarts.init(document.getElementById('dagEditDiv'));
    dagChart.clear();
    dagChart.setOption(option);
    //DAG图中元素左击事件
    dagChart.off("click");
    dagChart.on("click" , function (elem) {
        // console.log(elem);
        // console.log(elem.dataIndex);
        click(elem);
    });
    //DAG图中元素右击事件
    dagChart.off("contextmenu");
    dagChart.on("contextmenu" , function(elem){
        contextmenu(elem);
    });
}

//禁用浏览器的右击菜单
$(document).contextmenu(function() {
    return false;
});

//DAG图中元素左击事件
function click(task) {

    // console.log("click:");
    var name = task["name"];
    var type = task['dataType'];
    var color = task["color"];
    var taskName = parent.$("#taskName");
    var editFlag = parent.$("#editFlag");

    // console.log(task);
    // console.log("name:" + name);
    // console.log("dataType:" + type);
    // console.log("color:" + color);

    if(type == "node"){
        taskName.val(name);
        if(color == "#FF0000"){
            editFlag.val(false);
        }else{
            editFlag.val(true);
        }
        // editFlag.val(color);

        parent.layer.open({
            type: 2,
            shade:0,
            offset: "140px",
            title: "Custom Task",
            content: "/CodeRun",
            skin: "title-style",
            area: ['55%', '70%'],
            cancel: function(){

            }
        });
    }
}

//DAG图中元素右击事件
function contextmenu(task){

    console.log("contextmenu:");
    /*var name = task["name"];
    var type = task['dataType'];
    var taskName = parent.$("#taskName");

    // console.log(task);
    console.log("name:" + name);
    console.log("dataType:" + type);

    if(type == "node"){
        taskName.val(name);

        parent.layer.open({
            type: 2,
            shade:0,
            offset: "140px",
            title: "Custom Task",
            content: "/CodeRun",
            skin: "title-style",
            area: ['55%', '70%'],
            cancel: function(){

            }
        });
    }*/

    var mouseRightMenu = layui.mouseRightMenu;
    var menu_data=[
        {'data':"data",'type':1,'title':'RandomPI'},
        {'data':"data",'type':1,'title':'KMP Match'},
        {'data':"data",'type':1,'title':'Levenshtein'},
        {'data':"data",'type':1,'title':'SelectSort'},

    ]
    mouseRightMenu.open(menu_data,false,function(d){
        layer.alert(JSON.stringify(d));
    })
    return false;
}
//*************************用户自定义代码结束*************************
function test(){
    alert("iframe test!");
}
