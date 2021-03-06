/*
 * Copyright 2011 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.compiler.web.taglib;

import java.lang.Override;
import java.net.URL;
import java.util.regex.Pattern;

import grails.web.controllers.ControllerMethod;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.grails.core.artefact.ControllerArtefactHandler;
import org.grails.compiler.injection.AbstractGrailsArtefactTransformer;
import grails.compiler.ast.AstTransformer;
import org.grails.io.support.GrailsResourceUtils;
import org.grails.plugins.web.api.ControllerTagLibraryApi;

/**
 * Enhances controller classes with a method missing implementation for tags.
 *
 * @author Graeme Rocher
 * @since 2.0
 */
@AstTransformer
public class ControllerTagLibraryTransformer extends AbstractGrailsArtefactTransformer {

    public static Pattern CONTROLLER_PATTERN = Pattern.compile(".+/" +
            GrailsResourceUtils.GRAILS_APP_DIR + "/controllers/(.+)Controller\\.groovy");

    @Override
    public Class<?> getInstanceImplementation() {
        return ControllerTagLibraryApi.class;
    }

    @Override
    public Class<?> getStaticImplementation() {
        return null;  // No static api
    }

    public boolean shouldInject(URL url) {
        return url != null && CONTROLLER_PATTERN.matcher(url.getFile()).find();
    }


    @Override
    protected AnnotationNode getMarkerAnnotation() {
        return new AnnotationNode(new ClassNode(ControllerMethod.class).getPlainNodeReference());
    }

    @Override
    public String getArtefactType() {
        return ControllerArtefactHandler.TYPE;
    }
}
