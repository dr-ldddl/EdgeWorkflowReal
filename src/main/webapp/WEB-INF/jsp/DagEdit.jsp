<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>DAG Task Edit</title>
    <link rel="stylesheet" href="/layui/css/layui.css">
    <link rel="stylesheet" type="text/css" href="/layui/css/jquery-ui-1.10.4.min.css">
    <link rel="stylesheet" type="text/css" href="/mouseRightMenu/mouseRightMenu.css">
</head>
<body>
<button id="createContainer" class="layui-btn layui-btn-radius layui-btn-normal">生成Container</button>
<div id="dagHelpShow"></div>

<input id="taskName" hidden>
<input id="workflowName" hidden>
<input id="editFlag" hidden>
</body>
<script type="text/javascript" src="/jquery/jquery.min.js"></script>
<script type="text/javascript" src="/layui/layui.all.js"></script>
<script type="text/javascript" src="/layui/layui.js"></script>
<%--<script type="text/javascript" src="/echarts/echarts.min.js"></script>--%>
<script type="text/javascript" src="/echarts/echarts-en.min.js"></script>
<script type="text/javascript" src="/mouseRightMenu/mouseRightMenu.js"></script>
<script>
//注意：选项卡 依赖 element 模块，否则无法进行功能性操作
layui.use('element', function(){
    var element = layui.element;

    //…
});

var taskList;
var points;
var links;
var dagFlag = false;
var taskFlag = false;

$(document).ready(function(){
    var dagJson = {dagXml:"Montage_20.xml",customXml:""};
    // var dagJson = {dagXml:"Montage_20.xml",customXml:"line3_3.xml"};
    // console.log(dagParam);
    // var dagJson = eval("("+dagParam+")");
    var dagXML = dagJson["dagXml"];
    $("#workflowName").val(dagXML);



    var username = "dr";
    var planName = "drtest";
    var workflowName = $("#workflowName").val();
    var taskListParam = {"username": username , "planName" : planName , "workflowName" : workflowName};

    //获得所有已经存在编辑代码文件的工作流节点。
    getTaskList(taskListParam);

    //绘制DAG图
    getDag(dagJson);

    //绘制工作流DAG
    // console.log(dagFlag);
    // console.log(taskFlag);
    if(dagFlag && taskFlag){
        loadDag(points , links);
    }


});

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

        },
        error: function(data){
            console.log("error...");
        }
    });
}

//加载dag图
function loadDag(/*points, links*/){
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

    var dagChart = echarts.init(document.getElementById('dagHelpShow'));
    dagChart.clear();
    dagChart.setOption(option);
    //DAG图中元素左击事件
    dagChart.on("click" , function (elem) {
        // console.log(elem);
        // console.log(elem.dataIndex);
        click(elem);
    });
    //DAG图中元素右击事件
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
    var taskName = $("#taskName");
    var editFlag = $("#editFlag");

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

        layer.open({
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

    /*console.log("contextmenu:");
    var name = task["name"];
    var type = task['dataType'];
    var taskName = $("#taskName");

    // console.log(task);
    console.log("name:" + name);
    console.log("dataType:" + type);

    if(type == "node"){
        taskName.val(name);

        layer.open({
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
        {'data':"data",'type':1,'title':'右键操作1'},
        {'data':"data",'type':2,'title':'右键操作2'},
        {'data':"data",'type':3,'title':'右键操作3'},
        {'data':"data",'type':4,'title':'右键操作4'},

    ]
    mouseRightMenu.open(menu_data,false,function(d){
        layer.alert(JSON.stringify(d));
    })
    return false;

}

//测试在linux中生成容器
$("#createContainer").click(function(){
    var username = "dr";
    var planName = "drtest";
    var workflowName = $("#workflowName").val();
    var containerParam = {"username": username , "planName" : planName , "workflowName" : workflowName};

    $.ajax({
        type: "POST",
        url: "/createContainer",
        data: {"containerParam" : JSON.stringify(containerParam)},
        dataType: "text",
        async: false,
        success: function (data) {
            console.log(data);
        },
        error: function(data){
            console.log("error...");
        }
    });
});
</script>
<style>
body{
    background-color: #FFFFFF;
}

#dagHelpShow{
    position: relative;
    width: 99%;
    height: 99%;
    border: #aab9c3 solid 1px;
    border-radius: 10px 10px 10px 10px;
    background-color: #FFFFFF;
    /*display: none;*/
}
</style>
</html>