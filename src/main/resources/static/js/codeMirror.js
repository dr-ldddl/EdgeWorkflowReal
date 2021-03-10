ace.require("ace/ext/language_tools");
const editor = ace.edit("editor");
editor.session.setMode("ace/mode/java");
editor.setTheme("ace/theme/github");
// enable autocompletion and snippets
editor.setOptions({
    enableBasicAutocompletion: true,
    enableSnippets: true,
    enableLiveAutocompletion: true
});


function runCode(){
    var data = editor.getValue();
    // console.log(data);
    $.post("/CodeRunResult",{
            source:data,
            systemIn: "123"
        },
        function(data,status){
            // console.log(data);
            // console.log(status);
            // var re = new RegExp("<br/>","g");
            // data = data.replace(re,"\n");
            // console.log(data);
            $("#output").val(data);

        });

}

/*function submit() {
    document.querySelector("#output").value = "代码运行中！";
    let data = editor.getValue();

    fetch("http://127.0.0.1:8848/run", {
        method: "POST",
        headers: {
            "Content-Type": "application/json; charset=UTF-8"
        },
        body: JSON.stringify({
            code: data,
            type: document.querySelector("#language-type").value.toUpperCase()
        })

    }).then(response => response.json())
.then(json => {
        console.log(json);
    document.querySelector("#output").value = json.output;
});
}*/

function clean() {
    editor.setValue("");
}

function submitCode(){
    var username = parent.$("#username_a").text();
    var planName = parent.$("#planName").val();
    var workflowName = parent.$("#workflowName").val();
    var taskName = parent.$("#taskName").val();
    taskName = "task_" + taskName;

    var code = editor.getValue();
    // console.log(code);


    var CodeJson = {"username" : username , "planName" : planName, "workflowName" : workflowName, "taskName" : taskName, "code" : code};
    // console.log(CodeJson);
    $.ajax({
        type: "POST",
        url: "/submitCode",
        data: {codeJson: JSON.stringify(CodeJson)},
        async: true,
        dataType:"text",
        success: function (res) {
            if(res == "success"){
                //获取窗口索引
                var index = parent.layer.getFrameIndex(window.name);
                //关闭当前页面
                parent.layer.close(index);
                // parent.location.reload();

                parent.layer.msg("写入代码成功!",
                    {icon: 1,offset:['40%', '30%'],time:1000,area:['450px','70px']});
            }else{
                layer.msg("写入代码失败!",
                    {icon: 2,offset:['40%', '30%'],time:1000,area:['450px','70px']});
            }

            parent.$("#flushChildrenDag").click();
        },
    });

}

function selectLanguage(e) {
    var mode = "ace/mode/" + e.value.toLowerCase();
    if (e.value.toLowerCase() === "c" || e.value.toLowerCase() === "cpp") {
        mode = "ace/mode/c_cpp"
    }
    editor.session.setMode(mode);
}

$(document).ready(function(){
    /*layer.msg("There is no simulation data , Please simulate first!",
        {icon: 2,offset:['50%', '40%'],time:3000,area:['420px','70px']});*/
    // editor.setValue(code_init);

    var username = parent.$("#username_a").text();
    var planName = parent.$("#planName").val();
    var workflowName = parent.$("#workflowName").val();
    var taskName = parent.$("#taskName").val();
    var editFlag = parent.$("#editFlag").val();

    // console.log(workflowName);
    // console.log(taskName);
    // console.log(editFlag);

    //初始化代码
    if(editFlag == "false"){
        var param = {"taskName": taskName , "editFlag" : editFlag , "workflowName" : workflowName};
        initCode(param);
    }else{

        //需要修改
        // var username = "dr";
        // var planName = "drtest";
        var param = {"username" : username , "planName" : planName, "workflowName" : workflowName, "taskName" : taskName};
        initExitCode(param);
    }




});

//初始化代码
function initCode(param){

    $.ajax({
        type: "POST",
        url: "/initCode",
        data: param,
        async: true,
        dataType:"text",
        success: function (res) {
            // console.log(res);
            editor.setValue(res);
        },
    });
}

//加载已有代码
function initExitCode(param) {

    $.ajax({
        type: "POST",
        url: "/initExitCode",
        data: {"codeJson" : JSON.stringify(param)},
        async: true,
        dataType:"text",
        success: function (res) {
            // console.log(res);
            editor.setValue(res);
        },
    });
}