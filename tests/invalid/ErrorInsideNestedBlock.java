class ErrorInsideNestedBlock {
    public static void main(String[] args) {
        int i;
        i = 0;
        while (i < 10) {
            if (i < 5) {
                {
                    i = i + 1;
                }
            } else {
                i = false; // ERROR
            }
        }
    }
}
