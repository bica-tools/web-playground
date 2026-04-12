package com.bica.reborn.processor;

import com.bica.reborn.annotation.Exclusive;
import com.bica.reborn.annotation.ReadOnly;
import com.bica.reborn.annotation.Shared;
import com.bica.reborn.concurrency.ConcurrencyLevel;
import com.bica.reborn.concurrency.MethodClassification;
import com.bica.reborn.concurrency.MethodClassifier;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Classifies methods from a {@link TypeElement} using annotations and modifiers.
 *
 * <p>Classification priority (highest to lowest):
 * <ol>
 *   <li>{@code @Shared} annotation → {@code SHARED}</li>
 *   <li>{@code @ReadOnly} annotation → {@code READ_ONLY}</li>
 *   <li>{@code @Exclusive} annotation → {@code EXCLUSIVE}</li>
 *   <li>{@code synchronized} modifier → {@code SYNC}</li>
 *   <li>{@code static} modifier → {@code SHARED}</li>
 *   <li>Default → {@code EXCLUSIVE}</li>
 * </ol>
 */
public final class SourceMethodClassifier implements MethodClassifier {

    private final Map<String, MethodClassification> classifications;

    /**
     * Creates a classifier from the methods declared in the given type element.
     *
     * @param typeElement The class or interface whose methods to classify.
     */
    public SourceMethodClassifier(TypeElement typeElement) {
        Objects.requireNonNull(typeElement, "typeElement must not be null");
        this.classifications = new HashMap<>();

        for (var enclosed : typeElement.getEnclosedElements()) {
            if (enclosed instanceof ExecutableElement method) {
                String name = method.getSimpleName().toString();
                MethodClassification mc = classifyMethod(method);
                classifications.put(name, mc);
            }
        }
    }

    @Override
    public MethodClassification classify(String methodName) {
        return classifications.get(methodName);
    }

    private static MethodClassification classifyMethod(ExecutableElement method) {
        String name = method.getSimpleName().toString();

        // Annotation takes priority
        if (method.getAnnotation(Shared.class) != null) {
            return new MethodClassification(name, ConcurrencyLevel.SHARED, "annotation @Shared");
        }
        if (method.getAnnotation(ReadOnly.class) != null) {
            return new MethodClassification(name, ConcurrencyLevel.READ_ONLY, "annotation @ReadOnly");
        }
        if (method.getAnnotation(Exclusive.class) != null) {
            return new MethodClassification(name, ConcurrencyLevel.EXCLUSIVE, "annotation @Exclusive");
        }

        // Modifier-based inference
        if (method.getModifiers().contains(Modifier.SYNCHRONIZED)) {
            return new MethodClassification(name, ConcurrencyLevel.SYNC, "synchronized modifier");
        }
        if (method.getModifiers().contains(Modifier.STATIC)) {
            return new MethodClassification(name, ConcurrencyLevel.SHARED, "static modifier");
        }

        // Conservative default
        return new MethodClassification(name, ConcurrencyLevel.EXCLUSIVE, "default");
    }
}
