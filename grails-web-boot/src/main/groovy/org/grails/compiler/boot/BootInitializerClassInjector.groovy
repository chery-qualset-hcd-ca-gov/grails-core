package org.grails.compiler.boot

import grails.boot.config.GrailsWebConfiguration
import grails.compiler.ast.AstTransformer
import grails.compiler.ast.GlobalClassInjectorAdapter
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.MethodNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.SourceUnit
import org.grails.compiler.injection.GrailsASTUtils
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.web.SpringBootServletInitializer

import java.lang.reflect.Modifier

/**
 * A transformation that automatically produces a Spring servlet initializer for a class that extends GrailsConfiguration. Given a class "Application" that
 * extends {@link GrailsWebConfiguration}, it produces:
 *
 * <pre>
 * <code>
 *
 * class ApplicationLoader extends SpringBootServletInitializer {
 *
 *     protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
 *         return application.sources(Application)
 *     }
 * }
 * </code>
 * </pre>
 *
 * @author Graeme Rocher
 * @since 3.0
 */
@CompileStatic
@AstTransformer
class BootInitializerClassInjector extends GlobalClassInjectorAdapter {

    public static final ClassNode GRAILS_CONFIGURATION_CLASS_NODE = ClassHelper.make(GrailsWebConfiguration)

    @Override
    void performInjectionInternal(SourceUnit source, ClassNode classNode) {
        if(GrailsASTUtils.isAssignableFrom(GRAILS_CONFIGURATION_CLASS_NODE, classNode)) {
            def methods = classNode.getMethods("main")
            for(MethodNode mn in methods) {
                if(Modifier.isStatic(mn.modifiers) && Modifier.isPublic(mn.modifiers)) {
                    def loaderClassNode = new ClassNode("${classNode.name}Loader", Modifier.PUBLIC, ClassHelper.make(SpringBootServletInitializer))


                    def springApplicationBuilder = ClassHelper.make(SpringApplicationBuilder)

                    def parameter = new Parameter(springApplicationBuilder, "application")
                    def methodBody = new BlockStatement()
                    methodBody.addStatement( new ExpressionStatement( new MethodCallExpression( new VariableExpression(parameter), "sources", new ClassExpression(classNode))))
                    loaderClassNode.addMethod( new MethodNode("configure", Modifier.PROTECTED, springApplicationBuilder, [parameter] as Parameter[], [] as ClassNode[], methodBody))
                    source.getAST().addClass(
                            loaderClassNode
                    )

                    break
                }
            }
        }
    }
}
