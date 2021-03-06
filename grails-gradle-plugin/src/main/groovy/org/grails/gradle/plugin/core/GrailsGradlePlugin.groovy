package org.grails.gradle.plugin.core

import grails.util.BuildSettings
import grails.util.Environment
import grails.util.Metadata
import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.EscapeUnicode
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.CopySpec
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.GroovyCompile
import org.grails.gradle.plugin.agent.AgentTasksEnhancer
import org.grails.gradle.plugin.run.FindMainClassTask

class GrailsGradlePlugin extends GroovyPlugin {

    void apply(Project project) {
        super.apply(project)
        project.extensions.create("grails", GrailsExtension)


        registerFindMainClassTask(project)

        def projectDir = project.projectDir

        def grailsSourceDirs = []
        def excludedDirs = ['views', 'migrations', 'assets', 'i18n', 'conf']
        new File("$projectDir/grails-app").eachDir { File subdir ->
            def dirName = subdir.name
            if(!subdir.hidden && !dirName.startsWith(".") && !excludedDirs.contains(dirName)) {
                grailsSourceDirs << subdir.absolutePath
            }
        }

        grailsSourceDirs << "$projectDir/src/main/groovy"

        System.setProperty( BuildSettings.APP_BASE_DIR, project.projectDir.absolutePath)
        def environment = Environment.current


        enableFileWatch(environment, project)

        def grailsVersion = project.getProperties().get('grailsVersion')

        if(!grailsVersion) {
            def grailsCoreDep = project.configurations.getByName('compile').dependencies.find { Dependency d -> d.name == 'grails-core' }
            grailsVersion = grailsCoreDep.version
        }

        enableNative2Ascii(project, grailsVersion)


        if(project.extensions.findByName('assets')) {
            project.assets {
                assetsPath = 'grails-app/assets'
                compileDir = 'build/assetCompile/assets'
            }
        }

        project.afterEvaluate {
            if( project.tasks.findByName("war") ) {
                project.war {
                   from "${project.buildDir}/assetCompile"
                }
            }
            else {
                project.processResources {
                    from "${project.buildDir}/assetCompile"
                }
            }

        }
        project.tasks.withType(JavaExec).each { JavaExec task ->
            task.systemProperty Metadata.APPLICATION_NAME, project.name
            task.systemProperty Metadata.APPLICATION_VERSION, project.version
            task.systemProperty Metadata.APPLICATION_GRAILS_VERSION, grailsVersion
        }

        project.sourceSets {
            main {
                groovy {
                    srcDirs = grailsSourceDirs
                    resources {
                        srcDirs = [
                                "$projectDir/src/main/resources",
                                "$projectDir/grails-app/conf",
                                "$projectDir/grails-app/views",
                                "$projectDir/grails-app/i18n"
                        ]
                    }
                }
            }
        }
    }

    protected void enableFileWatch(Environment environment, Project project) {
        if (environment.isReloadEnabled()) {
//            configureWatchPlugin(project)

            project.configurations {
                agent
            }
            project.dependencies {
                agent "org.springframework:springloaded:1.2.1.RELEASE"
            }
            project.afterEvaluate(new AgentTasksEnhancer())
        }
    }

    @CompileStatic
    protected void registerFindMainClassTask(Project project) {
        def findMainClassTask = project.tasks.create(name: "findMainClass", type: FindMainClassTask, overwrite: true)
        findMainClassTask.mustRunAfter project.tasks.withType(GroovyCompile)
        def bootRepackageTask = project.tasks.findByName("bootRepackage")
        if(bootRepackageTask) {
            bootRepackageTask.dependsOn findMainClassTask
        }
    }

    /**
     * Enables native2ascii processing of resource bundles
     **/
    protected void enableNative2Ascii(Project project, grailsVersion) {
        for (SourceSet sourceSet in project.sourceSets) {
            project.tasks.getByName(sourceSet.processResourcesTaskName) { CopySpec task ->
                def grailsExt = project.extensions.getByType(GrailsExtension)
                task.filter( ReplaceTokens, tokens: [
                        'info.app.name': project.name,
                        'info.app.version': project.version,
                        'info.app.grailsVersion': grailsVersion
                    ]
                )
                task.from(sourceSet.resources) {
                    include '**/*.properties'
                    if(grailsExt.native2ascii) {
                        filter(EscapeUnicode)
                    }
                }
                task.from(sourceSet.resources) {
                    exclude '**/*.properties'
                }
                task.from(sourceSet.resources) {
                    include '**/*.groovy'
                }
            }
        }

    }

}
