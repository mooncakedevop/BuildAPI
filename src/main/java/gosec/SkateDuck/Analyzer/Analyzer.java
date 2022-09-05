package gosec.SkateDuck.Analyzer;

import gosec.SkateDuck.Builder.Builder;
import gosec.SkateDuck.Util;
import soot.*;
import soot.jimple.*;
import soot.tagkit.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;

import java.io.File;
import java.util.*;

import static gosec.SkateDuck.Analyzer.Retrofit.*;

public class Analyzer {
    //    <okhttp3.Request$Builder: void <init>()>()
    private List<String> excludeList = Arrays.asList(new String[]{"android.", "androidx.", "java.", "javax.", "sun.", "com.google.", "oracle.", "io."});
    //    <java.net.URL: void <init>(java.lang.String,java.lang.String,int,java.lang.String)>
    private List<String> start = Arrays.asList(new String[]{"<okhttp3.Request$Builder: void <init>()>"});
    private List<String> header = Arrays.asList(new String[]{"<okhttp3.Request$Builder: okhttp3.Request$Builder addHeader(java.lang.String,java.lang.String)>",
            "<okhttp3.Headers$Builder: okhttp3.Headers$Builder <add>(java.lang.String, java.lang.String)>"});
    private List<String> post = Arrays.asList(new String[]{"<okhttp3.Request$Builder: okhttp3.Request$Builder post(okhttp3.RequestBody)>"});

    private String pkg;
    private String outputPath;
    public Analyzer(Chain<SootClass> classes, String pkg) {
        this.pkg = pkg;
        outputPath = System.getProperty("user.dir") + File.separator + "output" + File.separator + pkg + ".txt";
        forwardAnalyze(classes);
    }

    private void backwardAnalyze() {
    }

    private void forwardAnalyze(Chain<SootClass> classes) {
        Iterator<SootClass> it = classes.stream().iterator();
        for (SootClass cls: classes) {
//
            if (!cls.getPackageName().equals( pkg)) continue;
            if (cls.isInterface()) {
                checkAnnotation(cls);
                continue;
            }
            for (SootMethod method : cls.getMethods()) {
                if (method.isPhantom()) continue;
                JimpleBody body = null;
                try {
                        body = (JimpleBody) method.retrieveActiveBody();
                        statementAnalysis(body);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    private void checkAnnotation(SootClass cls) {
        for (SootMethod method : cls.getMethods()) {
            for (Tag tag : method.getTags()) {
                if (tag instanceof VisibilityAnnotationTag) {
                    MethodAnnotation((VisibilityAnnotationTag) tag, method.getName(),outputPath);
                } else if (tag instanceof VisibilityParameterAnnotationTag) {
                    paramAnnotation((VisibilityParameterAnnotationTag) tag, method.getName(), outputPath);
                }
            }

        }
    }


    private void statementAnalysis(JimpleBody body) {
//        body.getUnits().snapshotIterator().next()
        Iterator<Unit> it = body.getUnits().snapshotIterator();
        while (it.hasNext()) {
            Unit u = it.next();
            u.apply(new AbstractStmtSwitch() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
//                    stmt.getInvokeExpr().getMethod()
                    super.caseInvokeStmt(stmt);
                    String sig = stmt.getInvokeExpr().getMethod().toString();
                    if (start.get(0).equals(sig)) {
                        BriefUnitGraph ug = new BriefUnitGraph(body);
                        System.out.println(sig);
                        List<Unit> succ = ug.getSuccsOf(u);
                        identifyAttr(ug, succ,"okhttp");
                    }else if (sig.equals(RetrofitBuilder)){
                        BriefUnitGraph ug = new BriefUnitGraph(body);
                        identifyAttr(ug, ug.getSuccsOf(u),"retrofit");
                    }

                }


                @Override
                public void caseAssignStmt(AssignStmt stmt) {
                    if (stmt.getRightOp() instanceof InvokeExpr) {
                        InvokeExpr invokeExpr = (InvokeExpr) stmt.getRightOp();
                        String sig = invokeExpr.getMethodRef().getSignature();
//                        okHttp
                        if (sig.equals(OkHttp.requestBuilder)) {
                            BriefUnitGraph ug = new BriefUnitGraph(body);

                            System.out.println(sig);
                            List<Unit> succ = ug.getSuccsOf(u);
                            identifyAttr(ug, succ, "okhttp");
                        } else if (sig.equals(RetrofitBuilder)) {
                            BriefUnitGraph ug = new BriefUnitGraph(body);

                            System.out.println("find retrofit");
                            identifyAttr(ug, ug.getSuccsOf(u), "retrofit");
                        }
                    }
                }
            });
        }

    }



    private void identifyAttr(BriefUnitGraph ug, List<Unit> succ, String frame) {

        for (Unit u : succ) {
            Stmt s = (Stmt) u;
            if (s instanceof InvokeExpr) {
                InvokeExpr invokeExpr = (InvokeExpr) s;
                String methodSig = invokeExpr.getMethod().toString();
                checkAndBuild(methodSig, invokeExpr, frame);
            } else if (s instanceof AssignStmt) {
                Value val = ((AssignStmt) s).getRightOp();

                if (val instanceof InvokeExpr) {
                    InvokeExpr invokeExpr = (InvokeExpr) val;
                    String methodSig = ((InvokeExpr) val).getMethodRef().getSignature();
                    checkAndBuild(methodSig, invokeExpr, frame);
                }
            }

            if (ug.getSuccsOf(u).size() > 0) identifyAttr(ug, ug.getSuccsOf(u),frame);
        }
    }
    public void buildOkHttp(String methodSig, InvokeExpr invokeExpr){
        if (methodSig.equals(OkHttp.url)) {
            ValueBox arg = invokeExpr.getArgBox(0);
            if (arg.getValue() instanceof StringConstant) {
                Util.output(outputPath,invokeExpr.getMethod().getName(),"url:" + arg);

            } else {
                Util.output(outputPath,invokeExpr.getMethod().getName(),"start pointer");
            }
        } else if (methodSig.equals(OkHttp.addHeader)) {
            Util.output(outputPath,invokeExpr.getMethod().getName(),"header:" + invokeExpr.getArg(0) + "  :" + invokeExpr.getArg(1));
        } else if (methodSig.equals(post.get(0))) {
            Util.output(outputPath,invokeExpr.getMethod().getName(),"post: " + invokeExpr.getArg(0));
        }
    }

    public void buildRetrofit(String methodSig, InvokeExpr invokeExpr){
        if (methodSig.equals(BaseUrlSig)) {
            ValueBox arg = invokeExpr.getArgBox(0);
            if (arg.getValue() instanceof StringConstant) {
                Util.output(outputPath,invokeExpr.getMethod().getName(),"baseurl:" + arg.getValue());
            } else {
                Util.output(outputPath,invokeExpr.getMethod().getName(),"baseurl start pointer");
            }
        }
    }

    private void checkAndBuild(String methodSig, InvokeExpr invokeExpr, String frame) {
        switch (frame){
            case "okhttp":
                buildOkHttp(methodSig, invokeExpr);
                break;
            case "retrofit":
                buildRetrofit(methodSig,invokeExpr);
                break;
            default:
                break;
        }

    }

    private Builder chooseBuilder(String toString) {
        return null;
    }


    public boolean isExclude(String name) {
        for (String exclude : excludeList) {
            if (name.contains(exclude)) return true;
        }
        return false;
    }
}
