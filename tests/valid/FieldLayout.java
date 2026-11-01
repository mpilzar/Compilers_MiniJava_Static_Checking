// Mixed field sizes: int = 4, boolean = 1, int[] and class references = 8.
class FieldLayout {
    public static void main(String[] args) {
        System.out.println(new Shape().area());
    }
}

class Shape {
    boolean visible;
    int width;
    int[] points;
    boolean filled;
    Shape next;
    int height;

    public int area() {
        width = 3;
        height = 4;
        return width * height;
    }
}

class Square extends Shape {
    boolean rounded;
    int side;

    public int area() {
        side = 5;
        return side * side;
    }

    public int perimeter() {
        return 4 * side;
    }
}
