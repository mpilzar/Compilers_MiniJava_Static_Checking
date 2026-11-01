// Locals/parameters shadow fields; subclass fields hide superclass fields of the same name.
class Shadowing {
    public static void main(String[] args) {
        Child c;
        c = new Child();
        System.out.println(c.test(4));
    }
}

class Parent {
    int value;
    boolean ready;

    public int setValue(int v) {
        value = v;
        return value;
    }
}

class Child extends Parent {
    Parent value;

    public int test(int ready) {
        int result;
        value = new Parent();
        result = value.setValue(ready);
        return ready + result;
    }

    public boolean check() {
        return ready;
    }
}
