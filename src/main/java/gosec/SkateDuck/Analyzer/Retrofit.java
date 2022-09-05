package gosec.SkateDuck.Analyzer;

import gosec.SkateDuck.Util;
import soot.SootMethod;
import soot.tagkit.*;

public class Retrofit {

    static String RetrofitBuilder = "<retrofit2.Retrofit$Builder: void <init>()>";
    static String BaseUrlSig = "<retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder baseUrl(java.lang.String)>";
    private static String baseUrl = "";

    public static void paramAnnotation(VisibilityParameterAnnotationTag tag, String name, String path) {
        for (VisibilityAnnotationTag v : tag.getVisibilityAnnotations()) {
            if (v == null) break;
            for (AnnotationTag a : v.getAnnotations()) {
                for (AnnotationElem elem : a.getElems()) {
                    AnnotationStringElem stringElem = (AnnotationStringElem) elem;
                    Util.output(path, name, "param type is " + a.getType() + "param name is: " + stringElem.getValue());
                }
            }
        }
    }

    public static void MethodAnnotation(VisibilityAnnotationTag tag, String name, String path) {
        for (AnnotationTag annotation : tag.getAnnotations()) {
            if (annotation.getType().equals("Lretrofit2/http/GET;") || annotation.getType().equals("Lretrofit2/http/POST;")) {
                Util.output(path, name, "request type : " + annotation.getType());
                for (AnnotationElem e : annotation.getElems()) {
                    AnnotationStringElem elem = (AnnotationStringElem) e;
                    Util.output(path, name, "request path : " + elem.getValue());
                }
            }
        }
    }
}
