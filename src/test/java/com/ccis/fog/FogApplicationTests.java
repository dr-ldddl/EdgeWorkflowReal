package com.ccis.fog;

import java.io.*;//导入所需的包
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class FogApplicationTests {

    @Autowired
    private IndexService indexService;
    @Test
	void contextLoads() {
        String source = "public class Man {\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tSystem.out.println(\"hello world 0\");\n" +
                "\t\tSystem.out.println(\"hello world 1\");\n" +
                "\t\tSystem.out.println(\"hello world 2\");\n" +
                "\t\tSystem.out.println(\"hello world 3\");\n" +
                "\t\tSystem.out.println(\"hello world 4\");\n" +
                "\t}\n" +
                "}";
        String source1 = "public class Man {\n" +
                "\tpublic static void main(String[] args) throws InterruptedException {\n" +
                "\t\tSystem.out.println(\"hello world 1\");\n" +
                "\t\tThread.sleep(5000);\n" +
                "\t\tSystem.out.println(\"hello world 1\");\n" +
                "\t}\n" +
                "}";
        String source2 = "public class Man {\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tSystem.out.println(\"hello world 2\");\n" +
                "\t\tSystem.out.println(\"hello world 2\");\n" +
                "\t}\n" +
                "}";

        // 测试 Scanner in = new Scanner(System.in);
        String sourceTestSystemIn = "import java.util.Scanner;\n" +
                "public class Run {\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tScanner in = new Scanner(System.in);\n" +
                "\t\tSystem.out.println(in.nextInt());\n" +
                "\t\tSystem.out.println(in.nextDouble());\n" +
                "\t\tSystem.out.println(in.next());\n" +
                "\t}\n" +
                "}";

        String systemIn = "1 1.5 \n fsdfasdfasdf";

        // Test2
        String sourceTestSystemIn1 = "import java.util.*;\n" +
                "public class Run {\n" +
                "\tpublic static void main(String[] args) {\n" +
                "\t\tScanner in = new Scanner(System.in);\n" +
                "\t}\n" +
                "}";

        String systemIn1 = "";


        String res;
//        res = executeStringSourceService.execute(sourceTestSystemIn1, systemIn1);
        res = indexService.execute(source, systemIn1);
        System.out.println("---------- Begin ----------");
        System.out.print(res);
        System.out.println("----------- End -----------");
	}


	@Test
    void createFile(){
        String path = "F:\\ChangeTask\\test";
        File file = new File(path);
        if(!file.exists()){//如果文件夹不存在
            file.mkdir();//创建文件夹
        }

        try{//异常处理
            //如果Qiju_Li文件夹下没有Qiju_Li.txt就会创建该文件
            BufferedWriter bw=new BufferedWriter(new FileWriter(path + "\\test.java"));
            bw.write("Hello I/O!");//在创建好的文件中写入"Hello I/O"
            bw.close();//一定要关闭文件
        }catch(IOException e){
            e.printStackTrace();
        }

    }

}
