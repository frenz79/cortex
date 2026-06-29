package com.cortex.base.serialization;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.cortex.base.annotations.SerializableAttribute;
import com.cortex.base.annotations.SerializableAttribute.SerializableType;
import com.cortex.base.annotations.SerializableClass;

public class SoASerializer {

	public Set<Class<?>> findAllClassesUsingClassLoader(String packageName) {
	    try {
	        ClassLoader cl = Thread.currentThread().getContextClassLoader();
	        String path = packageName.replace('.', '/');
	        Enumeration<URL> resources = cl.getResources(path);

	        Set<Class<?>> classes = new HashSet<>();

	        while (resources.hasMoreElements()) {
	            URL resource = resources.nextElement();
	            File dir = new File(resource.toURI());

	            for (File file : dir.listFiles()) {
	                if (file.getName().endsWith(".class")) {
	                    String className = packageName + "." +
	                        file.getName().replace(".class", "");
	                    classes.add(Class.forName(className));
	                }
	            }
	        }
	        return classes;
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	private void validateConstructorMatchesAttributes(SoAClassModel model) {
	    Set<String> attrNames = model.getAttributeModels().stream()
	        .map(AttributeModel::name)
	        .collect(Collectors.toSet());

	    for (String param : model.constructorParams) {
	        if (!attrNames.contains(param)) {
	            throw new IllegalStateException(
	                "Constructor parameter '%s' does not match any @SerializableAttribute".formatted(param)
	            );
	        }
	    }
	}
	
	public void serialize() {	
		for (Class<?> clazz : findAllClassesUsingClassLoader(".")) {

			if ( clazz.getAnnotation(SerializableClass.class)==null ) continue;
			SoAClassModel classModel = new SoAClassModel(clazz.getSimpleName());
			
			for ( Field field : clazz.getDeclaredFields() ) {
				SerializableAttribute attr = field.getAnnotation(SerializableAttribute.class);
				if (attr != null) {
					String name = attr.name().isEmpty() ? field.getName() : attr.name();

					// deduci tipo
					SerializableType type = attr.type();
					if (type == SerializableType.AUTO) {
						type = deduceType(field.getType());
					}

					// salva nel modello
					classModel.add(new AttributeModel(name, type));
				}
			}
			
			writeHeader( classModel );
			// writeData( classModel )
		}
	}

	private void writeHeader(SoAClassModel classModel) {
		// TODO Auto-generated method stub	
	}

	private SerializableType deduceType(Class<?> type) {
		switch (type.getTypeName()) {
		case "byte[]": return SerializableType.BYTE;
		case "int[]": return SerializableType.INT32;
		case "long[]": return SerializableType.INT64;
		case "float[]": return SerializableType.FLOAT32;
		default:
			throw new IllegalArgumentException("Usupported type:%s".formatted(String.valueOf(type)));
		}
	}

	public static class SoAClassModel {
		private final String name;
		private final List<AttributeModel> attributeModels = new ArrayList<>();
		
		public SoAClassModel(String name) {
			super();
			this.name = name;
		}

		public void add(AttributeModel attributeModel) {
			this.attributeModels.add(attributeModel);
		}

		public List<AttributeModel> getAttributeModels() {
			return attributeModels;
		}
	}

	public static record AttributeModel(String name, SerializableType type) {
		
	}
}
