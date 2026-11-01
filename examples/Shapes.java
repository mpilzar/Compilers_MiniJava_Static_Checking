class Shapes {
    public static void main(String[] args) {
        Canvas c;
        int area;
        c = new Canvas();
        area = c.draw(new Circle(), 3);
        area = c.draw(new Square(), true);
        System.out.println(area);
    }
}

class Shape {
    int size;

    public int area() {
        return size * size;
    }
}

class Circle extends Shape {
    public int area() {
        return (3 * size) * size;
    }
}

class Square extends Shape {
}

class Canvas {
    Shape last;

    public int draw(Shape s, int scale) {
        last = s;
        return scale * (s.area());
    }

    public int draw(Shape s, boolean outline) {
        last = s;
        return s.area();
    }
}
