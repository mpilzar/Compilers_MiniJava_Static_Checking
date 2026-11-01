// Subclass instances flow into superclass-typed variables, parameters and return values.
class SubtypingFlow {
    public static void main(String[] args) {
        Animal a;
        Dog d;
        Shelter s;
        d = new Puppy();
        a = d;
        s = new Shelter();
        a = s.adopt(d);
        a = s.adopt(new Puppy());
        System.out.println(s.count());
    }
}

class Animal {
    public int legs() {
        return 4;
    }
}

class Dog extends Animal {
}

class Puppy extends Dog {
    public int legs() {
        return 4;
    }
}

class Shelter {
    Animal last;
    int adopted;

    public Animal adopt(Animal x) {
        last = x;
        adopted = adopted + 1;
        return new Puppy();
    }

    public int count() {
        return adopted;
    }
}
