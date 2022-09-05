package Analyzer;

import soot.SootMethod;
import soot.tagkit.*;

public class Retrofit {

    static String RetrofitBuilder = "<retrofit2.Retrofit$Builder: void <init>()>";
    static String BaseUrlSig = "<retrofit2.Retrofit$Builder: retrofit2.Retrofit$Builder baseUrl(java.lang.String)>";
    private static String baseUrl = "";

    public static void paramAnnotation(VisibilityParameterAnnotationTag tag) {
        for (VisibilityAnnotationTag v : tag.getVisibilityAnnotations()) {
            if (v == null) break;
            for (AnnotationTag a : v.getAnnotations()) {
                for (AnnotationElem elem : a.getElems()) {
                    AnnotationStringElem stringElem = (AnnotationStringElem) elem;
                    System.out.println("param type is " + a.getType() + "param name is: " + stringElem.getValue());
                }
            }
        }
    }

    public static void MethodAnnotation(VisibilityAnnotationTag tag) {
        for (AnnotationTag annotation : tag.getAnnotations()) {
            if (annotation.getType().equals("Lretrofit2/http/GET;") || annotation.getType().equals("Lretrofit2/http/POST;")) {
                System.out.println(annotation.getType());
                for (AnnotationElem e : annotation.getElems()) {
                    AnnotationStringElem elem = (AnnotationStringElem) e;
                    System.out.println("request path : " + elem.getValue());
                }
            }
        }
    }
}
