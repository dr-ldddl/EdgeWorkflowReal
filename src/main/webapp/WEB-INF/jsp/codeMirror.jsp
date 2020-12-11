<%--<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>CodeMirror</title>
    <meta charset="utf-8">
    <link rel="stylesheet" href="/layui/css/layui.css">
    <link rel="stylesheet" href="/css/codeMirror.css">
</head>
<body>

CodeMirror

</body>
<script src="/jquery/jquery-1.9.1.min.js"></script>
<script type="text/javascript" src="/layui/layui.all.js"></script>
<script type="text/javascript" src="/layui/layui.js"></script>
<script type="text/javascript" src="/js/codeMirror.js"></script>
</html>--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>代码在线运行工具</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@4.4.1/dist/css/bootstrap.min.css" integrity="sha384-Vkoo8x4CGsO3+Hhxv8T/Q5PaXtkKtu6ug5TOeNV6gBiFeWPGFN9MuhOf23Q9Ifjh" crossorigin="anonymous">
    <style>
        #editor {
            position: absolute;
            width: 100%;
            height: 100%;
        }
    </style>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-light bg-light">
    <div class="container">
        <a class="navbar-brand" href="/">代码在线运行工具</a>
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
    </div>
</nav>
<div style="height: 30px"></div>
<div class="container shadow p-3 mb-5 bg-white rounded">
    <div class="container-fluid">
        <div class="row">
            <div class="col-2">
                <button id="sub-btn" class="btn btn-success " onclick="submit()">点击运行！</button>
            </div>
            <div class="col-3">
                <select onchange="selectLanguage(this)" id="language-type" class="form-control">
                    <option selected>Java</option>
                    <option>C</option>
                    <option>CPP</option>
                    <option>Python</option>
                </select>
            </div>
            <div class="col-3">
                <button type="button" class="btn btn-secondary" onclick="clean()">清空</button>
            </div>
        </div>
    </div>
    <div style="height: 20px"></div>

    <div class="row">
        <div class="col-7 border border-light">
            <div id="editor"></div>
        </div>
        <div class="col-1 border-left"></div>
        <div class="col text-center">
            <textarea id="output" class="form-control" rows="15"></textarea>
        </div>
    </div>
</div>
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.8/ace.js" type="text/javascript"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.8/ext-language_tools.min.js" type="text/javascript"></script>
<!--<script src="https://cdnjs.cloudflare.com/ajax/libs/ace/1.4.8/mode-java.min.js" type="text/javascript"></script>-->
<script>
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

    function submit() {
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
    }

    function clean() {
        editor.setValue("");
    }

    function selectLanguage(e) {
        let mode = "ace/mode/" + e.value.toLowerCase();
        if (e.value.toLowerCase() === "c" || e.value.toLowerCase() === "cpp") {
            mode = "ace/mode/c_cpp"
        }
        editor.session.setMode(mode);
    }
</script>
</body>
</html>
