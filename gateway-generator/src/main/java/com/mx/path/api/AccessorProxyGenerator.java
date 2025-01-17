package com.mx.path.api;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.rmi.ConnectException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;

import com.mx.path.core.common.accessor.API;
import com.mx.path.core.common.accessor.RootAccessor;
import com.mx.path.core.common.gateway.GatewayAPI;
import com.mx.path.core.common.lang.Strings;
import com.mx.path.core.common.reflection.Annotations;
import com.mx.path.core.utility.reflection.ClassHelper;
import com.mx.path.gateway.accessor.Accessor;
import com.mx.path.gateway.accessor.AccessorConfiguration;
import com.mx.path.gateway.configuration.AccessorConstructionContext;
import com.mx.path.gateway.configuration.AccessorProxy;
import com.mx.path.gateway.configuration.AccessorProxyMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import org.apache.commons.lang3.StringUtils;

/**
 * Generates a proxy wrapper for each accessor.
 */
public class AccessorProxyGenerator {
  private final Filer filer;
  private CodeBlock.Builder accessorProxyMappings;

  /**
   * Build new {@link AccessorProxyGenerator} instance with provided processing environment.
   *
   * @param processingEnvironment processing environment to interact with {@link Filer} file generation.
   */
  public AccessorProxyGenerator(ProcessingEnvironment processingEnvironment) {
    this.filer = processingEnvironment.getFiler();
  }

  /**
   * Entry point for generating an accessor proxy based on the provided root accessor class.
   *
   * @param rootAccessor class of the root accessor to generate an accessor proxy for.
   * @return accessor proxy
   * @throws IOException to be thrown
   */
  public final CodeBlock generateAll(Class<? extends Accessor> rootAccessor) throws IOException {
    if (!Annotations.hasAnnotation(rootAccessor, RootAccessor.class)) {
      throw new ConnectException("Accessor " + rootAccessor.getCanonicalName() + " is missing @RootAccessor annotation");
    }
    accessorProxyMappings = CodeBlock.builder();
    recursiveGenerate(rootAccessor);
    accessorProxyMappings.addStatement("$T.freeze()", ClassHelper.buildClassName(AccessorProxyMap.class));

    return accessorProxyMappings.build();
  }

  @SuppressWarnings("unchecked")
  private void recursiveGenerate(Class<? extends Accessor> klass) throws IOException {

    String proxyName = klass.getSimpleName() + "Proxy";
    String packageName = calculatePackageName(klass);

    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(proxyName)
        .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
        .superclass(klass)
        .addSuperinterface(ClassHelper.buildClassName(AccessorProxy.class))

        .addJavadoc(
            "Base class for wrapping " + klass.getName() + ".\n"
                + "<p>\n"
                + "Used to provide scoped construction strategies.")

        .addMethod(
            MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(AccessorConfiguration.class, "configuration")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(klass)), "accessorClass")
                .addStatement("this.setConfiguration(configuration)")
                .addStatement("this.accessorConstructionContext = new $T(accessorClass, configuration)", ParameterizedTypeName.get(ClassHelper.buildClassName(AccessorConstructionContext.class), TypeName.get(klass)))
                .build())

        .addField(
            FieldSpec.builder(ParameterizedTypeName.get(ClassHelper.buildClassName(AccessorConstructionContext.class), WildcardTypeName.subtypeOf(klass)), "accessorConstructionContext")
                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                .addAnnotation(ClassName.get("lombok", "Getter"))
                .build())

        .addMethod(
            MethodSpec.methodBuilder("getAccessorClass")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(klass)))
                .addStatement("return accessorConstructionContext.getAccessorClass()")
                .build());

    propagateRootAccessorAnnotation(klass, classBuilder);

    /**
     * Step through the fields with types that are assignable to Accessor, and generate
     * the accessor proxies for that class.
     */
    for (Field field : klass.getDeclaredFields()) {
      Class<?> fieldClass = field.getType();
      if (Accessor.class.isAssignableFrom(fieldClass)) {
        recursiveGenerate((Class<? extends Accessor>) fieldClass);
      }
    }

    /**
     * Find all GatewayAPI methods and add them to the proxy.
     * The body will forward the call on to the result of {@code build()}
     *
     * NOTE: We are skipping the sub-accessor and letting the base class handle
     * those. They can be configured with a scope proxy.
     */
    List<Method> methods = new ArrayList<>();
    for (Method method : klass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(API.class)) {
        methods.add(method);
      }
    }

    methods.sort(Comparator.comparing(Method::getName));

    methods.forEach(method -> {
      MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(method.getName())
          .addModifiers(Modifier.PUBLIC)
          .addAnnotation(Override.class)
          .addJavadoc("@return " + method.getName() + " accessor")
          .returns(ClassName.get(method.getGenericReturnType()));

      List<String> parameterNames = new ArrayList<>();
      for (Parameter parameter : method.getParameters()) {
        parameterNames.add(parameter.getName());
        methodBuilder.addParameter(parameter.getType(), parameter.getName());
      }
      if (!method.isAnnotationPresent(GatewayAPI.class)) {
        methodBuilder.addCode(CodeBlock.builder()
            .add("if (get$L() != null) {\n", StringUtils.capitalize(method.getName()))
            .add("  return get$L();\n", StringUtils.capitalize(method.getName()))
            .add("}\n")
            .build());
      }
      methodBuilder.addStatement("return build()." + method.getName() + "(" + String.join(",", parameterNames) + ")");
      classBuilder.addMethod(methodBuilder.build());
    });

    classBuilder.addMethod(
        MethodSpec.methodBuilder("buildAccessor")
            .addModifiers(Modifier.PROTECTED)
            .addStatement("return accessorConstructionContext.build()")
            .addJavadoc(
                "Create an instance of Accessor type klass\n\n"
                    + "<p>Override to change how the accessor is constructed.\n"
                    + "@param klass\n"
                    + "@return Accessor")
            .returns(klass)
            .build());

    classBuilder.addMethod(
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(klass)
            .build());

    TypeSpec annotationProxySpec = classBuilder.build();
    JavaFile javaFile = JavaFile.builder(packageName, annotationProxySpec)
        .addFileComment("---------------------------------------------------------------------------------------------------------------------\n"
            + "  GENERATED FILE - ** Do not edit **\n"
            + "  Wrapped Class: $L\n"
            + "---------------------------------------------------------------------------------------------------------------------",
            klass.getCanonicalName())
        .build();
    javaFile.writeTo(filer);

    generateSingletonProxy(proxyName, klass);
    generatePrototypeProxy(proxyName, klass);
  }

  private void generateSingletonProxy(String proxyBaseClass, Class<? extends Accessor> accessorClass) throws IOException {
    String packageName = calculatePackageName(accessorClass);
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(proxyBaseClass + "Singleton")
        .addModifiers(Modifier.PUBLIC)
        .superclass(ClassName.get(packageName, proxyBaseClass))
        .addField(FieldSpec.builder(accessorClass, "instance", Modifier.PRIVATE).build())
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(AccessorConfiguration.class, "configuration")
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(accessorClass)), "accessorClass")
            .addStatement("super(configuration, accessorClass)")
            .addStatement("this.instance = buildAccessor()")
            .build())
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(AccessorConfiguration.class, "configuration")
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(accessorClass)), "accessorClass")
            .addParameter(ClassName.get(accessorClass), "instance")
            .addStatement("super(configuration, accessorClass)")
            .addStatement("this.instance = instance")
            .build())
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(accessorClass)
            .addStatement("return instance")
            .build())
        .addMethod(MethodSpec.methodBuilder("getScope")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class)
            .addStatement("return $S", "singleton")
            .build());

    propagateRootAccessorAnnotation(accessorClass, classBuilder);

    TypeSpec annotationProxySpec = classBuilder.build();
    JavaFile javaFile = JavaFile.builder(packageName, annotationProxySpec)
        .addFileComment("---------------------------------------------------------------------------------------------------------------------\n"
            + "  GENERATED FILE - ** Do not edit **\n"
            + "---------------------------------------------------------------------------------------------------------------------")
        .build();
    javaFile.writeTo(filer);

    accessorProxyMappings.addStatement("$T.add(\"singleton\", $T.class, $T.class)", ClassHelper.buildClassName(AccessorProxyMap.class), accessorClass, ClassName.get(packageName, proxyBaseClass + "Singleton"));
  }

  private void generatePrototypeProxy(String proxyBaseClass, Class<? extends Accessor> accessorClass) throws IOException {
    String packageName = calculatePackageName(accessorClass);
    TypeSpec.Builder classBuilder = TypeSpec.classBuilder(proxyBaseClass + "Prototype")
        .addModifiers(Modifier.PUBLIC)
        .superclass(ClassName.get(packageName, proxyBaseClass))
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PUBLIC)
            .addParameter(AccessorConfiguration.class, "configuration")
            .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), WildcardTypeName.subtypeOf(accessorClass)), "accessorClass")
            .addStatement("super(configuration, accessorClass)")
            .build())
        .addMethod(MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(accessorClass)
            .addStatement("return buildAccessor()")
            .build())
        .addMethod(MethodSpec.methodBuilder("getScope")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class)
            .addStatement("return $S", "prototype")
            .build());

    propagateRootAccessorAnnotation(accessorClass, classBuilder);

    TypeSpec annotationProxySpec = classBuilder.build();
    JavaFile javaFile = JavaFile.builder(packageName, annotationProxySpec)
        .addFileComment("---------------------------------------------------------------------------------------------------------------------\n"
            + "  GENERATED FILE - ** Do not edit **\n"
            + "---------------------------------------------------------------------------------------------------------------------")
        .build();
    javaFile.writeTo(filer);

    accessorProxyMappings.addStatement("$T.add(\"prototype\", $T.class, $T.class)", ClassHelper.buildClassName(AccessorProxyMap.class), accessorClass, ClassName.get(packageName, proxyBaseClass + "Prototype"));
  }

  private String calculatePackageName(Class<? extends Accessor> accessorClass) {
    String packageClass = accessorClass.getPackage().getName().replaceAll(".*\\.accessors?[.]*", "");
    if (Strings.isBlank(packageClass)) {
      return "com.mx.path.gateway.accessor.proxy";
    }
    packageClass = "com.mx.path.gateway.accessor.proxy." + packageClass;
    return packageClass;
  }

  private void propagateRootAccessorAnnotation(Class<? extends Accessor> klass, TypeSpec.Builder classBuilder) {
    if (Annotations.hasAnnotation(klass, RootAccessor.class)) {
      classBuilder.addAnnotation(RootAccessor.class);
    }
  }
}
