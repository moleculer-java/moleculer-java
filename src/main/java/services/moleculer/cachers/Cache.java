package services.moleculer.cachers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.TYPE })
public @interface Cache {

	/**
	 * Cache keys (names of the key values in input JSON map, eg "userID")
	 * 
	 * @return cache keys
	 */
	String[] value() default {};

}