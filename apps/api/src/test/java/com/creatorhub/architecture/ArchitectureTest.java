package com.creatorhub.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 模块化单体边界基线。
 *
 * <p>约束：domain 不依赖 infrastructure；interfaces 不直接读 domain 实体；
 * 同上下文内 interfaces/application/domain/infrastructure 依赖方向正确。
 *
 * <p>ArchUnit 测试在 CI 中作为边界守门人；违反时让构建失败。
 */
@AnalyzeClasses(
        packages = "com.creatorhub",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_infrastructure = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_spring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_jpa = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_stripe = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("com.stripe..");

    @ArchTest
    static final ArchRule interfaces_must_not_call_repositories_directly = noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule application_must_not_depend_on_interfaces = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..interfaces..");

    @ArchTest
    static final ArchRule interfaces_layer_dependencies = classes()
            .that().resideInAPackage("..interfaces..")
            .should().onlyAccessClassesThat()
            .resideInAnyPackage("com.creatorhub..", "java..", "jakarta..", "org.springframework..");

    /**
     * 跨限界上下文禁止互相引用 Repository / JPA Entity / 数据库表。
     * 允许通过 application/port/in 或 Outbox 事件协作。
     *
     * <p>注意：当前 Week 1 阶段仅搭建骨架，目录占位，规则暂以注释形式保留；
     * 等到具体业务上下文落地后，再用 ArchUnit 的 slices + custom predicate
     * 表达"只允许访问对方 application/port/in 和 domain.event"。
     */
    // @ArchTest
    // static final ArchRule bounded_context_isolation = slices()...
}
