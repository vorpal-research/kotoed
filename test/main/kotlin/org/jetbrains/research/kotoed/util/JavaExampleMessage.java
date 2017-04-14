package org.jetbrains.research.kotoed.util;

import io.vertx.core.json.JsonObject;
import kotlin.jvm.JvmClassMappingKt;
import org.jetbrains.annotations.NotNull;

public class JavaExampleMessage implements Jsonable {
    private int bar;
    private String foo;
    private ExampleMessage inner;

    public JavaExampleMessage() {}

    public int getBar() {
        return bar;
    }

    public void setBar(int bar) {
        this.bar = bar;
    }

    public String getFoo() {
        return foo;
    }

    public void setFoo(String foo) {
        this.foo = foo;
    }

    public ExampleMessage getInner() {
        return inner;
    }

    public void setInner(ExampleMessage inner) {
        this.inner = inner;
    }

    static public JavaExampleMessage fromJson(JsonObject json) {
        JavaExampleMessage ret = new JavaExampleMessage();
        ret.setBar(json.getInteger("bar"));
        ret.setFoo(json.getString("foo"));
        ExampleMessage inner =
                JsonUtilKt.fromJson(
                        json.getJsonObject("inner"),
                        JvmClassMappingKt.getKotlinClass(ExampleMessage.class)
                );
        ret.setInner(inner);
        return ret;
    }

    @NotNull
    @Override
    public JsonObject toJson() {
        return new JsonObject(String.format("{\"bar\": %s, \"foo\": %s, \"inner\": %s}", bar, foo, inner.toJson()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JavaExampleMessage that = (JavaExampleMessage) o;

        if (getBar() != that.getBar()) return false;
        if (!getFoo().equals(that.getFoo())) return false;
        return getInner().equals(that.getInner());
    }

    @Override
    public int hashCode() {
        int result = getBar();
        result = 31 * result + getFoo().hashCode();
        result = 31 * result + getInner().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JavaExampleMessage{" +
                "bar=" + bar +
                ", foo='" + foo + '\'' +
                ", inner=" + inner +
                '}';
    }
}
