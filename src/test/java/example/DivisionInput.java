package example;

public final class DivisionInput {
    private final int numerator;
    private final int divisor;

    public DivisionInput(int numerator, int divisor) {
        this.numerator = numerator;
        this.divisor = divisor;
    }

    public int getNumerator() {
        return numerator;
    }

    public int getDivisor() {
        return divisor;
    }

    public Integer getBoxedDivisor() {
        return divisor;
    }
}
