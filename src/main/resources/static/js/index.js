$(document).ready(function () {

    {
        var alSet = new Set();
        var strategyList = ['','All-in-Fog','All-in-Cloud','Simple'];
        var alList = ['MINMIN','MAXMIN','FCFS','ROUNDROBIN','PSO','GA'];
        var typeList = ["Montage","CyberShake","Epigenomics","Inspiral","Sipht"];
        var objList = ['Time','Energy','Cost'];
        var records = null;
        $("#energy").attr("disabled", true);
        $("#cost").attr("disabled", true);
        checkChange($("#minmin"));

        var cloud_number;
        var fog_number;
        var mobile_number;
        var cloud_mips_list = [];
        var cloud_cost_list = [];
        var fog_mips_list = [];
        var fog_cost_list = [];
        var mobile_mips_list = [];

        var typeJson = new Object();
        var userJson = new Object();

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
                alert("import error!");
            }
        });

        //获取完整用户信息
        $.ajax({
            url:"/getUser",
            type:"POST",
            // data:"",
            dataType:"text",
            async: false,
            contentType: "application/json",
            success:function (res) {
                // console.log(res);
                userJson = eval("("+res+")");

            },
            error:function () {
                layer.msg("获取用户信息失败！");
            }
        });
    }

    // 输出提示信息
    function tips(content) {
        layer.open({
            type: 1
            , title: "Tips"
            , offset: '200px' //具体配置参考：offset参数项
            , content: '<div style="padding: 20px 80px;">' + content + '</div>'
            , btn: 'Ok'
            , btnAlign: 'c' //按钮居中
            , shade: 0 //不显示遮罩
            , yes: function () {
                layer.closeAll();
            }
        });
    }

    function showTypeSelect() {
        //TODO
        /*var data = '${typeJson}';
        typeJson = eval("("+data+")");*/

        $("#sType").empty();
        for (var i=0; i<typeList.length; i++) {
            $("#sType").append('<option value="'+i+'">'+typeList[i]+'</option>');
        }
    }

    showTypeSelect();

    function sortNumber(a,b) {
        return parseInt(a) - parseInt(b);
    }

    function showAmountSelect(ele) {
        $("#amount").empty();
        var typeName = typeList[$(ele).val()];
        var list = typeJson[typeName];
        list = list.sort(sortNumber);
        for (var i=0; i<list.length; i++) {
            $("#amount").append('<option value="'+i+'">'+list[i]+'</option>');
        }
    }

    showAmountSelect($("#sType"));

    $("#sType").change(function () {
        showAmountSelect(this);
    });

    function initParams() {
        var json = new Object();
        cloud_mips_list = [1600];
        cloud_cost_list = [0.96];
        fog_mips_list = [1300];
        fog_cost_list = [0.48];
        mobile_mips_list = [1000];
        cloud_number = 1;
        fog_number = 1;
        mobile_number = 1;
        json["cloud_mips_list"] = cloud_mips_list;
        json["cloud_cost_list"] = cloud_cost_list;
        json["fog_mips_list"] = fog_mips_list;
        json["fog_cost_list"] = fog_cost_list;
        json["mobile_mips_list"] = mobile_mips_list;
        json["cloud_number"] = cloud_number;
        json["fog_number"] = fog_number;
        json["mobile_number"] = mobile_number;

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

        json["GA"] = ga_json;
        json["PSO"] = pso_json;
        $('#parent_cloud_tips').text(JSON.stringify(json));

        $(".number_input").val('1');
    }

    //根据隐藏json串反馈给界面
    function feedSetting() {
        var data = $("#parent_cloud_tips").text();
        var json = eval("("+data+")");
        cloud_mips_list = json["cloud_mips_list"];
        cloud_cost_list = json["cloud_cost_list"];
        fog_mips_list = json["fog_mips_list"];
        fog_cost_list = json["fog_cost_list"];
        mobile_mips_list = json["mobile_mips_list"];

        cloud_number = json["cloud_number"];
        fog_number = json["fog_number"];
        mobile_number = json["mobile_number"];

        /*$("#cloudServer").val(cloud_number);
        $("#fogServer").val(fog_number);
        $("#mobile").val(mobile_number);*/

        $("#cloudServer_input").val(cloud_number);
        $("#fogServer_input").val(fog_number);
        $("#mobile_input").val(mobile_number);

        /*addImgs($("#cloudServer"));
        addImgs($("#fogServer"));
        addImgs($("#mobile"));*/
        addImgs_input($("#cloudServer_input"));
        addImgs_input($("#fogServer_input"));
        addImgs_input($("#mobile_input"));
    }

    //初始化fog环境变量
    initParams();
    feedSetting();

    //下拉选择参数设置
    function addImgs(e) {
        var pic_name = "/images/" + $(e).attr("id") + ".png";
        var list = "#" + $(e).attr("id") + "Imgs";
        $(list).empty();
        for (var i = 0; i < $(e).val(); i++) {
            $("<img class='imgs' src=" + pic_name + ">").appendTo($(list));
        }
    }
    //手动输入填写参数设置
    function addImgs_input(e) {
        var id = $(e).attr("id").substring(0,$(e).attr("id").length-6);
        var pic_name = "/images/" + id + ".png";
        var list = "#" + id + "Imgs";
        var sum = $(e).val();
        $(list).empty();
        for (var i = 0; i < sum; i++) {
            $("<img class='imgs' src=" + pic_name + ">").appendTo($(list));
        }

    }
    //Fog Environemnt参数配置
    function changeNumbers() {
        /*cloud_number = $("#cloudServer").val();
        fog_number = $("#fogServer").val();
        mobile_number = $("#mobile").val();
        var data = $("#parent_cloud_tips").text();
        var json = eval("("+data+")");
        json["cloud_number"] = cloud_number;
        json["fog_number"] = fog_number;
        json["mobile_number"] = mobile_number;
        $('#parent_cloud_tips').text(JSON.stringify(json));*/

        var data = $("#parent_cloud_tips").text();
        var json = eval("("+data+")");
        cloud_mips_list = json["cloud_mips_list"];
        cloud_cost_list = json["cloud_cost_list"];
        fog_mips_list = json["fog_mips_list"];
        fog_cost_list = json["fog_cost_list"];
        mobile_mips_list = json["mobile_mips_list"];

        cloud_number = $("#cloudServer_input").val();
        fog_number = $("#fogServer_input").val();
        mobile_number = $("#mobile_input").val();
        var data = $("#parent_cloud_tips").text();
        var json = eval("("+data+")");
        // debugger;

        cloud_mips_list = [];
        cloud_cost_list = [];
        for(var i = 0; i < cloud_number; i++){
            cloud_mips_list.push(1600);
            cloud_cost_list.push(0.96);
        }

        fog_mips_list = [];
        fog_cost_list = [];
        for(var i = 0; i< fog_number; i++){
            fog_mips_list.push(1300);
            fog_cost_list.push(0.48);
        }

        mobile_mips_list = [];
        for(var i = 0; i< mobile_number; i++){
            mobile_mips_list.push(1000);
        }

        json["cloud_number"] = cloud_number;
        json["fog_number"] = fog_number;
        json["mobile_number"] = mobile_number;
        json["cloud_mips_list"] = cloud_mips_list;
        json["cloud_cost_list"] = cloud_cost_list;
        json["fog_mips_list"] = fog_mips_list;
        json["fog_cost_list"] = fog_cost_list;
        json["mobile_mips_list"] = mobile_mips_list;
        $('#parent_cloud_tips').text(JSON.stringify(json));
    }

    $(".choose-imgs").change(function () {
       /* addImgs($(this));
        changeNumbers();*/

        var number = $(this).val();
        $(this).parent().find(".number_input").val(number);
        addImgs($(this));
        changeNumbers();

    });

    // Fog Computing Environment Setting文本框内容改变事件
    $(".number_input").change(function(){
        // debugger
        var number = $(this).val();
        var reg = /^[1-9]\d*$/;
        if(!reg.test(number)){
            tips("Please enter a positive integer!");
            $(this).val('1');
            addImgs_input($(this));
            changeNumbers();
            return;
        }
        $(this).parent().find(".number_input").val(number);
        addImgs_input($(this));
        changeNumbers();
    });

    // Algorithms Setting按钮
    $("#setting").click(function () {
        layer.open({
            type: 2
            , offset: "140px"
            , title: "Algorithms Setting"
            , content: "/alg/PSO"
            , skin: 'title-style'
            , area: ['600px', '580px']
            ,cancel: function(){
                // feedSetting();
            }
        });
    });

    // Developer Information
    $(".developer_logo").click(function () {
        layer.open({
            type: 2,
            offset: "140px",
            title: "Developer Information",
            content: "/developerInfo",
            skin: "title-style",
            area: ['600px', '400px'],
            cancel: function(){

            }
        });
    });

    // Developer Information
    $(".developer_png").click(function () {
        layer.open({
            type: 2,
            offset: "140px",
            title: "Developer Information",
            content: "/developerInfo",
            skin: "title-style",
            area: ['600px', '400px'],
            cancel: function(){

            }
        });
    });

    // more Detail按钮
    $("#more").click(function () {
        layer.open({
            type: 2
            , offset: "140px"
            , title: "Fog Computing Environment Setting"
            , content: "/fog"
            , skin: 'title-style'
            , area: ['1000px', '650px']
            ,cancel: function(){
                feedSetting();
            }
        });
    });

    // 复选框改变事件
    function checkChange(ele) {
        var value = $(ele).val();
        if ($(ele).prop("checked")) {
            alSet.add(value);
            $("#output").append("<option value='"+value+"'>"+alList[value]+"</option>")
            if (alSet.has("0") || alSet.has("1") || alSet.has("2") || alSet.has("3")) {
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
            } else if (!alSet.has("0") && !alSet.has("1") && !alSet.has("2") && !alSet.has("3")) {
                $("#energy").attr("disabled", false);
                $("#cost").attr("disabled", false);
            }
        }
    }

    // Scheduling Algorithms的复选框改变触发的事件
    $(".al_check").change(function () {
        checkChange(this);
    });

    // custom复选框
    $("#custom").click(function () {
        if ($(this).prop("checked")) {
            $("#custom_input").attr("disabled", false);
            $("#select_file_btn").attr("disabled", false);
        } else {
            $("#custom_input").attr("disabled", true);
            $("#select_file_btn").attr("disabled", true);
        }
    });

    $("#select_file_btn").click(function () {
        return $("#select-file").click();
    });

    // 上传自定义的xml文件
    $("#select-file").change(function () {
        var formData = new FormData();
        formData.append("file",$(this)[0].files[0])
        $.ajax({
            url:"/customFile",
            type:"POST",
            data:formData,
            dataType:"text",
            processData: false,
            cache:false,
            contentType: false,
            mimeType:"multipart/form-data",
            success:function (res) {
                // console.log(res);
                $("#custom_input").val(res);
            },
            error:function () {
                // alert("import error!");
            }
        })
        $(this).val("");
    });

    // 开始模拟，传入simulation/compare
    function start(url) {
        var cloudServer = $("#cloudServer").val();
        var fogServer = $("#fogServer").val();
        var mobile = $("#mobile").val();
        var strategy = strategyList[$("#strategy").val()];
        var optimize_objective = objList[$("input[name='radioGroup'][checked]").val()];
        var workflow_type = $("#sType").find("option:selected").text();
        var nodeSize = $("#amount").find("option:selected").text();
        var daxPath = workflow_type + "_" + nodeSize + ".xml";
        var custom = $("#custom_input").val();
        var deadline = $("#deadline").val();

        var json = new Object();
        json.cloudServer = cloudServer;
        json.fogServer = fogServer;
        json.mobile = mobile;
        json.strategy = strategy;
        json.optimize_objective = optimize_objective;
        json.daxPath = daxPath;
        json.nodeSize = nodeSize;


        if ($("#custom").prop("checked")) {
            var fileType = custom.substring(custom.length - 4);
            var customName = custom.substring(0,custom.length - 4);
            var customArr = customName.split('_');
            var reg1 = /^[A-Za-z]+$/;
            var reg2 = /^\d+$/;
            if(fileType != ".xml" || customArr.length != 2 || !reg1.test(customArr[0]) || !reg2.test(customArr[1])){
                tips("The file format you selected is incorrect. Please select again!");
                return;
            }
            json.custom = custom;
        }
        json.deadline = deadline;

        var data = $("#parent_cloud_tips").text();
        var setting_json = eval("("+data+")");
        json.setting_json = setting_json;

        var al_array = [];
        alSet.forEach(function (value) {
            al_array.push(alList[value]);
        });
        json.alSet = al_array;


        $.ajax({
            type: "POST",
            url: url,
            data: {json: JSON.stringify(json)},
            async: false,
            dataType:"JSON",
            success: function (res) {
                // console.log(res);
                records = res["outputMap"];
                $("#output").empty();
                for (var record in records) {
                    $("#output").append("<option value='"+record+"'>"+record+"</option>")
                }
                var pso_time = res["pso_time"];
                var ga_time = res["ga_time"];
                $("#output-time").text("PSO:"+pso_time+"ms     GA:"+ga_time+"ms");
                showTable();

                if (url == 'simulation') {
                    if (al_array[0] == 'PSO' || al_array['0'] == 'GA') {
                        var x_num = res["x"];
                        var x = [];
                        for (var i=0; i<x_num; i++) {
                            x[i] = i;
                        }
                        var y = res["y"];
                        var char_json = new Object();
                        char_json.x = x;
                        char_json.y = y;
                        char_json.x_name = "Iterations";
                        char_json.y_name = optimize_objective;
                        $("#chart_content").text(JSON.stringify(char_json));

                        layer.open({
                            type: 2
                            , offset: "140px"
                            , title: "FogWorkflowSim simulation result"
                            , content: "/lineChart"
                            , skin: 'title-style'
                            , area: ['1000px', '580px']
                            ,cancel: function(){
                                // feedSetting();
                            }
                        });
                    }
                } else if (url == 'compare') {
                    var head = ['product', 'Time', 'Energy', 'Cost'];
                    var list = res["record"];
                    var source = [];
                    source.push(head);
                    for (var i=0; i<list.length; i++) {
                        var content = list[i];
                        if (content[0] == 1) content[0] = 'MINMIN';
                        if (content[0] == 2) content[0] = 'MAXMIN';
                        if (content[0] == 3) content[0] = 'FCFS';
                        if (content[0] == 4) content[0] = 'ROUNDROBIN';
                        if (content[0] == 5) content[0] = 'PSO';
                        if (content[0] == 6) content[0] = 'GA';
                        source.push(list[i]);
                    }
                    $("#chart_content").text(JSON.stringify(source));

                    layer.open({
                        type: 2
                        , offset: "140px"
                        , title: "FogWorkflowSim simulation result"
                        , content: "/barChart"
                        , skin: 'title-style'
                        , area: ['1000px', '580px']
                        ,cancel: function(){
                            // feedSetting();
                        }
                    });
                }
            },
        });
    }

    // start simulation按钮
    $("#simulation").click(function () {
        if (alSet.size > 1) {
            tips("You chose more than one, please click the 'Compare' button!");
            return;
        } else if (alSet.size == 0) {
            tips("Please choose an Algorithm!");
            return;
        }
        var url = "simulation";
        start(url);
    });

    // compare按钮
    $("#compare").click(function () {
        if (alSet.size == 1) {
            tips("You selected one scheduling algorithm, please click the 'Start Simulation' button!");
            return;
        } else if (alSet.size == 0) {
            tips("Please select at least two algorithms!");
            return;
        }
        var url = "compare";
        start(url);
    });

    // 表格添加数据
    function showTable() {
        $(".tr-line").remove();
        var key = $("#output").val();
        var list = records[key];
        /*for (var i=0; i<list.length; i++) {
            var obj = list[i];
            var line = '<tr class="tr-line">'+
                    '<td>'+obj["jobId"]+'</td>'+
                    '<td>'+obj["taskId"]+'</td>'+
                    '<td>'+obj["status"]+'</td>'+
                    '<td>'+obj["dataCenterId"]+'</td>'+
                    '<td>'+obj["vmId"]+'</td>'+
                    '<td>'+obj["time"]+'</td>'+
                    '<td>'+obj["startTime"]+'</td>'+
                    '<td>'+obj["finishTime"]+'</td>'+
                    '<td>'+obj["depth"]+'</td>'+
                    '<td>'+obj["cost"]+'</td>'+
                    '<td>'+obj["parents"]+'</td>'+
                '</tr>';
            $("#output-table").append(line);

        }*/
        var parents = "";
        var html = '';
        for (var i=0; i<list.length; i++) {
            var obj = list[i];
            html += '<tr class="tr-line">'+
                '<td>'+obj["jobId"]+'</td>'+
                '<td>'+obj["taskId"]+'</td>'+
                '<td>'+obj["status"]+'</td>'+
                '<td>'+obj["dataCenterId"]+'</td>'+
                '<td>'+obj["vmId"]+'</td>'+
                '<td>'+obj["time"]+'</td>'+
                '<td>'+obj["startTime"]+'</td>'+
                '<td>'+obj["finishTime"]+'</td>'+
                '<td>'+obj["depth"]+'</td>'+
                '<td>'+obj["cost"]+'</td>';
            parents = obj["parents"];
            var temp = '';
            if(i%2 == 0){
                temp = '<td><input class="input_even" value="'+ parents +'" onblur="parents_blur(this)" onfocus="parents_focus(this)"></td></tr>';
            }else{
                temp = '<td><input class="input_odd" value="'+ parents +'" onblur="parents_blur(this)" onfocus="parents_focus(this)"></td></tr>';
            }
            html += temp;
        }
        $("#data_tbody").html(html);
    }

    {
        var html = '';
        for (var i=0; i<20; i++) {
            html += '<tr class="tr-line">'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>'+
                '<td></td>';

            var temp = '';
            if(i%2 == 0){
                temp = '<td><input class="input_even" value="" onblur="parents_blur(this)" onfocus="parents_focus(this)"></td></tr>';
            }else{
                temp = '<td><input class="input_odd" value="" onblur="parents_blur(this)" onfocus="parents_focus(this)"></td></tr>';
            }
            html += temp;

            // $("#output-table").append(line);
        }
        $("#data_tbody").html(html);

    }
    // output改变事件
    $("#output").change(function () {
        var flag = false;
        $(".tr-line").each(function(){
            var text = $(this).find("td:first").text();
            if(text == ""|| text ==null){
                flag = true;
                return false;
            }
        });
        if(!flag){
            showTable();
        }
    });

    // 对Date的扩展，将 Date 转化为指定格式的String
    // 月(M)、日(d)、小时(h)、分(m)、秒(s)、季度(q) 可以用 1-2 个占位符，
    // 年(y)可以用 1-4 个占位符，毫秒(S)只能用 1 个占位符(是 1-3 位的数字)
    Date.prototype.Format = function (fmt) { //author: meizz
        var o = {
            "M+": this.getMonth() + 1, //月份
            "d+": this.getDate(), //日
            "H+": this.getHours(), //小时
            "m+": this.getMinutes(), //分
            "s+": this.getSeconds(), //秒
            "q+": Math.floor((this.getMonth() + 3) / 3), //季度
            "S": this.getMilliseconds() //毫秒
        };
        if (/(y+)/.test(fmt)) fmt = fmt.replace(RegExp.$1, (this.getFullYear() + "").substr(4 - RegExp.$1.length));
        for (var k in o)
            if (new RegExp("(" + k + ")").test(fmt)) fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        return fmt;
    }

    // 导出表格数据
    $("#export").click(function () {
        var flag = false;
        $(".tr-line").each(function(){
            var text = $(this).find("td:first").text();
            if(text == ""|| text ==null){
                flag = true;
                tips("There is no data to export");
                return false;

            }
        });
        if(!flag){
            var filename = "";
            var algorithms_type = $("#output").text();
            var currentTime = new Date().Format("yyyy-MM-dd_HH:mm:ss");

            // console.log(currentTime);
            filename = currentTime + "_" + algorithms_type;
            $("#output-table").table2excel({
                exclude: ".noExl",//class="noExl"的列不导出
                name: "Excel Document Name",
                filename: filename,//文件名称
                fileext: ".xlsx",//文件后缀名
                exclude_img: true,//导出图片
                exclude_links: true,//导出超链接
                exclude_inputs: true//导出输入框内容
            });
        }

    });

    //可视化工作流
    $("#draw_workflow").click(function(){
        layer.open({
            type: 2
            , offset: "140px"
            , title: "Draw Workflow"
            , content: "drawWorkflow"
            , skin: 'title-style'
            , area: ['1400px', '780px']
            ,cancel: function(){
                // feedSetting();
            }
        });
    });

    var username = userJson['username'];
    //统计网页被访问次数
   $.ajax({
       url : "/getVisitCount",
       data : JSON.stringify({"username":username}),
       type : "POST",
       dataType : "JSON",
       contentType : "application/json;charset=utf-8",
       async : false,
       success : function(res){
                // console.log(res);
                var sum = res['sum'];
                var sum_today = res['sum_today'];
                var sum_user = res['sum_user'];
                var sum_user_today = res['sum_user_today'];
                var last_location = res['last_location'];
                $("#visitcount").children()[0].innerHTML = "总访问次数：" + sum + " || ";
                $("#visitcount").children()[1].innerHTML = "今日总访问次数：" + sum_today + " || ";
                $("#visitcount").children()[2].innerHTML = "您访问总次数：" + sum_user + " || ";
                $("#visitcount").children()[3].innerHTML = "今日您访问次数：" + sum_user_today + " || ";
                $("#visitcount").children()[4].innerHTML = "您上次登录地点：" + last_location;
       },
       error : function(res){
            // console.log(res);
            console.log("error");
       }
   })

});
function parents_blur(obj){
    if(obj.className.indexOf("input_even")){
        // console.log(obj.className);
        obj.className = "input_odd";
    }
    else{
        obj.className = "input_even";
    }
}
function parents_focus(obj){
    obj.className += ' input_border';
}