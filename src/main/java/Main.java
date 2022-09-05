import Analyzer.Analyzer;
import soot.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;
import soot.util.Chain;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Main {
    public void setupSoot(){
        soot.G.reset();
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_output_format(Options.output_format_jimple);

        Options.v().set_validate(true);
        Options.v().set_keep_line_number(true);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_app(true);
        String androidJarPath = Scene.v().getAndroidJarPath(GlobalConfig.defaultPlatform, GlobalConfig.defaultApkPath);

        List<String> pathList = new ArrayList<String>();
        pathList.add(GlobalConfig.defaultApkPath);
        pathList.add(androidJarPath);

        Options.v().set_process_dir(pathList);
        Options.v().set_force_android_jar(androidJarPath);

        Options.v().set_prepend_classpath(true);
        Options.v().set_process_multiple_dex(true);

        Options.v().set_wrong_staticness(Options.wrong_staticness_ignore);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
//        Options.v().set_output_dir(System.getProperty("user.dir") + File.separator + "output");
        Scene.v().loadNecessaryClasses();
        PackManager.v().runPacks();
//        PackManager.v().writeOutput();
    }
    public static void main(String[] args){
        Main main = new Main();
        main.setupSoot();
        Analyzer aly  = new Analyzer(Scene.v().getClasses());
    }
}
