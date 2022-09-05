import com.alibaba.fastjson.JSONObject;
import org.xmlpull.v1.XmlPullParserException;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.CallGraphBuilder;
import soot.options.Options;
import soot.util.Chain;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.zip.ZipException;

public abstract class Analysis {
    private List<String> excludeList;
    private String defaultJarsPath;
    private String defaultApksPath;

    public List<String> getExcludeList() {
        return excludeList;
    }

    public void setExcludeList(List<String> excludeList) {
        this.excludeList = excludeList;
    }

    public String getDefaultJarsPath() {
        return defaultJarsPath;
    }

    public void setDefaultJarsPath(String defaultJarsPath) {
        this.defaultJarsPath = defaultJarsPath;
    }

    public String getDefaultApksPath() {
        return defaultApksPath;
    }

    public void setDefaultApksPath(String defaultApksPath) {
        this.defaultApksPath = defaultApksPath;
    }

    public Analysis() {
        List<String> list = Arrays.asList("android.", "java.", "javax.", "sun.", "com.google.", "oracle.", "io.netty");
        excludeList = new ArrayList<>(list);
        defaultJarsPath = "C:\\Users\\HP\\AppData\\Local\\Android\\Sdk\\platforms";
//        defaultApksPath = "D:\\code";
        defaultApksPath = "D:\\myWork\\User\\myWork\\App\\test";
    }

    public abstract <T> T analysis(String appName, String packageName, Chain<SootClass> classes);

    public <T> List<T> batchAnalysis() {
        List<T> res = new ArrayList<>();
        List<String> apks = Util.allAPK(GlobalConfig.defaultApksPath);
        for (String apk : apks) {
            try {
                long start = System.currentTimeMillis();
                setupSoot(GlobalConfig.defaultPlatform, GlobalConfig.defaultApkPath);
                T obj = analysis("appName", "appPackageName", Scene.v().getClasses());
                long end = System.currentTimeMillis();
                if (obj != null) res.add(obj);
                System.out.println("analyzed:" + "appName");
                int second = (int) ((end - start) / 1000);
                System.out.println("time costed:" + second + " s");

//                builder.getCallGraph().sourceMethods()
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }




    public void setupSoot(String jarsPath, String apkPath) {
        soot.G.reset();
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_format(Options.output_format_jimple);

        Options.v().set_validate(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_app(true);
        String androidJarPath = Scene.v().getAndroidJarPath(jarsPath, apkPath);

        List<String> pathList = new ArrayList<String>();
        pathList.add(apkPath);
        pathList.add(androidJarPath);

        Options.v().set_process_dir(pathList);
        Options.v().set_force_android_jar(androidJarPath);

        Options.v().set_prepend_classpath(true);
        Options.v().set_process_multiple_dex(true);

        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);
        //  要排除的
        //  Options.v().set_exclude(excludePackagesList);
        //  分析整个程序
        Options.v().set_whole_program(true);
        //忽略无法解析的类
        Options.v().set_allow_phantom_refs(true);
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
    }

    public boolean isExclude(String pkgName) {
        String lower = pkgName.toLowerCase();
        for (String innerPkg : excludeList) {
            if (lower.contains(innerPkg)) return true;
        }
        return false;
    }

    public void outputJson(String s, String path) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(path));
            out.write(s);
            out.close();
            System.out.println("文件创建成功！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}