// Each position is related by subtyping (Cat <: Pet, Pet :> Fish), so the overloads clash.
class AmbiguousCrossedParameters {
    public static void main(String[] args) {
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

    public int handle(Pet a, Fish b) { // ERROR
        return 2;
    }
}
