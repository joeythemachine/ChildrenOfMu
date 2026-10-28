package javax.annotation;
import java.lang.annotation.*;
@Retention(RetentionPolicy.CLASS) @Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface Nullable {}
