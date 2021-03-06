package per.wsj.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import per.wsj.annotation.JudgeLogin;
import per.wsj.annotation.LoginActivity;
import per.wsj.annotation.RequireLogin;

@AutoService(Processor.class)
@SupportedOptions("room.schemaLocation")
public class RequireLoginProcessor extends AbstractProcessor {

    private String pkName = "me.wsj.login.apt";

    private Messager mMessager;

    private List<String> activityList;

    private String loginActivity;

    private String judgeLoginMethod;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnv.getMessager();
        activityList = new ArrayList<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(RequireLogin.class.getCanonicalName());
        supportTypes.add(LoginActivity.class.getCanonicalName());
        supportTypes.add(JudgeLogin.class.getCanonicalName());
        return supportTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (set.isEmpty()) {
            return false;
        }
        mMessager.printMessage(Diagnostic.Kind.WARNING, "\nprocessing...\n");
        // 1?????????????????????????????????Activity????????????List???
        parseAnno(roundEnvironment);

        // 2???????????????NeedLogin??????
        TypeSpec typeSpec = TypeSpec.classBuilder("AndLoginUtils")
                .addModifiers(Modifier.PUBLIC)
                // 3?????????????????????list?????????
                .addMethod(createNeedLoginFun())
                // ????????????activity????????????
                .addMethod(createLoginActivityFun())
                .addMethod(createJudgeLoginFun())
                .build();

        // 4?????????????????????per.wsj.gitstar.apt
        JavaFile javaFile = JavaFile.builder(pkName, typeSpec).build();
        try {
            // 5???????????????
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMessager.printMessage(Diagnostic.Kind.WARNING, "\nprocess finish ...\n");
        return true;// ??????false?????????????????????
    }

    /**
     * ?????????????????????Activity,?????????
     *
     * @param roundEnv
     */
    private void parseAnno(RoundEnvironment roundEnv) {
        activityList.clear();
        // ?????????????????????NeedLogin?????????
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(RequireLogin.class);
        for (Element element : elements) {
            // ???????????????????????????class.  ??????????????????instanceof TypeElement????????????????????????????????????TypeElement.
            if (element.getKind() != ElementKind.CLASS) {
                mMessager.printMessage(Diagnostic.Kind.WARNING,
                        element.getSimpleName().toString() + "????????????????????????");
                continue;
            }
            // ????????????????????????TypeElement
            TypeElement classElement = (TypeElement) element;
            // ??????+??????:per.wsj.gitstar.ui.activity.EventActivity
            String fullClassName = classElement.getQualifiedName().toString();
            activityList.add(fullClassName);
        }

        // ???????????????Activity
        Set<? extends Element> loginActivityElements = roundEnv.getElementsAnnotatedWith(LoginActivity.class);
        for (Element loginActivityElement : loginActivityElements) {
            if (loginActivityElement.getKind() != ElementKind.CLASS) {
                mMessager.printMessage(Diagnostic.Kind.WARNING,
                        loginActivityElement.getSimpleName().toString() + "????????????????????????");
                continue;
            }
            // ????????????????????????TypeElement
            TypeElement classElement = (TypeElement) loginActivityElement;
            // ??????+??????
            String fullClassName = classElement.getQualifiedName().toString();
            loginActivity = fullClassName;
        }

        // ?????????????????????????????????
        Set<? extends Element> judgeLoginElements = roundEnv.getElementsAnnotatedWith(JudgeLogin.class);
        for (Element element : judgeLoginElements) {
            if (element instanceof ExecutableElement) {
                mMessager.printMessage(Diagnostic.Kind.WARNING,
                        "\n?????????????????????:" + element.getSimpleName());
                ExecutableElement method = (ExecutableElement) element;
                TypeElement classElement = (TypeElement) method.getEnclosingElement();
                mMessager.printMessage(Diagnostic.Kind.WARNING, "\n????????????????????????" + classElement.getQualifiedName().toString());
                String classPath = classElement.getQualifiedName().toString();
                if (classPath.endsWith("Companion")) {
                    continue;
                }
                judgeLoginMethod = classPath + "#" + element.getSimpleName();
            }
        }
    }

    /**
     * ??????????????????????????????
     */
    private MethodSpec createNeedLoginFun() {
        ClassName arrayList = ClassName.get("java.util", "ArrayList");
        // ??????????????? List<String>
        TypeName listOfView = ParameterizedTypeName.get(List.class, String.class);

        // ????????????getViewAnno?????????
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getRequireLoginList")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(listOfView);
        // List<String> result = new ArrayList<>();
        methodBuilder.addStatement("$T result = new $T<>()", listOfView, arrayList);
        for (String s : activityList) {
            // result.add("per.wsj.gitstar.ui.activity.EventActivity");
            methodBuilder.addStatement("result.add(\"" + s + "\")");
        }
        // return result;
        methodBuilder.addStatement("return result");
        return methodBuilder.build();
    }

    /**
     * ???????????????activity
     *
     * @return
     */
    private MethodSpec createLoginActivityFun() {
        ClassName stringName = ClassName.get(String.class);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getLoginActivity")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(stringName);

        methodBuilder.addStatement("return \"" + loginActivity + "\"");
        return methodBuilder.build();
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    private MethodSpec createJudgeLoginFun() {
        ClassName stringName = ClassName.get(String.class);
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getJudgeLoginMethod")
                .addModifiers(Modifier.PUBLIC)
                .addModifiers(Modifier.STATIC)
                .returns(stringName);

        methodBuilder.addStatement("return \"" + judgeLoginMethod + "\"");
        return methodBuilder.build();
    }
}
