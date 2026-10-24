/** Enable semantic highlighting to follow each input and intermediate value. */
public class SemanticDemo {
    static double calculateTotal(double subtotal, double taxRate, double discount) {
        double discounted = Math.max(0, subtotal - discount);
        double tax = discounted * taxRate;
        double total = discounted + tax;
        return Math.round(total * 100) / 100.0;
    }

    public static void main(String[] args) {
        double subtotal = 120;
        double taxRate = 0.25;
        double discount = 15;
        double total = calculateTotal(subtotal, taxRate, discount);
        System.out.printf("Subtotal: %.2f, discount: %.2f, total: %.2f%n",
                subtotal, discount, total);
    }
}
