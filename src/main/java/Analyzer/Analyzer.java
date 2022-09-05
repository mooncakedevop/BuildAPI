package Analyzer;

import Builder.Builder;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.ImmediateBox;
import soot.tagkit.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;

import java.util.*;

import static Analyzer.Retrofit.*;

public class Analyzer {
    //    <okhttp3.Request$Builder: void <init>()>()
    private List<String> excludeList = Arrays.asList(new String[]{"android.", "androidx.", "java.", "javax.", "sun.", "com.google.", "oracle.", "io."});
    //    <java.net.URL: void <init>(java.lang.String,java.lang.String,int,java.lang.String)>
    private List<String> start = Arrays.asList(new String[]{"<okhttp3.Request$Builder: void <init>()>"});
    private List<String> header = Arrays.asList(new String[]{"<okhttp3.Request$Builder: okhttp3.Request$Builder addHeader(java.lang.String,java.lang.String)>",
            "<okhttp3.Headers$Builder: okhttp3.Headers$Builder <add>(java.lang.String, java.lang.String)>"});
    private List<String> post = Arrays.asList(new String[]{"<okhttp3.Request$Builder: okhttp3.Request$Builder post(okhttp3.RequestBody)>"});

    public Analyzer(Chain<SootClass> classes) {
        forwardAnalyze(classes);
    }
//    public void analyze(){
//        forwardAnalyze(this.classes);
//    }

    private void backwardAnalyze() {
    }

    private void forwardAnalyze(Chain<SootClass> classes) {
        Iterator<SootClass> it = classes.stream().iterator();
        for (SootClass cls: classes) {
//
//            if (isExclude(cls.getPackageName()) || isExclude(cls.getName() )) continue;
            if (!cls.getName().contains("gosec"))continue;
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
                    MethodAnnotation((VisibilityAnnotationTag) tag);
                } else if (tag instanceof VisibilityParameterAnnotationTag) {
                    paramAnnotation((VisibilityParameterAnnotationTag) tag);
                }
            }

        }
    }


    private void statementAnalysis(JimpleBody body) {
//        body.getUnits().snapshotIterator().next()
        BriefUnitGraph ug = new BriefUnitGraph(body);
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
                        System.out.println(sig);
                        List<Unit> succ = ug.getSuccsOf(u);
                        identifyAttr(ug, succ,"okhttp");
                    }else if (sig.equals(RetrofitBuilder)){
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
                            System.out.println(sig);
                            List<Unit> succ = ug.getSuccsOf(u);
                            identifyAttr(ug, succ, "okhttp");
                        } else if (sig.equals(RetrofitBuilder)) {
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
        if (methodSig.equals("<okhttp3.Request$Builder: okhttp3.Request$Builder url(java.lang.String)>")) {
            ValueBox arg = invokeExpr.getArgBox(0);
            if (arg.getValue() instanceof StringConstant) {
//
                System.out.println("url:" + arg);
            } else {
                System.out.println("start pointer");

            }
        } else if (methodSig.equals(header.get(0))) {
            System.out.println("header:" + invokeExpr.getArg(0) + "  :" + invokeExpr.getArg(1));
        } else if (methodSig.equals(post.get(0))) {
            System.out.println("post: " + invokeExpr.getArg(0));
        }
    }

    public void buildRetrofit(String methodSig, InvokeExpr invokeExpr){
        if (methodSig.equals("<retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder baseUrl(java.lang.String)>")) {
            ValueBox arg = invokeExpr.getArgBox(0);
            if (arg.getValue() instanceof StringConstant) {
//
                System.out.println("baseurl:" + arg.getValue());
            } else {
                System.out.println("baseurl start pointer");

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
