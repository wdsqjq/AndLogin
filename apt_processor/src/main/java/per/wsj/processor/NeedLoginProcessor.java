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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

import per.wsj.annotation.NeedLogin;

@AutoService(Processor.class)
@SupportedOptions("room.schemaLocation")
public class NeedLoginProcessor extends AbstractProcessor {

    private Messager mMessager;

    private List<String> tempList;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mMessager = processingEnv.getMessager();
        tempList = new ArrayList<>();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportTypes = new LinkedHashSet<>();
        supportTypes.add(NeedLogin.class.getCanonicalName());
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
        mMessager.printMessage(Diagnostic.Kind.WARNING, "processing...");
        // 1，获取所有添加了注解的Activity，保存到List中
        parseAnno(roundEnvironment);

        // 2，创建名为NeedLogin的类
        TypeSpec typeSpec = TypeSpec.classBuilder("NeedLogin")
                .addModifiers(Modifier.PUBLIC)
                // 3，添加获取类的list的方法
                .addMethod(createNeedLoginFun())
                .build();

        // 4，设置包路径：per.wsj.gitstar.apt
        JavaFile javaFile = JavaFile.builder("per.wsj.login.apt", typeSpec).build();
        try {
            // 5，生成文件
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMessager.printMessage(Diagnostic.Kind.WARNING, "process finish ...");
        return true;// 返回false则只会执行一次
    }

    /**
     * 获取所有注解的Activity,并保存
     *
     * @param roundEnv
     */
    private void parseAnno(RoundEnvironment roundEnv) {
        tempList.clear();
        // 得到所有注解为NeedLogin的元素
        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(NeedLogin.class);
        for (Element element : elements) {
            // 检查元素是否是一个class.  注意：不能用instanceof TypeElement来判断，因为接口类型也是TypeElement.
            if (element.getKind() != ElementKind.CLASS) {
                mMessager.printMessage(Diagnostic.Kind.WARNING,
                        element.getSimpleName().toString() + "不是类，不予处理");
                continue;
            }
            // 放心大胆地强转成TypeElement
            TypeElement classElement = (TypeElement) element;
            // 包名+类型:per.wsj.gitstar.ui.activity.EventActivity
            String fullClassName = classElement.getQualifiedName().toString();
            tempList.add(fullClassName);
        }
    }

    /**
     * 创建获取注解名的方法
     */
    private MethodSpec createNeedLoginFun() {
        ClassName arrayList = ClassName.get("java.util", "ArrayList");
        // 返回值类型 List<String>
        TypeName listOfView = ParameterizedTypeName.get(List.class, String.class);

        // 创建名为getViewAnno的方法
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("getNeedLoginList")
                .addModifiers(Modifier.PUBLIC)
                .returns(listOfView);
        // List<String> result = new ArrayList<>();
        methodBuilder.addStatement("$T result = new $T<>()", listOfView, arrayList);
        for (String s : tempList) {
            // result.add("per.wsj.gitstar.ui.activity.EventActivity");
            methodBuilder.addStatement("result.add(\"" + s + "\")");
        }
        // return result;
        methodBuilder.addStatement("return result");
        return methodBuilder.build();
    }
}
