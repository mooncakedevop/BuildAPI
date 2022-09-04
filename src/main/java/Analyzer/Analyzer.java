package Analyzer;

import Builder.Builder;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.util.Chain;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Analyzer {
    private Chain<SootClass> classes;
    private List<String> excludeList = Arrays.asList(new String[]{"android.", "androidx.","java.", "javax.", "sun.", "com.google.", "oracle.", "io."});
//    <java.net.URL: void <init>(java.lang.String,java.lang.String,int,java.lang.String)>
    private List<String> start = Arrays.asList(new String[]{"<okhttp3.Request$Builder: void <init>()>"});
    public Analyzer(Chain<SootClass> classes){
        this.classes = classes;
        forwardAnalyze(classes);
    }
    public void analyze(){
        forwardAnalyze(this.classes);
    }

    private void backwardAnalyze() {
    }

    private void forwardAnalyze(Chain<SootClass> classes) {
        for (SootClass cls :classes){
//            if (cls.getPackageName().contains("gosec") || cls.getName().contains("gosec")  ||cls.getPackageName().contains("example")||cls.getName().toLowerCase().contains("main")){
//                System.out.println(cls.getName());
//            }
            if (isExclude(cls.getPackageName())) continue;
            System.out.println(cls.getName());
            for (SootMethod method: cls.getMethods()){
                if (method.isPhantom() || method.isAbstract()) return;

                JimpleBody body = null;
                try {
                    if (method.hasActiveBody()){
                        body = (JimpleBody) method.retrieveActiveBody();
                        statementAnalysis(body);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    private void statementAnalysis(JimpleBody body) {
//        body.getUnits().snapshotIterator().next()
        BriefUnitGraph ug = new BriefUnitGraph(body);
        Iterator<Unit> it = body.getUnits().snapshotIterator();
        while (it.hasNext()){
            Unit u = it.next();
            u.apply(new AbstractStmtSwitch() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
//                    stmt.getInvokeExpr().getMethod()
                    super.caseInvokeStmt(stmt);
                    String sig =  stmt.getInvokeExpr().getMethod().toString();
                    if (start.get(0).equals(sig)){
                        System.out.println(sig);
                        List<Unit> succ =  ug.getSuccsOf(u);
                        identifyAttr(succ);
                    }

                }
            });
        }

    }

    private void identifyAttr(List<Unit> succ) {
        for (Unit u:succ){
            u.apply(new AbstractStmtSwitch() {
                @Override
                public void caseInvokeStmt(InvokeStmt stmt) {
                    super.caseInvokeStmt(stmt);
                    String methodSig = stmt.getInvokeExpr().getMethod().toString();
                    if (methodSig.equals("<okhttp3.Request$Builder: ava.lang.String url(java.lang.String)>")){
                        Value val = stmt.getInvokeExpr().getArg(0);
                        System.out.println(val.toString());
                    }
                }
            });
        }
    }

    private void checkAndBuild(String stmtMethodSig) {
//        for(String method:networkMehods){
//            if (method.equals(stmtMethodSig){
//                //重建属性
//            }
//        }
    }

    private Builder chooseBuilder(String toString) {
        return null;
    }



    public boolean isExclude(String name){
        for (String exclude: excludeList){
            if (name.contains(exclude)) return true;
        }
        return false;
    }
}
