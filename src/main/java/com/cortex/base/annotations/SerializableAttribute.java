package com.cortex.base.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SerializableAttribute {
	
	public enum SerializableType {
	    AUTO,      // deduce via reflection
	    INT32,
	    INT64,
	    FLOAT32,
	    BYTE
	}
	
    String name() default "";               // se vuoto → usa il nome del campo
    SerializableType type() default SerializableType.AUTO; 
}
