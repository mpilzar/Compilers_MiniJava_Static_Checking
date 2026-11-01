// Overloads distinguished by sibling classes (neither is a subtype of the other).
class SiblingOverloads {
    public static void main(String[] args) {
        Handler h;
        int r;
        h = new Handler();
        r = h.handle(new Cat(), new Cat());
        r = h.handle(new Fish(), new Fish());
        System.out.println(r);
    }
}

class Pet {
}

class Cat extends Pet {
}

class Fish extends Pet {
}

class Handler {
    public int handle(Cat a, Pet b) {
        return 1;
    }

    public int handle(Fish a, Pet b) {
        return 2;
    }
}
